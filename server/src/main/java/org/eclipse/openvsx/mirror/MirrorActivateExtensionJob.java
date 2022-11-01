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

import org.eclipse.openvsx.IExtensionRegistry;
import org.eclipse.openvsx.repositories.RepositoryService;
import org.eclipse.openvsx.util.NotFoundException;
import org.eclipse.openvsx.util.TargetPlatform;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import io.micrometer.core.annotation.Timed;

public class MirrorActivateExtensionJob implements Job {

    protected final Logger logger = LoggerFactory.getLogger(MirrorActivateExtensionJob.class);

    @Autowired
    DataMirrorService data;

    @Autowired
    RepositoryService repositories;

    @Autowired
    IExtensionRegistry mirror;

    @Override
    @Timed(longTask = true)
    public void execute(JobExecutionContext context) throws JobExecutionException {
        starting(context, logger);
        var extensions = repositories.findAllInactiveExtensions().toList();
        for (var extension : extensions) {
            var dbTotal = repositories.countVersions(extension);
            var namespace = extension.getNamespace().getName();
            var name = extension.getName();
            var total = 0;
            for (var targetPlatform : TargetPlatform.TARGET_PLATFORM_NAMES) {
                try {
                    var json = mirror.getExtension(namespace, name, targetPlatform);
                    total += json.allVersions.size() - 1; // Remove latest
                } catch (NotFoundException e) {
                    // combination of extension and target platform doesn't exist, try next
                }
            }
            if (dbTotal >= total) {
                data.activateExtension(namespace, name);
            }
        }
        completed(context, logger);
    }
}
