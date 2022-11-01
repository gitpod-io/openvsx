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

import static org.quartz.CronScheduleBuilder.cronSchedule;
import static org.quartz.JobBuilder.newJob;
import static org.quartz.TriggerBuilder.newTrigger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import org.eclipse.openvsx.entities.Extension;
import org.eclipse.openvsx.mirror.DataMirrorJob;
import org.eclipse.openvsx.mirror.MirrorExtensionJob;
import org.jooq.DSLContext;
import org.quartz.Job;
import org.quartz.JobKey;
import org.quartz.JobListener;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.TriggerKey;
import org.quartz.impl.matchers.GroupMatcher;
import org.quartz.listeners.BroadcastJobListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.util.Streamable;
import org.springframework.stereotype.Component;

@Component
public class SchedulerService {

    protected final Logger logger = LoggerFactory.getLogger(SchedulerService.class);

    @Autowired
    Scheduler scheduler;

    @Autowired
    AutowiringSpringBeanJobFactory jobFactory;

    @Autowired
    DSLContext dsl;

    @Value("${ovsx.data.mirror.enabled:false}")
    boolean mirrorModeEnabled;

    @PostConstruct
    public void init() throws SchedulerException {
        scheduler.setJobFactory(jobFactory);
        var listeners = new ArrayList<JobListener>();
        listeners.add(new RetryFailedJobListener(this, "RetryFailedJobs"));
        scheduler.getListenerManager().addJobListener(new BroadcastJobListener("Broadcaster", listeners));
    }

    public void unscheduleMirrorJobs() throws SchedulerException {
        var triggerKeys = scheduler.getTriggerKeys(GroupMatcher.groupEquals(JobUtil.Groups.MIRROR));
        scheduler.unscheduleJobs(new ArrayList<>(triggerKeys));
    }

    public void scheduleDataMirror(String schedule) throws SchedulerException {
        scheduleCronJob("DataMirror", "Data mirror", schedule, DataMirrorJob.class);
    }

    private void scheduleCronJob(String jobName, String description, String schedule, Class<? extends Job> jobClass) throws SchedulerException {
        var jobId = jobName + "Job";
        var jobKey = new JobKey(jobId, JobUtil.Groups.MIRROR);
        var job = scheduler.getJobDetail(jobKey);
        if(job == null) {
            job = newJob(jobClass)
                    .withIdentity(jobKey)
                    .withDescription(description)
                    .storeDurably()
                    .build();

            scheduler.addJob(job, false);
        }

        var triggerId = jobId + "Trigger";
        var triggerKey = new TriggerKey(triggerId, JobUtil.Groups.MIRROR);
        var trigger = scheduler.getTrigger(triggerKey);
        var cronTrigger = newTrigger()
                .withIdentity(triggerKey)
                .forJob(jobKey)
                .startNow()
                .withSchedule(cronSchedule(schedule))
                .build();

        if(trigger != null) {
            scheduler.rescheduleJob(triggerKey, cronTrigger);
            logger.info("++ Rescheduled {} [{}]", jobId, schedule);
        } else {
            scheduler.scheduleJob(cronTrigger);
            logger.info("++ Scheduled {} [{}]", jobId, schedule);
        }
    }

    public void scheduleMirrorExtension(String namespace, String extension, String schedule) throws SchedulerException {
        var jobId = "MirrorExtension::" + namespace + "." + extension;
        var jobKey = new JobKey(jobId, JobUtil.Groups.MIRROR);
        var job = scheduler.getJobDetail(jobKey);
        if(job == null) {
            job = newJob(MirrorExtensionJob.class)
                .withIdentity(jobKey)
                .withDescription(jobId)
                .usingJobData("namespace", namespace)
                .usingJobData("extension", extension)
                .storeDurably()
                .build();

            scheduler.addJob(job, false);
        }
        
        var triggerKey = new TriggerKey(jobId + "Trigger", JobUtil.Groups.MIRROR);
        var trigger = scheduler.getTrigger(triggerKey);
        if(trigger == null) {
            trigger = newTrigger()
                .withIdentity(triggerKey)
                .forJob(jobKey)
                .startNow()
                .withSchedule(cronSchedule(schedule))
                .build();
            scheduler.scheduleJob(trigger);
        }
    }

	public void unscheduleMirrorExtensions(Streamable<Extension> extensions) throws SchedulerException {
        var triggerKeys = extensions.stream().map(e -> {
            var jobId = "MirrorExtension::" + e.getNamespace().getName() + "." + e.getName();
            return new TriggerKey(jobId + "Trigger", JobUtil.Groups.MIRROR);
        }).collect(Collectors.toList());
        scheduler.unscheduleJobs(triggerKeys);
	}

}
