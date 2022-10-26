/** ******************************************************************************
 * Copyright (c) 2022 Precies. Software and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 * ****************************************************************************** */
package org.eclipse.openvsx.adapter;

import com.google.common.base.Strings;

import org.eclipse.openvsx.util.NotFoundException;
import org.eclipse.openvsx.util.UrlUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

@Component
public class UpstreamVSCodeService implements IVSCodeService {

    protected final Logger logger = LoggerFactory.getLogger(UpstreamVSCodeService.class);

    @Autowired
    RestTemplate restTemplate;

    @Autowired
    RestTemplate nonRedirectingRestTemplate;

    @Value("${ovsx.upstream.url:}")
    String upstreamUrl;

    public boolean isValid() {
        return !Strings.isNullOrEmpty(upstreamUrl);
    }

    @Override
    public ExtensionQueryResult extensionQuery(ExtensionQueryParam param, int defaultPageSize) {
        var apiUrl = UrlUtil.createApiUrl(upstreamUrl, "vscode", "gallery", "extensionquery");
        var request = new RequestEntity<>(param, HttpMethod.POST, URI.create(apiUrl));
        ResponseEntity<ExtensionQueryResult> response;
        try {
            response = restTemplate.exchange(request, ExtensionQueryResult.class);
        } catch(RestClientException exc) {
            logger.error("POST " + apiUrl, exc);
            throw new NotFoundException();
        }

        var statusCode = response.getStatusCode();
        if(statusCode.is2xxSuccessful()) {
            return response.getBody();
        }
        if(statusCode.isError() && statusCode != HttpStatus.NOT_FOUND) {
            logger.error("POST {}: {}", apiUrl, response);
        }

        throw new NotFoundException();
    }

    @Override
    public ResponseEntity<byte[]> browse(String namespaceName, String extensionName, String version, String path) {
        var urlTemplate = upstreamUrl + "/vscode/unpkg/{namespace}/{extension}/{version}";
        var uriVariables = new HashMap<String, String>(Map.of(
            "namespace", namespaceName,
            "extension", extensionName,
            "version", version
        ));
        var segIndex = 0;
        for (String segment : path.split("/")) {
            var varName = "seg" + segIndex;
            urlTemplate = urlTemplate + "/{" + varName + "}";
            uriVariables.put(varName, segment);
            segIndex++;
        }

        ResponseEntity<byte[]> response;
        try {
            response = nonRedirectingRestTemplate.exchange(urlTemplate, HttpMethod.GET, null, byte[].class, uriVariables);
        } catch(RestClientException exc) {
            var url = UriComponentsBuilder.fromUriString(urlTemplate).build(uriVariables);
            logger.error("GET " + url, exc);
            throw new NotFoundException();
        }

        var statusCode = response.getStatusCode();
        if(statusCode.is2xxSuccessful() || statusCode.is3xxRedirection()) {
            return response;
        }
        if(statusCode.isError() && statusCode != HttpStatus.NOT_FOUND) {
            var url = UriComponentsBuilder.fromUriString(urlTemplate).build(uriVariables);
            logger.error("GET {}: {}", url, response);
        }

        throw new NotFoundException();
    }

    @Override
    public String download(String namespace, String extension, String version, String targetPlatform) {
        // TODO add targetPlatform as query parameter once upstream supports it
        var urlTemplate = upstreamUrl + "/vscode/gallery/publishers/{namespace}/vsextensions/{extension}/{version}/vspackage";
        var uriVariables = Map.of(
                "namespace", namespace,
                "extension", extension,
                "version", version
        );

        ResponseEntity<Void> response;
        try {
            response = nonRedirectingRestTemplate.exchange(urlTemplate, HttpMethod.GET, null, Void.class, uriVariables);
        } catch(RestClientException exc) {
            var url = UriComponentsBuilder.fromUriString(urlTemplate).build(uriVariables);
            logger.error("GET " + url, exc);
            throw new NotFoundException();
        }

        var statusCode = response.getStatusCode();
        if(statusCode.is3xxRedirection()) {
            return response.getHeaders().getLocation().toString();
        }
        if(statusCode.isError() && statusCode != HttpStatus.NOT_FOUND) {
            var url = UriComponentsBuilder.fromUriString(urlTemplate).build(uriVariables);
            logger.error("GET {}: {}", url, response);
        }

        throw new NotFoundException();
    }

    @Override
    public String getItemUrl(String namespace, String extension) {
        var urlTemplate = upstreamUrl + "/vscode/item?itemName={namespace}.{extension}";
        var uriVariables = Map.of("namespace", namespace, "extension", extension);

        ResponseEntity<Void> response;
        try {
            response = nonRedirectingRestTemplate.exchange(urlTemplate, HttpMethod.GET, null, Void.class, uriVariables);
        } catch (RestClientException exc) {
            var url = UriComponentsBuilder.fromUriString(urlTemplate).build(uriVariables);
            logger.error("GET " + url, exc);
            throw new NotFoundException();
        }

        var statusCode = response.getStatusCode();
        if(statusCode.is3xxRedirection()) {
            return response.getHeaders().getLocation().toString();
        }
        if(statusCode.isError() && statusCode != HttpStatus.NOT_FOUND) {
            var url = UriComponentsBuilder.fromUriString(urlTemplate).build(uriVariables);
            logger.error("GET {}: {}", url, response);
        }

        throw new NotFoundException();
    }

    @Override
    public ResponseEntity<byte[]> getAsset(String namespace, String extensionName, String version, String assetType, String targetPlatform, String restOfTheUrl) {
        // TODO add targetPlatform once upstream supports it
        var urlTemplate = upstreamUrl + "/vscode/asset/{namespace}/{extension}/{version}/{assetType}";
        var uriVariables = new HashMap<String, String>(Map.of(
            "namespace", namespace,
            "extension", extensionName,
            "version", version,
            "assetType", assetType
        ));
        var segIndex = 0;
        for (String segment : restOfTheUrl.split("/")) {
            var varName = "seg" + segIndex;
            urlTemplate = urlTemplate + "/{" + varName + "}";
            uriVariables.put(varName, segment);
            segIndex++;
        }

        ResponseEntity<byte[]> response;
        try {
            response = nonRedirectingRestTemplate.exchange(urlTemplate, HttpMethod.GET, null, byte[].class, uriVariables);
        } catch (RestClientException exc) {
            var url = UriComponentsBuilder.fromUriString(urlTemplate).build(uriVariables);
            logger.error("GET " + url, exc);
            throw new NotFoundException();
        }

        var statusCode = response.getStatusCode();
        if(statusCode.is2xxSuccessful() || statusCode.is3xxRedirection()) {
            return response;
        }
        if(statusCode.isError() && statusCode != HttpStatus.NOT_FOUND) {
            var url = UriComponentsBuilder.fromUriString(urlTemplate).build(uriVariables);
            logger.error("GET {}: {}", url, response);
        }

        throw new NotFoundException();
    }
}
