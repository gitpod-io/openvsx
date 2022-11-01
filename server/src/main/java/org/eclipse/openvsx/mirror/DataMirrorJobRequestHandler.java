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

import static org.eclipse.openvsx.util.UrlUtil.createApiUrl;

import java.io.StringReader;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilderFactory;

import org.eclipse.openvsx.AdminService;
import org.eclipse.openvsx.UrlConfigService;
import org.eclipse.openvsx.repositories.RepositoryService;
import org.eclipse.openvsx.schedule.Scheduler;
import org.jobrunr.jobs.annotations.Job;
import org.jobrunr.jobs.lambdas.JobRequestHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.RequestEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;

import io.micrometer.core.annotation.Timed;

@Component
public class DataMirrorJobRequestHandler implements JobRequestHandler<DataMirrorJobRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DataMirrorJobRequestHandler.class);

    @Value("${ovsx.data.mirror.enabled:false}")
    boolean enabled;

    @Value("${ovsx.data.mirror.schedule:}")
    String schedule;

    @Value("${ovsx.data.mirror.user-name:}")
    String userName;

    @Autowired
    Scheduler scheduler;

    @Autowired
    RepositoryService repositories;

    @Autowired
    RestTemplate restTemplate;

    @Autowired
    List<String> excludedExtensions;

    @Autowired
    List<String> includeExtensions;
    
    @Autowired
    UrlConfigService urlConfigService;

    @Autowired
    AdminService admin;

    @Override
    @Job(name="Data Mirror")
    @Timed(longTask = true)
    public void run(DataMirrorJobRequest jobRequest) throws Exception {
        if(!enabled) {
            scheduler.unscheduleDataMirror();
            return;
        }

        LOGGER.debug(">> Starting DataMirrorJob");
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
                        LOGGER.debug("Excluded {} extension, skipping", extensionId);
                        continue;
                    }
                    if (!includeExtensions.isEmpty() && !includeExtensions.contains(namespace + ".*") && !includeExtensions.contains(extensionId)) {
                        LOGGER.debug("Excluded {} extension, skipping", extensionId);
                        continue;
                    }
                    scheduler.enqueueMirrorExtension(namespace, extension);
                    extensionIds.add(extensionId);
                }
            }

            var notMatchingExtensions = repositories.findAllNotMatchingByExtensionId(extensionIds);
            if (!notMatchingExtensions.isEmpty()) {
                var mirrorUser = repositories.findUserByLoginName(null, userName);
                for(var extension : notMatchingExtensions) {
                    scheduler.unscheduleMirrorExtension(extension.getNamespace().getName(), extension.getName());
                    admin.deleteExtension(extension.getNamespace().getName(), extension.getName(), mirrorUser);
                }
            }
        } finally {
            LOGGER.debug("<< Completed DataMirrorJob");
        }
    }

    private String getSitemap() {
        var requestUrl = URI.create(createApiUrl(urlConfigService.getMirrorServerUrl(), "sitemap.xml"));
        var request = new RequestEntity<Void>(HttpMethod.GET, requestUrl);
        var response = restTemplate.exchange(request, String.class);
        return response.getStatusCode().is2xxSuccessful() ? response.getBody() : null;
    }
}
