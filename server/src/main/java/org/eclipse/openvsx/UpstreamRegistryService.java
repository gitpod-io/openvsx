/********************************************************************************
 * Copyright (c) 2019 TypeFox and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/
package org.eclipse.openvsx;

import static org.eclipse.openvsx.util.UrlUtil.addQuery;
import static org.eclipse.openvsx.util.UrlUtil.createApiUrl;

import java.net.URI;
import java.util.HashMap;

import com.google.common.base.Strings;

import org.apache.commons.lang3.ArrayUtils;
import org.eclipse.openvsx.util.TargetPlatform;
import org.eclipse.openvsx.util.UrlUtil;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import org.eclipse.openvsx.json.ExtensionJson;
import org.eclipse.openvsx.json.NamespaceJson;
import org.eclipse.openvsx.json.QueryParamJson;
import org.eclipse.openvsx.json.QueryResultJson;
import org.eclipse.openvsx.json.ReviewListJson;
import org.eclipse.openvsx.json.SearchResultJson;
import org.eclipse.openvsx.search.ISearchService;
import org.eclipse.openvsx.util.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UpstreamRegistryService implements IExtensionRegistry {

    protected final Logger logger = LoggerFactory.getLogger(UpstreamRegistryService.class);

    private RestTemplate restTemplate;
    private String upstreamUrl;

    public UpstreamRegistryService(RestTemplate restTemplate, String upstreamUrl) {
        this.restTemplate = restTemplate;
        this.upstreamUrl = upstreamUrl;
    }

    public boolean isValid() {
        return !Strings.isNullOrEmpty(upstreamUrl);
    }

    @Override
    public NamespaceJson getNamespace(String namespace) {
        var requestUrl = createApiUrl(upstreamUrl, "api", namespace);
        try {
            return restTemplate.getForObject(requestUrl, NamespaceJson.class);
        } catch (RestClientException exc) {
            if(!isNotFound(exc)) {
                logger.error("GET " + requestUrl, exc);
            }

            throw new NotFoundException();
        }
    }

    @Override
    public ExtensionJson getExtension(String namespace, String extension, String targetPlatform) {
        var segments = new String[]{ "api", namespace, extension };
        if(targetPlatform != null) {
            segments = ArrayUtils.add(segments, targetPlatform);
        }

        var requestUrl = createApiUrl(upstreamUrl, segments);
        try {
            var json = restTemplate.getForObject(requestUrl, ExtensionJson.class);
            makeDownloadsCompatible(json);
            return json;
        } catch (RestClientException exc) {
            if(!isNotFound(exc)) {
                logger.error("GET " + requestUrl, exc);
            }

            throw new NotFoundException();
        }
    }

    @Override
    public ExtensionJson getExtension(String namespace, String extension, String targetPlatform, String version) {
        var requestUrl = UrlUtil.createApiVersionUrl(upstreamUrl, namespace, extension, targetPlatform, version);
        try {
            var json = restTemplate.getForObject(requestUrl, ExtensionJson.class);
            makeDownloadsCompatible(json);
            return json;
        } catch (RestClientException exc) {
            if(!isNotFound(exc)) {
                logger.error("GET " + requestUrl, exc);
            }

            throw new NotFoundException();
        }
    }

    @Override
    public ResponseEntity<byte[]> getFile(String namespace, String extension, String targetPlatform, String version, String fileName) {
        return getFile(UrlUtil.createApiFileUrl(upstreamUrl, namespace, extension, targetPlatform, version, fileName));
    }

    private ResponseEntity<byte[]> getFile(String url) {
        var upstreamLocation = URI.create(url);
        var request = new RequestEntity<Void>(HttpMethod.HEAD, upstreamLocation);
        ResponseEntity<byte[]> response;
        try {
            response = restTemplate.exchange(request, byte[].class);
        } catch(RestClientException exc) {
            if(!isNotFound(exc)) {
                logger.error("HEAD " + url, exc);
            }

            throw new NotFoundException();
        }
        var statusCode = response.getStatusCode();
        if (statusCode.is2xxSuccessful()) {
            return ResponseEntity.status(HttpStatus.FOUND)
                    .location(upstreamLocation)
                    .build();
        }
        if (statusCode.is3xxRedirection()) {
            return response;
        }
        if (statusCode.isError() && statusCode != HttpStatus.NOT_FOUND) {
            logger.error("HEAD {}: {}", url, response);
        }
        throw new NotFoundException();
    }

    @Override
    public ReviewListJson getReviews(String namespace, String extension) {
        var requestUrl = createApiUrl(upstreamUrl, "api", namespace, extension, "reviews");
        try {
            return restTemplate.getForObject(requestUrl, ReviewListJson.class);
        } catch (RestClientException exc) {
            if(!isNotFound(exc)) {
                logger.error("GET " + requestUrl, exc);
            }

            throw new NotFoundException();
        }
    }

	@Override
	public SearchResultJson search(ISearchService.Options options) {
        var searchUrl = createApiUrl(upstreamUrl, "api", "-", "search");
        var requestUrl = addQuery(searchUrl,
                "query", options.queryString,
                "category", options.category,
                "size", Integer.toString(options.requestedSize),
                "offset", Integer.toString(options.requestedOffset),
                "sortOrder", options.sortOrder,
                "sortBy", options.sortBy,
                "includeAllVersions", Boolean.toString(options.includeAllVersions),
                "targetPlatform", options.targetPlatform
        );

        try {
            return restTemplate.getForObject(requestUrl, SearchResultJson.class);
        } catch (RestClientException exc) {
            if(!isNotFound(exc)) {
                logger.error("GET " + requestUrl, exc);
            }

            throw new NotFoundException();
        }
    }

    @Override
    public QueryResultJson query(QueryParamJson param) {
        var requestUrl = createApiUrl(upstreamUrl, "api", "-", "query");
        try {
            return restTemplate.postForObject(requestUrl, param, QueryResultJson.class);
        } catch (RestClientException exc) {
            if(!isNotFound(exc)) {
                logger.error("POST " + requestUrl, exc);
            }

            throw new NotFoundException();
        }
    }

    private void makeDownloadsCompatible(ExtensionJson json) {
        if(json.downloads == null && json.files.containsKey("download")) {
            json.downloads = new HashMap<>();
            json.downloads.put(TargetPlatform.NAME_UNIVERSAL, json.files.get("download"));
        }
    }

    private boolean isNotFound(RestClientException exc) {
        return exc instanceof HttpStatusCodeException
                && ((HttpStatusCodeException) exc).getStatusCode() == HttpStatus.NOT_FOUND;
    }
}