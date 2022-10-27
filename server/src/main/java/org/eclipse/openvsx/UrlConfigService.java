/** ******************************************************************************
 * Copyright (c) 2022 Precies. Software and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 * ****************************************************************************** */
package org.eclipse.openvsx;

import org.eclipse.openvsx.util.ConfigCat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class UrlConfigService {

    @Value("${ovsx.upstream.url:}")
    String upstreamUrl;

    @Value("${ovsx.data.mirror.server-url:}")
    String mirrorServerUrl;


    @Autowired(required = false)
    ConfigCat configCat;

    public String getUpstreamUrl() {
        if (configCat == null) {
            return upstreamUrl;
        }
        return configCat.getUpstreamURL(upstreamUrl);
    }

    public String getMirrorServerUrl() {
        if (configCat == null) {
            return mirrorServerUrl;
        }
        return configCat.getUpstreamURL(mirrorServerUrl);
    }

}
