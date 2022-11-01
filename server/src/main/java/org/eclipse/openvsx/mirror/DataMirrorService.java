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

import java.util.AbstractMap;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.transaction.Transactional;

import org.eclipse.openvsx.AdminService;
import org.eclipse.openvsx.ExtensionService;
import org.eclipse.openvsx.IExtensionRegistry;
import org.eclipse.openvsx.LocalRegistryService;
import org.eclipse.openvsx.UserService;
import org.eclipse.openvsx.entities.Extension;
import org.eclipse.openvsx.entities.ExtensionReview;
import org.eclipse.openvsx.entities.ExtensionVersion;
import org.eclipse.openvsx.entities.FileResource;
import org.eclipse.openvsx.entities.Namespace;
import org.eclipse.openvsx.entities.NamespaceMembership;
import org.eclipse.openvsx.entities.PersonalAccessToken;
import org.eclipse.openvsx.entities.UserData;
import org.eclipse.openvsx.json.NamespaceJson;
import org.eclipse.openvsx.json.ReviewJson;
import org.eclipse.openvsx.json.UserJson;
import org.eclipse.openvsx.repositories.RepositoryService;
import org.eclipse.openvsx.util.TimeUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(value = "ovsx.data.mirror.enabled", havingValue = "true")
public class DataMirrorService {

    protected final Logger logger = LoggerFactory.getLogger(DataMirrorService.class);

    @Autowired
    RepositoryService repositories;

    @Autowired
    EntityManager entityManager;

    @Autowired
    UserService users;

    @Autowired
    ExtensionService extensions;

    @Autowired
    IExtensionRegistry mirror;

    @Autowired
    LocalRegistryService local;

    @Value("${ovsx.data.mirror.user-name}")
    String userName;

    @Value("${ovsx.data.mirror.schedule}")
    String schedule;

    @Autowired
    AdminService admin;

    public String getSchedule() {
        return schedule;
    }

    public String getUserName() {
        return schedule;
    }

    public IExtensionRegistry getMirror() {
        return mirror;
    }

    @Transactional
    public void createMirrorUser() {
        if(repositories.findUserByLoginName(null, userName) == null) {
            var user = new UserData();
            user.setLoginName(userName);
            entityManager.persist(user);
        }
    }

    @Transactional
    public UserData getOrAddUser(UserJson json) {
        var user = repositories.findUserByLoginName(json.provider, json.loginName);
        if(user == null) {
            // TODO do we need all of the data?
            // TODO Is this legal (GDPR, other laws)? I'm not a lawyer.
            user = new UserData();
            user.setLoginName(json.loginName);
            user.setFullName(json.fullName);
            user.setAvatarUrl(json.avatarUrl);
            user.setProviderUrl(json.homepage);
            user.setProvider(json.provider);
            entityManager.persist(user);
        }

        return user;
    }

    @Transactional
    public void updateMetadata(String namespaceName, String extensionName, String targetPlatform, String version, String timestamp, String filename) {
        var extVersion = repositories.findVersion(version, targetPlatform, extensionName, namespaceName);
        extVersion.setTimestamp(TimeUtil.fromUTCString(timestamp));
        var extension = extVersion.getExtension();
        if(extension.getPublishedDate().equals(extension.getLastUpdatedDate())) {
            extension.setPublishedDate(extVersion.getTimestamp());
        }

        extension.setLastUpdatedDate(extVersion.getTimestamp());

        var resource = repositories.findFileByType(extVersion, FileResource.DOWNLOAD);
        resource.setName(filename);
    }

    public String getOrAddAccessTokenValue(UserData user, String description) {
        return repositories.findAccessTokens(user)
                .filter(PersonalAccessToken::isActive)
                .filter(token -> token.getDescription().equals(description))
                .stream()
                .findFirst()
                .map(PersonalAccessToken::getValue)
                .orElse(users.createAccessToken(user, description).value);
    }

    @Transactional
    public void activateExtension(String namespaceName, String extensionName) {
        var extension = repositories.findExtension(extensionName, namespaceName);
        extension.getVersions().stream().forEach(extVersion -> extVersion.setActive(true));
        extensions.updateExtension(extension);
    }

    @Transactional
    public void updateMetadata(String namespaceName, String extensionName) {
        var extension = repositories.findExtension(extensionName, namespaceName);
        if(extension == null) {
            // extension has been deleted in the meantime
            return;
        }

        var json = mirror.getExtension(namespaceName, extensionName, null);
        extension.setDownloadCount(json.downloadCount);
        extension.setAverageRating(json.averageRating);

        var remoteReviews = mirror.getReviews(namespaceName, extensionName);
        var localReviews = repositories.findAllReviews(extension)
                .map(review -> new AbstractMap.SimpleEntry<>(review.toReviewJson(), review));

        remoteReviews.reviews.stream()
                .filter(review -> localReviews.stream().noneMatch(entry -> entry.getKey().equals(review)))
                .forEach(review -> addReview(review, extension));

        localReviews.stream()
                .filter(entry -> remoteReviews.reviews.stream().noneMatch(review -> review.equals(entry.getKey())))
                .map(Map.Entry::getValue)
                .forEach(entityManager::remove);
    }

    private void addReview(ReviewJson json, Extension extension) {
        var review = new ExtensionReview();
        review.setExtension(extension);
        review.setActive(true);
        review.setTimestamp(TimeUtil.fromUTCString(json.timestamp));
        review.setUser(getOrAddUser(json.user));
        review.setTitle(json.title);
        review.setComment(json.comment);
        review.setRating(json.rating);
        entityManager.persist(review);
    }

    public void deleteExtensionVersion(ExtensionVersion extVersion, UserData user) {
        var extension = extVersion.getExtension();
        admin.deleteExtension(
                extension.getNamespace().getName(),
                extension.getName(),
                extVersion.getTargetPlatform(),
                extVersion.getVersion(),
                user
        );
    }

    public void mirrorNamespaceVerified(String namespaceName) {
        var remoteVerified = mirror.getNamespace(namespaceName).verified;
        var localVerified = local.getNamespace(namespaceName).verified;
        if(!localVerified && remoteVerified) {
            // verify the namespace by adding an owner to it
            var namespace = repositories.findNamespace(namespaceName);
            var memberships = repositories.findMemberships(namespace);
            users.addNamespaceMember(namespace, memberships.toList().get(0).getUser(), NamespaceMembership.ROLE_OWNER);
        }
        if(localVerified && !remoteVerified) {
            // unverify namespace by changing owner(s) back to contributor
            var namespace = repositories.findNamespace(namespaceName);
            repositories.findMemberships(namespace, NamespaceMembership.ROLE_OWNER)
                    .forEach(membership -> users.addNamespaceMember(namespace, membership.getUser(), NamespaceMembership.ROLE_CONTRIBUTOR));
        }
    }

    public void ensureNamespaceMembership(UserData user, Namespace namespace) {
        var membership = repositories.findMembership(user, namespace);
        if (membership == null) {
            users.addNamespaceMember(namespace, user, NamespaceMembership.ROLE_CONTRIBUTOR);
        }
    }

    public void ensureNamespace(String namespaceName) {
        if(repositories.findNamespace(namespaceName) == null) {
            var json = new NamespaceJson();
            json.name = namespaceName;
            admin.createNamespace(json);
        }
    }
}
