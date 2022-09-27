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

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import com.google.common.base.Strings;

import org.eclipse.openvsx.util.TargetPlatform;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.http.*;
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
import org.springframework.web.util.UriComponentsBuilder;

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
        var urlTemplate = upstreamUrl + "/api/{namespace}";
        var uriVariables = Map.of("namespace", namespace);
        try {
            return restTemplate.getForObject(urlTemplate, NamespaceJson.class, uriVariables);
        } catch (RestClientException exc) {
            if(!isNotFound(exc)) {
                var url = UriComponentsBuilder.fromUriString(urlTemplate).build(uriVariables);
                logger.error("GET " + url, exc);
            }
            
            throw new NotFoundException();
        }
    }

    @Override
    public ExtensionJson getExtension(String namespace, String extension, String targetPlatform) {
        var urlTemplate = upstreamUrl + "/api/{namespace}/{extension}";
        var uriVariables = new HashMap<String, String>();
        uriVariables.put("namespace", namespace);
        uriVariables.put("extension", extension);
        if(targetPlatform != null) {
            urlTemplate += "/{targetPlatform}";
            uriVariables.put("targetPlatform", targetPlatform);
        }

        try {
            var json = restTemplate.getForObject(urlTemplate, ExtensionJson.class, uriVariables);
            makeDownloadsCompatible(json);
            return json;
        } catch (RestClientException exc) {
            if(!isNotFound(exc)) {
                var url = UriComponentsBuilder.fromUriString(urlTemplate).build(uriVariables);
                logger.error("GET " + url, exc);
            }
            
            throw new NotFoundException();
        }
    }

    @Override
    public ExtensionJson getExtension(String namespace, String extension, String targetPlatform, String version) {
        var urlTemplate = upstreamUrl + "/api/{namespace}/{extension}";
        var uriVariables = new HashMap<String, String>();
        uriVariables.put("namespace", namespace);
        uriVariables.put("extension", extension);
        if(targetPlatform != null) {
            urlTemplate += "/{targetPlatform}";
            uriVariables.put("targetPlatform", targetPlatform);
        }
        if(version != null) {
            urlTemplate += "/{version}";
            uriVariables.put("version", version);
        }

        try {
            var json = restTemplate.getForObject(urlTemplate, ExtensionJson.class, uriVariables);
            makeDownloadsCompatible(json);
            return json;
        } catch (RestClientException exc) {
            if(!isNotFound(exc)) {
                var url = UriComponentsBuilder.fromUriString(urlTemplate).build(uriVariables);
                logger.error("GET " + url, exc);
            }
            
            throw new NotFoundException();
        }
    }

    @Override
    public ResponseEntity<byte[]> getFile(String namespace, String extension, String targetPlatform, String version, String fileName) {
        var urlTemplate = upstreamUrl + "/api/{namespace}/{extension}";
        var uriVariables = new HashMap<String, String>();
        uriVariables.put("namespace", namespace);
        uriVariables.put("extension", extension);
        if(TargetPlatform.isUniversal(targetPlatform)) {
            targetPlatform = null;
        }
        if(targetPlatform != null) {
            urlTemplate += "/{targetPlatform}";
            uriVariables.put("targetPlatform", targetPlatform);
        }
        if(version != null) {
            urlTemplate += "/{version}";
            uriVariables.put("version", version);
        }

        urlTemplate += "/file/{fileName}";
        uriVariables.put("fileName", fileName);
        return getFile(urlTemplate, uriVariables);
    }

    private ResponseEntity<byte[]> getFile(String urlTemplate, Map<String, ?> uriVariables) {
        ResponseEntity<byte[]> response;
        try {
            response = restTemplate.exchange(urlTemplate, HttpMethod.HEAD, null, byte[].class, uriVariables);
        } catch(RestClientException exc) {
            if(!isNotFound(exc)) {
                var url = UriComponentsBuilder.fromUriString(urlTemplate).build(uriVariables);
                logger.error("HEAD " + url, exc);
            }
            
            throw new NotFoundException();
        }
        var statusCode = response.getStatusCode();
        if (statusCode.is2xxSuccessful()) {
            return ResponseEntity.status(HttpStatus.FOUND)
                    .location(UriComponentsBuilder.fromHttpUrl(urlTemplate).build(uriVariables))
                    .build();
        }
        if (statusCode.is3xxRedirection()) {
            return response;
        }
        if (statusCode.isError() && statusCode != HttpStatus.NOT_FOUND) {
            var url = UriComponentsBuilder.fromUriString(urlTemplate).build(uriVariables);
            logger.error("HEAD {}: {}", url, response);
        }
        throw new NotFoundException();
    }

    @Override
    public ReviewListJson getReviews(String namespace, String extension) {
        var urlTemplate = upstreamUrl + "/api/{namespace}/{extension}/reviews";
        var uriVariables = new HashMap<String, String>();
        uriVariables.put("namespace", namespace);
        uriVariables.put("extension", extension);

        try {
            return restTemplate.getForObject(urlTemplate, ReviewListJson.class, uriVariables);
        } catch (RestClientException exc) {
            if(!isNotFound(exc)) {
                var url = UriComponentsBuilder.fromUriString(urlTemplate).build(uriVariables);
                logger.error("GET " + url, exc);
            }
            
            throw new NotFoundException();
        }
    }

	@Override
	public SearchResultJson search(ISearchService.Options options) {
        var urlTemplate = upstreamUrl + "/api/-/search";
        var uriVariables = new HashMap<String, String>();
        uriVariables.put("size", Integer.toString(options.requestedSize));
        uriVariables.put("offset", Integer.toString(options.requestedOffset));
        uriVariables.put("includeAllVersions", Boolean.toString(options.includeAllVersions));
        if(options.queryString != null) {
            uriVariables.put("query", options.queryString);
        }
        if(options.category != null) {
            uriVariables.put("category", options.category);
        }
        if(options.sortOrder != null) {
            uriVariables.put("sortOrder", options.sortOrder);
        }
        if(options.sortBy != null) {
            uriVariables.put("sortBy", options.sortBy);
        }
        if(options.targetPlatform != null) {
            uriVariables.put("targetPlatform", options.targetPlatform);
        }

        var queryString = uriVariables.keySet().stream()
                .map(queryParam -> queryParam + "={" + queryParam + "}")
                .collect(Collectors.joining("&"));
        if(!queryString.isEmpty()) {
            urlTemplate += "?" + queryString;
        }

        try {
            return restTemplate.getForObject(urlTemplate, SearchResultJson.class, uriVariables);
        } catch (RestClientException exc) {
            if(!isNotFound(exc)) {
                var url = UriComponentsBuilder.fromUriString(urlTemplate).build(uriVariables);
                logger.error("GET " + url, exc);
            }
            
            throw new NotFoundException();
        }
    }

    @Override
    public QueryResultJson query(QueryParamJson param) {
        var urlTemplate = upstreamUrl + "/api/-/query";
        var uriVariables = new HashMap<String, String>();
        uriVariables.put("includeAllVersions", Boolean.toString(param.includeAllVersions));
        if(param.namespaceName != null) {
            uriVariables.put("namespaceName", param.namespaceName);
        }
        if(param.extensionName != null) {
            uriVariables.put("extensionName", param.extensionName);
        }
        if(param.extensionVersion != null) {
            uriVariables.put("extensionVersion", param.extensionVersion);
        }
        if(param.extensionId != null) {
            uriVariables.put("extensionId", param.extensionId);
        }
        if(param.extensionUuid != null) {
            uriVariables.put("extensionUuid", param.extensionUuid);
        }
        if(param.namespaceUuid != null) {
            uriVariables.put("namespaceUuid", param.namespaceUuid);
        }
        if(param.targetPlatform != null) {
            uriVariables.put("targetPlatform", param.targetPlatform);
        }

        var queryString = uriVariables.keySet().stream()
                .map(queryParam -> queryParam + "={" + queryParam + "}")
                .collect(Collectors.joining("&"));
        if(!queryString.isEmpty()) {
            urlTemplate += "?" + queryString;
        }

        try {
            return restTemplate.getForObject(urlTemplate, QueryResultJson.class, uriVariables);
        } catch (RestClientException exc) {
            if(!isNotFound(exc)) {
                var url = UriComponentsBuilder.fromUriString(urlTemplate).build(uriVariables);
                logger.error("POST " + url, exc);
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