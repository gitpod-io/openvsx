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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Map;

import org.eclipse.openvsx.ExtensionService;
import org.eclipse.openvsx.LocalRegistryService;
import org.eclipse.openvsx.UserService;
import org.eclipse.openvsx.entities.ExtensionVersion;
import org.eclipse.openvsx.entities.Namespace;
import org.eclipse.openvsx.entities.NamespaceMembership;
import org.eclipse.openvsx.entities.UserData;
import org.eclipse.openvsx.json.UserJson;
import org.eclipse.openvsx.repositories.RepositoryService;
import org.eclipse.openvsx.util.ErrorResultException;
import org.eclipse.openvsx.util.TimeUtil;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.client.RestTemplate;

import io.micrometer.core.annotation.Timed;

public class MirrorExtensionVersionJob implements Job {

    protected final Logger logger = LoggerFactory.getLogger(MirrorExtensionVersionJob.class);

    @Autowired
    RestTemplate contentRestTemplate;

    @Autowired
    RestTemplate nonRedirectingRestTemplate;

    @Autowired
    DataMirrorService data;

    @Autowired
    RepositoryService repositories;

    @Autowired
    LocalRegistryService local;

    @Autowired
    UserService users;

    @Autowired
    ExtensionService extensions;

    @Override
    @Timed(longTask = true)
    public void execute(JobExecutionContext context) throws JobExecutionException {
        starting(context, logger);
        var map = context.getMergedJobDataMap();
        var download = map.getString("download");
        var userJson = new UserJson();
        userJson.provider = map.getString("userProvider");
        userJson.loginName = map.getString("userLoginName");
        userJson.fullName = map.getString("userFullName");
        userJson.avatarUrl = map.getString("userAvatarUrl");
        userJson.homepage = map.getString("userHomepage");
        var namespaceName = map.getString("namespace");

        var vsixResourceHeaders = nonRedirectingRestTemplate.headForHeaders(download);
        var vsixLocation = vsixResourceHeaders.getLocation();
        if (vsixLocation == null) {
            throw new JobExecutionException("Failed to parse location header from redirected vsix url");
        }

        var tokens = vsixLocation.getPath().split("/");
        var filename = tokens[tokens.length-1];
        if (!filename.endsWith(".vsix")) {
            throw new JobExecutionException("Invalid vsix filename from redirected vsix url");
        }

        var vsixPackage = contentRestTemplate.getForObject("{mirrorExtensionVersionUri}", byte[].class, Map.of("mirrorExtensionVersionUri", download));

        var user = data.getOrAddUser(userJson);
        var namespace = repositories.findNamespace(namespaceName);
        getOrAddNamespaceMembership(user, namespace);

        var description = "MirrorExtensionVersion";
        var accessTokenValue = data.getOrAddAccessTokenValue(user, description);

        ExtensionVersion extVersion;
        try(var input = new ByteArrayInputStream(vsixPackage)) {
            var token = users.useAccessToken(accessTokenValue);
            extVersion = extensions.mirrorVersion(input, token);
        } catch (IOException | ErrorResultException e) {
            throw new JobExecutionException(e);
        }

        var ext = extVersion.getExtension();
        data.updateMetadata(ext.getNamespace().getName(), ext.getName(), extVersion.getTargetPlatform(), extVersion.getVersion(), TimeUtil.toUTCString(extVersion.getTimestamp()), filename);
        completed(context, logger);
    }

    private void getOrAddNamespaceMembership(UserData user, Namespace namespace) {
        var membership = repositories.findMembership(user, namespace);
        if (membership == null) {
            users.addNamespaceMember(namespace, user, NamespaceMembership.ROLE_CONTRIBUTOR);
        }
    }
}
