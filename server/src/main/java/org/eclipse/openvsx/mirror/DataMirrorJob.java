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
import static org.eclipse.openvsx.util.UrlUtil.createApiUrl;

import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.eclipse.openvsx.AdminService;
import org.eclipse.openvsx.UrlConfigService;
import org.eclipse.openvsx.repositories.RepositoryService;
import org.eclipse.openvsx.schedule.SchedulerService;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.PersistJobDataAfterExecution;
import org.quartz.SchedulerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.RequestEntity;
import org.springframework.web.client.RestTemplate;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import io.micrometer.core.annotation.Timed;

@PersistJobDataAfterExecution
@DisallowConcurrentExecution
public class DataMirrorJob implements Job {
    protected final Logger logger = LoggerFactory.getLogger(DataMirrorJob.class);

    @Autowired
    RepositoryService repositories;

    @Autowired
    RestTemplate restTemplate;

    @Autowired
    SchedulerService schedulerService;

    @Autowired
    List<String> excludedExtensions;

    @Autowired
    List<String> includeExtensions;
    
    @Autowired
    UrlConfigService urlConfigService;

    @Value("${ovsx.data.mirror.schedule:}")
    String schedule;

    @Value("${ovsx.data.mirror.user-name}")
    String userName;

    @Autowired
    AdminService admin;

    @Override
    @Timed(longTask = true)
    public void execute(JobExecutionContext context) throws JobExecutionException {
        starting(context, logger);
        try {
            var extensionIds = new ArrayList<String>();
            try(var reader = new StringReader(getSitemap())) {
                var factory = DocumentBuilderFactory.newInstance();
                var builder = factory.newDocumentBuilder();
                var sitemap = builder.parse(new InputSource(reader));
                var urls = sitemap.getElementsByTagName("url");
                for(var i = 0; i < urls.getLength(); i++) {
                    var url = (Element) urls.item(i);
                    var location = URI.create(url.getElementsByTagName("loc").item(0).getTextContent());
                    var pathParams = location.getPath().split("/");
                    var namespace = pathParams[pathParams.length - 2];
                    var extension = pathParams[pathParams.length - 1];
                    var extensionId = String.join(".", namespace, extension);
                    if(excludedExtensions.contains(namespace + ".*") || excludedExtensions.contains(extensionId)) {
                        logger.debug("Excluded {} extension, skipping", extensionId);
                        continue;
                    }
                    if (!includeExtensions.isEmpty() && !includeExtensions.contains(namespace + ".*") && !includeExtensions.contains(extensionId)) {
                        logger.debug("Excluded {} extension, skipping", extensionId);
                        continue;
                    }
                    schedulerService.scheduleMirrorExtension(namespace, extension, schedule);
                    extensionIds.add(extensionId);
                }
            } catch (ParserConfigurationException | IOException | SAXException | SchedulerException e) {
                throw new JobExecutionException(e);
            }

            var notMatchingExtensions = repositories.findAllNotMatchingByExtensionId(extensionIds);
            if (!notMatchingExtensions.isEmpty()) {
                try {
                    schedulerService.unscheduleMirrorExtensions(notMatchingExtensions);
                    var mirrorUser = repositories.findUserByLoginName(null, userName);
                    for(var extension : notMatchingExtensions) {
                        admin.deleteExtension(extension.getNamespace().getName(), extension.getName(), mirrorUser);    
                    }
                } catch (SchedulerException e) {
                    throw new JobExecutionException(e);
                }
            }
        } finally {
            completed(context, logger);
        }
    }

    private String getSitemap() {
        var requestUrl = URI.create(createApiUrl(urlConfigService.getMirrorServerUrl(), "sitemap.xml"));
        var request = new RequestEntity<Void>(HttpMethod.GET, requestUrl);
        var response = restTemplate.exchange(request, String.class);
        return response.getStatusCode().is2xxSuccessful() ? response.getBody() : null;
    }
}
