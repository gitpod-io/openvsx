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

import static org.eclipse.openvsx.schedule.JobUtil.completed;
import static org.eclipse.openvsx.schedule.JobUtil.starting;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;
import java.util.stream.Collectors;

import org.eclipse.openvsx.AdminService;
import org.eclipse.openvsx.ExtensionService;
import org.eclipse.openvsx.IExtensionRegistry;
import org.eclipse.openvsx.LocalRegistryService;
import org.eclipse.openvsx.UserService;
import org.eclipse.openvsx.entities.ExtensionVersion;
import org.eclipse.openvsx.entities.Namespace;
import org.eclipse.openvsx.entities.NamespaceMembership;
import org.eclipse.openvsx.entities.UserData;
import org.eclipse.openvsx.json.ExtensionJson;
import org.eclipse.openvsx.json.NamespaceJson;
import org.eclipse.openvsx.json.UserJson;
import org.eclipse.openvsx.repositories.RepositoryService;
import org.eclipse.openvsx.schedule.SchedulerService;
import org.eclipse.openvsx.util.NotFoundException;
import org.eclipse.openvsx.util.TargetPlatform;
import org.eclipse.openvsx.util.TimeUtil;
import org.eclipse.openvsx.util.VersionAlias;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.client.RestTemplate;

import io.micrometer.core.annotation.Timed;

@DisallowConcurrentExecution
public class MirrorExtensionJob implements Job {

    protected final Logger logger = LoggerFactory.getLogger(MirrorExtensionJob.class);

    @Autowired
    AdminService admin;

    @Autowired
    IExtensionRegistry mirror;

    @Autowired
    RepositoryService repositories;

    @Autowired
    SchedulerService schedulerService;

    @Autowired
    DataMirrorService data;

    @Value("${ovsx.data.mirror.user-name}")
    String userName;

    @Autowired
    RestTemplate contentRestTemplate;

    @Autowired
    RestTemplate nonRedirectingRestTemplate;

    @Autowired
    LocalRegistryService local;

    @Autowired
    UserService users;

    @Autowired
    ExtensionService extensions;

    @Override
    @Timed(longTask = true)
    public void execute(JobExecutionContext context) throws JobExecutionException {
        starting(context, logger);
        try {
            var map = context.getMergedJobDataMap();
            var namespaceName = map.getString("namespace");
            var extensionName = map.getString("extension");
            if(repositories.findNamespace(namespaceName) == null) {
                var json = new NamespaceJson();
                json.name = namespaceName;
                admin.createNamespace(json);
            }

            var extension = repositories.findExtension(extensionName, namespaceName);
            var extVersions = extension != null
                    ? extension.getVersions()
                    : Collections.<ExtensionVersion>emptyList();

            var mirrorUser = repositories.findUserByLoginName(null, userName);
            for(var targetPlatform : TargetPlatform.TARGET_PLATFORM_NAMES) {
                var targetVersions = extVersions.stream()
                        .filter(extVersion -> extVersion.getTargetPlatform().equals(targetPlatform))
                        .collect(Collectors.toList());

                try {
                    var json = mirror.getExtension(namespaceName, extensionName, targetPlatform);
                    var versions = json.allVersions.keySet();
                    VersionAlias.ALIAS_NAMES.forEach(versions::remove);

                    targetVersions.stream()
                            .filter(extVersion -> !versions.contains(extVersion.getVersion()))
                            .forEach(extVersion -> deleteExtensionVersion(extVersion, mirrorUser));

                    //TODO cherry pick Huiwen's improvements
                    var toAdd = versions.stream()
                            .filter(version -> targetVersions.stream().noneMatch(extVersion -> extVersion.getVersion().equals(version)))
                            .map(version -> mirror.getExtension(namespaceName, extensionName, targetPlatform, version))
                            .sorted(Comparator.comparing(extensionJson -> TimeUtil.fromUTCString(extensionJson.timestamp)))
                            .collect(Collectors.toList());

                    //TODO if order is not important then we can sync latest first and activate immediately
                    for (var extensionJson : toAdd) {
                        mirrorExtensionVersion(extensionJson);
                    }
                } catch (NotFoundException e) {
                    // combination of extension and target platform doesn't exist, try next
                }
            }

            logger.info("updating extension metadata: {}", namespaceName + "." + extensionName);
            data.updateMetadata(namespaceName, extensionName);
            logger.info("activating extension: {}", namespaceName + "." + extensionName);
            data.activateExtension(namespaceName, extensionName);
            mirrorNamespaceVerified(namespaceName);
        } finally {
            completed(context, logger);
        }
    }

    private void deleteExtensionVersion(ExtensionVersion extVersion, UserData user) {
        var extension = extVersion.getExtension();
        admin.deleteExtension(
                extension.getNamespace().getName(),
                extension.getName(),
                extVersion.getTargetPlatform(),
                extVersion.getVersion(),
                user
        );
    }

    private void mirrorNamespaceVerified(String namespaceName) {
        logger.info("starting to mirror verified namespace: {}", namespaceName);
        var remoteVerified = mirror.getNamespace(namespaceName).verified;
        var localVerified = local.getNamespace(namespaceName).verified;
        if(!localVerified && remoteVerified) {
            // verify the namespace by adding an owner to it
            var namespace = repositories.findNamespace(namespaceName);
            var memberships = repositories.findMemberships(namespace);
            users.addNamespaceMember(namespace, memberships.toList().get(0).getUser(), NamespaceMembership.ROLE_OWNER);
        }
        if(localVerified && !remoteVerified) {
            // unverify namespace by changing owner(s) back to contributor
            var namespace = repositories.findNamespace(namespaceName);
            repositories.findMemberships(namespace, NamespaceMembership.ROLE_OWNER)
                    .forEach(membership -> users.addNamespaceMember(namespace, membership.getUser(), NamespaceMembership.ROLE_CONTRIBUTOR));
        }
        logger.info("completed mirroring of verified namespace: {}", namespaceName);
    }

    private void mirrorExtensionVersion(ExtensionJson json) throws JobExecutionException {
        logger.info("starting to mirror extension version: {}", json.namespace + "." + json.name + "-" + json.version + "@" + json.targetPlatform);
        var download = json.files.get("download");
        var userJson = new UserJson();
        userJson.provider = json.publishedBy.provider;
        userJson.loginName = json.publishedBy.loginName;
        userJson.fullName = json.publishedBy.fullName;
        userJson.avatarUrl = json.publishedBy.avatarUrl;
        userJson.homepage = json.publishedBy.homepage;
        var namespaceName = json.namespace;

        var vsixResourceHeaders = nonRedirectingRestTemplate.headForHeaders(download);
        var vsixLocation = vsixResourceHeaders.getLocation();
        if (vsixLocation == null) {
            throw new JobExecutionException("Failed to parse location header from redirected vsix url");
        }

        var tokens = vsixLocation.getPath().split("/");
        var filename = tokens[tokens.length-1];
        if (!filename.endsWith(".vsix")) {
            throw new JobExecutionException("Invalid vsix filename from redirected vsix url");
        }

        var vsixPackage = contentRestTemplate.getForObject("{mirrorExtensionVersionUri}", byte[].class, Map.of("mirrorExtensionVersionUri", download));

        var user = data.getOrAddUser(userJson);
        var namespace = repositories.findNamespace(namespaceName);
        getOrAddNamespaceMembership(user, namespace);

        var description = "MirrorExtensionVersion";
        var accessTokenValue = data.getOrAddAccessTokenValue(user, description);

        ExtensionVersion extVersion;
        try(var input = new ByteArrayInputStream(vsixPackage)) {
            var token = users.useAccessToken(accessTokenValue);
            extVersion = extensions.mirrorVersion(input, token);
        } catch (IOException e) {
            throw new JobExecutionException(e);
        }

        var ext = extVersion.getExtension();
        data.updateMetadata(ext.getNamespace().getName(), ext.getName(), extVersion.getTargetPlatform(), extVersion.getVersion(), TimeUtil.toUTCString(extVersion.getTimestamp()), filename);
        logger.info("completed mirroring of extension version: {}", json.namespace + "." + json.name + "-" + json.version + "@" + json.targetPlatform);
    }

    private void getOrAddNamespaceMembership(UserData user, Namespace namespace) {
        var membership = repositories.findMembership(user, namespace);
        if (membership == null) {
            users.addNamespaceMember(namespace, user, NamespaceMembership.ROLE_CONTRIBUTOR);
        }
    }
}
