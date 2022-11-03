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

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.openvsx.UrlConfigService;
import org.eclipse.openvsx.util.NotFoundException;
import org.eclipse.openvsx.util.UrlUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.google.common.base.Strings;

@Component
public class UpstreamVSCodeService implements IVSCodeService {

    protected final Logger logger = LoggerFactory.getLogger(UpstreamVSCodeService.class);

    @Autowired
    RestTemplate restTemplate;

    @Autowired
    RestTemplate nonRedirectingRestTemplate;

    @Autowired
    UrlConfigService urlConfigService;

    public boolean isValid() {
        return !Strings.isNullOrEmpty(urlConfigService.getUpstreamUrl());
    }

    @Override
    public ExtensionQueryResult extensionQuery(ExtensionQueryParam param, int defaultPageSize) {
        var apiUrl = UrlUtil.createApiUrl(urlConfigService.getUpstreamUrl(), "vscode", "gallery", "extensionquery");
        var request = new RequestEntity<>(param, UrlUtil.getForwardedHeaders(), HttpMethod.POST, URI.create(apiUrl));
        ResponseEntity<ExtensionQueryResult> response;
        try {
            response = restTemplate.exchange(request, ExtensionQueryResult.class);
        } catch(RestClientException exc) {
            throw propagateRestException(exc, request.getMethod(), apiUrl, null);
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
        var urlTemplate = urlConfigService.getUpstreamUrl() + "/vscode/unpkg/{namespace}/{extension}/{version}";
        var uriVariables = new HashMap<String, String>(Map.of(
            "namespace", namespaceName,
            "extension", extensionName,
            "version", version
        ));

        if (path != null && !path.isBlank()) {
            var segIndex = 0;
            for (String segment : path.split("/")) {
                var varName = "seg" + segIndex;
                urlTemplate = urlTemplate + "/{" + varName + "}";
                uriVariables.put(varName, segment);
                segIndex++;
            }
        }

        ResponseEntity<byte[]> response;
        var method = HttpMethod.GET;
        try {
            response = nonRedirectingRestTemplate.exchange(urlTemplate, method, null, byte[].class, uriVariables);
        } catch(RestClientException exc) {
            throw propagateRestException(exc, method, urlTemplate, uriVariables);
        }

        var statusCode = response.getStatusCode();
        if(statusCode.is2xxSuccessful() || statusCode.is3xxRedirection()) {
            var headers = new HttpHeaders();
            headers.addAll(response.getHeaders());
            headers.remove(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN);
            headers.remove(HttpHeaders.VARY);
            return new ResponseEntity<>(response.getBody(), headers, response.getStatusCode());
        }
        if(statusCode.isError() && statusCode != HttpStatus.NOT_FOUND) {
            var url = UriComponentsBuilder.fromUriString(urlTemplate).build(uriVariables);
            logger.error("GET {}: {}", url, response);
        }

        throw new NotFoundException();
    }

    @Override
    public String download(String namespace, String extension, String version, String targetPlatform) {
        var urlTemplate = urlConfigService.getUpstreamUrl() + "/vscode/gallery/publishers/{namespace}/vsextensions/{extension}/{version}/vspackage?targetPlatform={targetPlatform}";
        var uriVariables = Map.of(
                "namespace", namespace,
                "extension", extension,
                "version", version,
                "targetPlatform", targetPlatform
        );

        ResponseEntity<Void> response;
        var method = HttpMethod.GET;
        try {
            response = nonRedirectingRestTemplate.exchange(urlTemplate, method, null, Void.class, uriVariables);
        } catch(RestClientException exc) {
            throw propagateRestException(exc, method, urlTemplate, uriVariables);
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
        var urlTemplate = urlConfigService.getUpstreamUrl() + "/vscode/item?itemName={namespace}.{extension}";
        var uriVariables = Map.of("namespace", namespace, "extension", extension);

        ResponseEntity<Void> response;
        var method = HttpMethod.GET;
        try {
            response = nonRedirectingRestTemplate.exchange(urlTemplate, method, null, Void.class, uriVariables);
        } catch (RestClientException exc) {
            throw propagateRestException(exc, method, urlTemplate, uriVariables);
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
        var urlTemplate = urlConfigService.getUpstreamUrl() + "/vscode/asset/{namespace}/{extension}/{version}/{assetType}";
        var uriVariables = new HashMap<String, String>(Map.of(
            "namespace", namespace,
            "extension", extensionName,
            "version", version,
            "assetType", assetType,
            "targetPlatform", targetPlatform
        ));

        if (restOfTheUrl != null && !restOfTheUrl.isBlank()) {
            var segIndex = 0;
            for (String segment : restOfTheUrl.split("/")) {
                var varName = "seg" + segIndex;
                urlTemplate = urlTemplate + "/{" + varName + "}";
                uriVariables.put(varName, segment);
                segIndex++;
            }
        }

        urlTemplate = urlTemplate + "?targetPlatform={targetPlatform}";

        ResponseEntity<byte[]> response;
        var method = HttpMethod.GET;
        try {
            response = nonRedirectingRestTemplate.exchange(urlTemplate, method, null, byte[].class, uriVariables);
        } catch (RestClientException exc) {
            throw propagateRestException(exc, method, urlTemplate, uriVariables);
        }

        var statusCode = response.getStatusCode();
        if(statusCode.is2xxSuccessful() || statusCode.is3xxRedirection()) {
            var headers = new HttpHeaders();
            headers.addAll(response.getHeaders());
            headers.remove(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN);
            headers.remove(HttpHeaders.VARY);
            return new ResponseEntity<>(response.getBody(), headers, response.getStatusCode());
        }
        if(statusCode.isError() && statusCode != HttpStatus.NOT_FOUND) {
            var url = UriComponentsBuilder.fromUriString(urlTemplate).build(uriVariables);
            logger.error("GET {}: {}", url, response);
        }

        throw new NotFoundException();
    }

    private NotFoundException propagateRestException(RestClientException exc, HttpMethod method, String urlTemplate,
        Map<String, String> uriVariables) {
        if (exc instanceof HttpStatusCodeException) {
            var statusCode = ((HttpStatusCodeException)exc).getStatusCode();
            if(statusCode == HttpStatus.NOT_FOUND) {
                return new NotFoundException();
            }
        }

        URI url;
        if (uriVariables != null) {
            url = UriComponentsBuilder.fromUriString(urlTemplate).build(uriVariables);
        } else {
            url = URI.create(urlTemplate);
        }
        logger.error("upstream: " + method + ": " + url, exc);
        return new NotFoundException();
    }

}
