/** ******************************************************************************
 * Copyright (c) 2022 Precies. Software and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 * ****************************************************************************** */
package org.eclipse.openvsx.storage;

import org.eclipse.openvsx.entities.Extension;
import org.eclipse.openvsx.entities.ExtensionVersion;
import org.eclipse.openvsx.entities.FileResource;
import org.eclipse.openvsx.entities.Namespace;
import org.eclipse.openvsx.search.DatabaseSearchService;
import org.eclipse.openvsx.search.RelevanceService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(SpringExtension.class)
@MockBean({ StorageUtilService.class })
public class AzureBlobStorageServiceTest {

    @Autowired
    AzureBlobStorageService service;

    @Test
    public void testGetLocation() {
        service.serviceEndpoint = "http://azure.blob.storage/";
        service.blobContainer = "blob-container";
        var namespace = new Namespace();
        namespace.setName("abelfubu");

        var extension = new Extension();
        extension.setName("abelfubu-dark");
        extension.setNamespace(namespace);

        var extVersion = new ExtensionVersion();
        extVersion.setVersion("1.3.4");
        extVersion.setTargetPlatform("universal");
        extVersion.setExtension(extension);

        var resource = new FileResource();
        resource.setName("extension/themes/abelFubu Dark+-color-theme.json");
        resource.setExtension(extVersion);

        var uri = service.getLocation(resource);
        var expected = URI.create("http://azure.blob.storage/blob-container/abelfubu/abelfubu-dark/1.3.4/extension/themes/abelFubu%20Dark+-color-theme.json");
        assertEquals(expected, uri);
    }

    @TestConfiguration
    static class TestConfig {
        @Bean
        public MeterRegistry registry() {
            return new SimpleMeterRegistry();
        }
        
        @Bean
        AzureBlobStorageService azureBlobStorageService() {
            return new AzureBlobStorageService();
        }
    }
}
