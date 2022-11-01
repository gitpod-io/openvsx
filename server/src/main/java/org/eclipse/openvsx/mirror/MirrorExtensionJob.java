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

import java.util.Collections;
import java.util.stream.Collectors;

import org.eclipse.openvsx.AdminService;
import org.eclipse.openvsx.ExtensionService;
import org.eclipse.openvsx.IExtensionRegistry;
import org.eclipse.openvsx.entities.ExtensionVersion;
import org.eclipse.openvsx.entities.UserData;
import org.eclipse.openvsx.json.NamespaceJson;
import org.eclipse.openvsx.repositories.RepositoryService;
import org.eclipse.openvsx.schedule.SchedulerService;
import org.eclipse.openvsx.util.NotFoundException;
import org.eclipse.openvsx.util.TargetPlatform;
import org.eclipse.openvsx.util.VersionAlias;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.SchedulerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import io.micrometer.core.annotation.Timed;

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
    ExtensionService extensions;

    @Override
    @Timed(longTask = true)
    public void execute(JobExecutionContext context) throws JobExecutionException {
        starting(context, logger);
        var map = context.getMergedJobDataMap();
        var namespaceName = map.getString("namespace");
        var extensionName = map.getString("extension");
        if(repositories.findNamespace(namespaceName) == null) {
            var json = new NamespaceJson();
            json.name = namespaceName;
            admin.createNamespace(json);
        }

        var extension = data.getDeactivatedExtension(extensionName, namespaceName);
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

                var toAdd = versions.stream()
                        .filter(version -> targetVersions.stream().noneMatch(extVersion -> extVersion.getVersion().equals(version)))
                        .collect(Collectors.toList());

                if (toAdd.size() == 0) {
                    continue;
                }

                // create extension before version jobs start, to make sure extension is created with correct name
                // otherwise, mirrorExtensionVersion will create extension with data from .vsix file
                extensions.findOrCreateExtension(json.namespace, json.name);

                for (int i = toAdd.size() - 1; i >= 0; i--) {
                    var version = toAdd.get(i);
                    schedulerService.mirrorExtensionVersion(namespaceName, extensionName, version, targetPlatform);
                    // TODO: cleanup mirrorExtensionVersionJobKey if prevPublishExtensionVersionJobKey fails but after retries
                }
            } catch (SchedulerException e) {
                throw new RuntimeException(e);
            } catch (NotFoundException e) {
                // combination of extension and target platform doesn't exist, try next
            }
        }

        completed(context, logger);
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
}
