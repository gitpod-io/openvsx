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

import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URLConnection;
import java.nio.channels.Channels;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.openvsx.ExtensionService;
import org.eclipse.openvsx.UserService;
import org.eclipse.openvsx.entities.UserData;
import org.eclipse.openvsx.json.ExtensionJson;
import org.eclipse.openvsx.json.UserJson;
import org.eclipse.openvsx.repositories.RepositoryService;
import org.eclipse.openvsx.util.NotFoundException;
import org.eclipse.openvsx.util.TargetPlatform;
import org.eclipse.openvsx.util.TimeUtil;
import org.eclipse.openvsx.util.VersionAlias;
import org.jobrunr.jobs.context.JobContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class MirrorExtensionService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MirrorExtensionService.class);

    @Autowired(required = false)
    DataMirrorService data;

    @Autowired
    RepositoryService repositories;

    @Autowired
    RestTemplate backgroundNonRedirectingRestTemplate;

    @Autowired
    UserService users;

    @Autowired
    ExtensionService extensions;

    /**
     * It applies delta from previous execution.
     */
    public void mirrorExtension(String namespaceName, String extensionName, UserData mirrorUser, LocalDate lastModified, JobContext jobContext) {
        var latest = data.getMirror().getExtension(namespaceName, extensionName, null);
        if (shouldMirrorExtensionVersions(namespaceName, extensionName, lastModified, latest)) {
            mirrorExtensionVersions(namespaceName, extensionName, mirrorUser, jobContext);
        } else {
            jobContext.logger().info("all versions are up to date " + namespaceName + "." + extensionName);
        }
        LOGGER.debug("activating extension: {}", namespaceName + "." + extensionName);
        data.activateExtension(namespaceName, extensionName);

        LOGGER.debug("updating extension metadata: {}", namespaceName + "." + extensionName);
        data.updateMetadata(namespaceName, extensionName, latest);
        
        LOGGER.debug("updating namespace metadata: {}", namespaceName);
        data.mirrorNamespaceMetadata(namespaceName);
    }
    
    private boolean shouldMirrorExtensionVersions(String namespaceName, String extensionName, LocalDate lastModified, ExtensionJson latest) {
        if (lastModified == null) {
            return true;
        }
        var extension = repositories.findExtension(extensionName, namespaceName);
        if (extension == null) {
            return true;
        }
        var lastMirrorDateTime = extension.getLastUpdatedDate();
        var lastMirroredDate = lastMirrorDateTime.toLocalDate();
        if (lastMirroredDate.isBefore(lastModified)) {
            return true;
        }
        return lastMirrorDateTime.isBefore(TimeUtil.fromUTCString(latest.timestamp));
    }

    private void mirrorExtensionVersions(String namespaceName, String extensionName, UserData mirrorUser, JobContext jobContext) {
        data.ensureNamespace(namespaceName);

        var toAdd = new ArrayList<ExtensionJson>();
        for(var targetPlatform : TargetPlatform.TARGET_PLATFORM_NAMES) {
            Set<String> versions;
            try {
                var json = data.getMirror().getExtension(namespaceName, extensionName, targetPlatform);
                versions = json.allVersions.keySet();
                VersionAlias.ALIAS_NAMES.forEach(versions::remove);
            } catch (NotFoundException e) {
                // combination of extension and target platform doesn't exist, try next
                continue;
            }

            var targetVersions = data.getExtensionTargetVersions(namespaceName, extensionName, targetPlatform);

            targetVersions.stream()
                    .filter(extVersion -> !versions.contains(extVersion.getVersion()))
                    .forEach(extVersion -> data.deleteExtensionVersion(extVersion, mirrorUser));

            toAdd.addAll(
                versions.stream()
                    .filter(version -> targetVersions.stream().noneMatch(extVersion -> extVersion.getVersion().equals(version)))
                    .map(version -> data.getMirror().getExtension(namespaceName, extensionName, targetPlatform, version))
                    .collect(Collectors.toList())
            );
        }
        toAdd.sort(Comparator.comparing(extensionJson -> TimeUtil.fromUTCString(extensionJson.timestamp)));
        
        var i = 0;
        for (var json : toAdd) {
            jobContext.logger().info("mirroring " + json.namespace + "." + json.name + "-" + json.version + "@" + json.targetPlatform + " (" + (i+1) + "/" +  toAdd.size() + ")");
            try {
                mirrorExtensionVersion(json);
                data.getMirroredVersions().increment();
            } catch (Throwable t) {
                data.getFailedVersions().increment();
                throw t;
            }
            i++;
        }
    }

    private void mirrorExtensionVersion(ExtensionJson json) throws RuntimeException {
        LOGGER.debug("mirroring: {}", json.namespace + "." + json.name + "-" + json.version + "@" + json.targetPlatform);
        var download = json.files.get("download");
        var userJson = new UserJson();
        userJson.provider = json.publishedBy.provider;
        userJson.loginName = json.publishedBy.loginName;
        userJson.fullName = json.publishedBy.fullName;
        userJson.avatarUrl = json.publishedBy.avatarUrl;
        userJson.homepage = json.publishedBy.homepage;
        var namespaceName = json.namespace;

        var vsixResourceHeaders = backgroundNonRedirectingRestTemplate.headForHeaders("{resolveVsixLocation}", Map.of("resolveVsixLocation", download));
        var vsixLocation = vsixResourceHeaders.getLocation();
        if (vsixLocation == null) {
            throw new RuntimeException("Failed to parse location header from redirected vsix url");
        }

        var tokens = vsixLocation.getPath().split("/");
        var filename = tokens[tokens.length-1];
        if (!filename.endsWith(".vsix")) {
            throw new RuntimeException("Invalid vsix filename from redirected vsix url");
        }

        URLConnection vsixConnection;
        try {
            vsixConnection = vsixLocation.toURL().openConnection();
            vsixConnection.setConnectTimeout((int) Duration.ofSeconds(30).toMillis());
            vsixConnection.setReadTimeout((int) Duration.ofMinutes(5).toMillis());
        } catch (IOException e) {
            throw new RuntimeException("failed to download vsix", e);
        }
        Path downloadPath;
        try {
            downloadPath = Files.createTempFile("extension_", ".vsix");
        } catch (IOException e) {
            throw new RuntimeException("failed to download vsix", e);
        }
        try {
            try (
                var vsisStream = vsixConnection.getInputStream();
                var vsixChannel = Channels.newChannel(vsisStream);
                var downlodStream =  new FileOutputStream(downloadPath.toFile());
                var downlodChannel = downlodStream.getChannel();
            ) {
                downlodChannel.transferFrom(vsixChannel, 0, Long.MAX_VALUE);
            } catch (IOException e) {
                throw new RuntimeException("failed to download vsix", e);
            }

            var user = data.getOrAddUser(userJson);
            var namespace = repositories.findNamespace(namespaceName);
            data.ensureNamespaceMembership(user, namespace);

            var description = "MirrorExtensionVersion";
            var accessTokenValue = data.getOrAddAccessTokenValue(user, description);

            var token = users.useAccessToken(accessTokenValue);
            extensions.mirrorVersion(downloadPath, token, filename, json.timestamp);
            LOGGER.debug("completed mirroring of extension version: {}", json.namespace + "." + json.name + "-" + json.version + "@" + json.targetPlatform);
        } finally {
            try {
                Files.delete(downloadPath);
            } catch (IOException e) {
                LOGGER.error("failed to delete temp file", e);
            }
        }
    }

}
