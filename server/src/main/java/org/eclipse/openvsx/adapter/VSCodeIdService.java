/********************************************************************************
 * Copyright (c) 2020 TypeFox and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/
package org.eclipse.openvsx.adapter;

import java.util.UUID;

import javax.transaction.Transactional;

import org.eclipse.openvsx.UrlConfigService;
import org.eclipse.openvsx.entities.Extension;
import org.eclipse.openvsx.repositories.RepositoryService;
import org.eclipse.openvsx.util.UrlUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;

@Component
public class VSCodeIdService {

    private static final String API_VERSION = "3.0-preview.1";

    protected final Logger logger = LoggerFactory.getLogger(VSCodeIdService.class);

    @Autowired
    RestTemplate restTemplate;

    @Autowired
    RestTemplate backgroundRestTemplate;

    @Autowired
    RepositoryService repositories;

    @Value("${ovsx.vscode.upstream.gallery-url:}")
    String upstreamUrl;

    @Value("${ovsx.data.mirror.enabled:false}")
    boolean mirrorModeEnabled;

    @Autowired
    UrlConfigService urlConfigService;

    public boolean setPublicIds(Extension extension) {
        var updateExistingPublicIds = false;
        var upstream = getUpstreamExtension(extension);
        if (upstream != null) {
            if (upstream.extensionId != null) {
                extension.setPublicId(upstream.extensionId);
                updateExistingPublicIds = true;
            }
            if (upstream.publisher != null && upstream.publisher.publisherId != null) {
                extension.getNamespace().setPublicId(upstream.publisher.publisherId);
                updateExistingPublicIds = true;
            }
        }
        if (extension.getPublicId() == null) {
            extension.setPublicId(createRandomId());
        }
        if (extension.getNamespace().getPublicId() == null) {
            extension.getNamespace().setPublicId(createRandomId());
        }

        return updateExistingPublicIds;
    }

    @Transactional
    public void updateExistingPublicIds(Extension extension) {
        var existingExtension = repositories.findExtensionByPublicId(extension.getPublicId());
        if(existingExtension != null && !existingExtension.equals(extension)) {
            existingExtension.setPublicId(createRandomId());
        }

        var existingNamespace = repositories.findNamespaceByPublicId(extension.getNamespace().getPublicId());
        if(existingNamespace != null && !existingNamespace.equals(extension.getNamespace())) {
            existingNamespace.setPublicId(createRandomId());
        }
    }

    private String createRandomId() {
        return UUID.randomUUID().toString();
    }

    private ExtensionQueryResult.Extension getUpstreamExtension(Extension extension) {
        String galleryUrl = upstreamUrl;
        if (mirrorModeEnabled) {
            galleryUrl = urlConfigService.getMirrorServerUrl() + "/vscode/gallery";
        }
        if (Strings.isNullOrEmpty(galleryUrl)) {
            return null;
        }
        try {
            var requestUrl = UrlUtil.createApiUrl(galleryUrl, "extensionquery");
            var requestData = createRequestData(extension);
            var headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set(HttpHeaders.ACCEPT, "application/json;api-version=" + API_VERSION);
            var template = mirrorModeEnabled ? backgroundRestTemplate : restTemplate;
            var result = template.postForObject(requestUrl, new HttpEntity<>(requestData, headers), ExtensionQueryResult.class);

            if (result.results != null && result.results.size() > 0) {
                var item = result.results.get(0);
                if (item.extensions != null && item.extensions.size() > 0) {
                    return item.extensions.get(0);
                }
            }
        } catch (RestClientException exc) {
            if (mirrorModeEnabled) {
                // This is critical in mirror mode so rethrow the error
                throw exc;
            }
            // TODO: it most likely should fail if an error is not 404
            // otherwise it can cause issues for some VS Code clients
            // i.e. users switching between OpenVSX and MS marketplafe via User Data settings sync
            logger.error("Failed to query extension id from upstream URL", exc);
        }
        return null;
    }

    private ExtensionQueryParam createRequestData(Extension extension) {
        var request = new ExtensionQueryParam();
        var filter = new ExtensionQueryParam.Filter();
        filter.criteria = Lists.newArrayList();
        var targetCriterion = new ExtensionQueryParam.Criterion();
        targetCriterion.filterType = ExtensionQueryParam.Criterion.FILTER_TARGET;
        targetCriterion.value = "Microsoft.VisualStudio.Code";
        filter.criteria.add(targetCriterion);
        var nameCriterion = new ExtensionQueryParam.Criterion();
        nameCriterion.filterType = ExtensionQueryParam.Criterion.FILTER_EXTENSION_NAME;
        nameCriterion.value = extension.getNamespace().getName() + "." + extension.getName();
        filter.criteria.add(nameCriterion);
        filter.pageNumber = 1;
        filter.pageSize = 1;
        request.filters = Lists.newArrayList(filter);
        return request;
    }

}
