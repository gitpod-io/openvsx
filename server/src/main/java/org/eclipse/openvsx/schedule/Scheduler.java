/** ******************************************************************************
 * Copyright (c) 2022 Precies. Software Ltd and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 * ****************************************************************************** */
package org.eclipse.openvsx.schedule;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import javax.persistence.EntityManager;

import org.eclipse.openvsx.migration.ExtractResourcesJobRequest;
import org.eclipse.openvsx.mirror.DataMirrorJobRequest;
import org.eclipse.openvsx.mirror.MirrorExtensionJobRequest;
import org.jobrunr.scheduling.JobRequestScheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class Scheduler {

    private static final Logger LOGGER = LoggerFactory.getLogger(Scheduler.class);

    @Autowired
    JobRequestScheduler scheduler;

    @Autowired
    RecurringJobRepository recurringJobs;

    @Autowired
    EntityManager entityManager;

    public void enqueueExtractResourcesMigration(long id) {
        scheduler.enqueue(toUUID("ExtractResourcesMigration::itemId=" + id), new ExtractResourcesJobRequest(id));
        LOGGER.debug("++ Scheduled ExtractResourcesMigration::itemId={}", id);
    }

    public void scheduleDataMirror(String schedule) {
        scheduler.scheduleRecurrently("DataMirror", schedule, new DataMirrorJobRequest());
        LOGGER.debug("++ Scheduled DataMirror");
    }

    public void unscheduleDataMirror() {
        scheduler.delete("DataMirror");
        LOGGER.debug("++ Unscheduled DataMirror");
    }

    public void enqueueMirrorExtension(String namespace, String extension) {
        var id = "MirrorExtension" + namespace + extension;
        scheduler.enqueue(toUUID(id), new MirrorExtensionJobRequest(namespace, extension));
        LOGGER.debug("++ Scheduled " + id);
    }

    public void unscheduleMirrorExtension(String namespace, String extension) {
        var id = "MirrorExtension" + namespace + extension;
        try {
            scheduler.delete(toUUID(id));
        } catch (Throwable t) {
            // no-op
        }
        LOGGER.debug("++ Unscheduled " + id);
    }

    private UUID toUUID(String jobId) {
        return UUID.nameUUIDFromBytes(jobId.getBytes(StandardCharsets.UTF_8));
    }
}
