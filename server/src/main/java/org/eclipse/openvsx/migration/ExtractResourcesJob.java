/** ******************************************************************************
 * Copyright (c) 2022 Precies. Software and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 * ****************************************************************************** */
package org.eclipse.openvsx.migration;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import io.micrometer.core.annotation.Timed;

import java.io.IOException;

import static org.eclipse.openvsx.schedule.JobUtil.completed;
import static org.eclipse.openvsx.schedule.JobUtil.starting;

public class ExtractResourcesJob implements Job {

    protected final Logger logger = LoggerFactory.getLogger(ExtractResourcesJob.class);

    @Autowired
    ExtractResourcesService service;

    @Override

    @Timed(longTask = true)
    public void execute(JobExecutionContext context) throws JobExecutionException {
        starting(context, logger);
        var itemId = context.getMergedJobDataMap().getLong("itemId");
        try {
            service.extractResources(itemId);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        completed(context, logger);
    }
}
