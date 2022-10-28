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

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MirrorConfiguration {

    @Bean
    @ConfigurationProperties(prefix = "ovsx.data.mirror.excluded-extensions")
    public List<String> excludedExtensions(){
        return new ArrayList<>();
    }

    @Bean
    @ConfigurationProperties(prefix = "ovsx.data.mirror.include-extensions")
    public List<String> includeExtensions(){
        return new ArrayList<>();
    }
}
