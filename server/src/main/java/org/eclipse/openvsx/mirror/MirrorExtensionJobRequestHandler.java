/** ******************************************************************************
 * Copyright (c) 2022 Precies. Software Ltd and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 * ****************************************************************************** */
package org.eclipse.openvsx.mirror;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;
import java.util.stream.Collectors;

import org.eclipse.openvsx.ExtensionService;
import org.eclipse.openvsx.UserService;
import org.eclipse.openvsx.entities.ExtensionVersion;
import org.eclipse.openvsx.json.ExtensionJson;
import org.eclipse.openvsx.json.UserJson;
import org.eclipse.openvsx.repositories.RepositoryService;
import org.eclipse.openvsx.schedule.Scheduler;
import org.eclipse.openvsx.util.NotFoundException;
import org.eclipse.openvsx.util.TargetPlatform;
import org.eclipse.openvsx.util.TimeUtil;
import org.eclipse.openvsx.util.VersionAlias;
import org.jobrunr.jobs.annotations.Job;
import org.jobrunr.jobs.lambdas.JobRequestHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import io.micrometer.core.annotation.Timed;

@Component
public class MirrorExtensionJobRequestHandler implements JobRequestHandler<MirrorExtensionJobRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(MirrorExtensionJobRequestHandler.class);

    @Autowired(required = false)
    DataMirrorService data;

    @Autowired
    Scheduler scheduler;

    @Autowired
    RepositoryService repositories;

    @Autowired
    RestTemplate contentRestTemplate;

    @Autowired
    RestTemplate contentNonRedirectingRestTemplate;

    @Autowired
    UserService users;

    @Autowired
    ExtensionService extensions;

    @Override
    @Job(name="Mirror Extension")
    @Timed(longTask = true)
    public void run(MirrorExtensionJobRequest jobRequest) throws Exception {
        var namespaceName = jobRequest.getNamespace();
        var extensionName = jobRequest.getExtension();
        if(data == null) {
            scheduler.unscheduleMirrorExtension(namespaceName, extensionName);
            return;
        }
        jobContext().logger().info(namespaceName + '.' + extensionName);
        LOGGER.debug(">> Starting MirrorExtensionJob for {}.{}", namespaceName, extensionName);
        try {
            mirrorExtension(namespaceName, extensionName);
        } finally {
            LOGGER.debug("<< Completed MirrorExtensionJob for {}.{}", namespaceName, extensionName);
        }
    }
    
    /**
     * It applies delta from previous execution.
     */
    public void mirrorExtension(String namespaceName, String extensionName) {
        data.ensureNamespace(namespaceName);

        var extension = repositories.findExtension(extensionName, namespaceName);
        var extVersions = extension != null
                ? extension.getVersions()
                : Collections.<ExtensionVersion>emptyList();

        // TODO(jp) optimize to fetch only for available target and only if version was not synced before
        var mirrorUser = repositories.findUserByLoginName(null, data.getUserName());
        for(var targetPlatform : TargetPlatform.TARGET_PLATFORM_NAMES) {
            var targetVersions = extVersions.stream()
                    .filter(extVersion -> extVersion.getTargetPlatform().equals(targetPlatform))
                    .collect(Collectors.toList());

            try {
                var json = data.getMirror().getExtension(namespaceName, extensionName, targetPlatform);
                var versions = json.allVersions.keySet();
                VersionAlias.ALIAS_NAMES.forEach(versions::remove);

                targetVersions.stream()
                        .filter(extVersion -> !versions.contains(extVersion.getVersion()))
                        .forEach(extVersion -> data.deleteExtensionVersion(extVersion, mirrorUser));

                //TODO cherry pick Huiwen's improvements
                var toAdd = versions.stream()
                        .filter(version -> targetVersions.stream().noneMatch(extVersion -> extVersion.getVersion().equals(version)))
                        .map(version -> data.getMirror().getExtension(namespaceName, extensionName, targetPlatform, version))
                        .sorted(Comparator.comparing(extensionJson -> TimeUtil.fromUTCString(extensionJson.timestamp)))
                        .collect(Collectors.toList());

                for (var extensionJson : toAdd) {
                    mirrorExtensionVersion(extensionJson);
                }
            } catch (NotFoundException e) {
                // combination of extension and target platform doesn't exist, try next
            }
        }

        LOGGER.debug("updating extension metadata: {}", namespaceName + "." + extensionName);
        data.updateMetadata(namespaceName, extensionName);
        LOGGER.debug("activating extension: {}", namespaceName + "." + extensionName);
        data.activateExtension(namespaceName, extensionName);
        data.mirrorNamespaceVerified(namespaceName);
    }

    private void mirrorExtensionVersion(ExtensionJson json) throws RuntimeException {
        LOGGER.debug("starting to mirror extension version: {}", json.namespace + "." + json.name + "-" + json.version + "@" + json.targetPlatform);
        var download = json.files.get("download");
        var userJson = new UserJson();
        userJson.provider = json.publishedBy.provider;
        userJson.loginName = json.publishedBy.loginName;
        userJson.fullName = json.publishedBy.fullName;
        userJson.avatarUrl = json.publishedBy.avatarUrl;
        userJson.homepage = json.publishedBy.homepage;
        var namespaceName = json.namespace;

        var vsixResourceHeaders = contentNonRedirectingRestTemplate.headForHeaders("{resolveVsixLocation}", Map.of("resolveVsixLocation", download));
        var vsixLocation = vsixResourceHeaders.getLocation();
        if (vsixLocation == null) {
            throw new RuntimeException("Failed to parse location header from redirected vsix url");
        }

        var tokens = vsixLocation.getPath().split("/");
        var filename = tokens[tokens.length-1];
        if (!filename.endsWith(".vsix")) {
            throw new RuntimeException("Invalid vsix filename from redirected vsix url");
        }

        var vsixPackage = contentRestTemplate.getForObject("{mirrorExtensionVersionUri}", byte[].class, Map.of("mirrorExtensionVersionUri", vsixLocation));

        var user = data.getOrAddUser(userJson);
        var namespace = repositories.findNamespace(namespaceName);
        data.ensureNamespaceMembership(user, namespace);

        var description = "MirrorExtensionVersion";
        var accessTokenValue = data.getOrAddAccessTokenValue(user, description);

        ExtensionVersion extVersion;
        try(var input = new ByteArrayInputStream(vsixPackage)) {
            var token = users.useAccessToken(accessTokenValue);
            extVersion = extensions.mirrorVersion(input, token);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        var ext = extVersion.getExtension();
        data.updateMetadata(ext.getNamespace().getName(), ext.getName(), extVersion.getTargetPlatform(), extVersion.getVersion(), TimeUtil.toUTCString(extVersion.getTimestamp()), filename);
        LOGGER.debug("completed mirroring of extension version: {}", json.namespace + "." + json.name + "-" + json.version + "@" + json.targetPlatform);
    }

}
