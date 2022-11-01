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

import java.net.HttpURLConnection;
import java.time.Duration;

import javax.sql.DataSource;

import org.eclipse.openvsx.mirror.ReadOnlyRequestFilter;
import org.eclipse.openvsx.web.ShallowEtagHeaderFilter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.DefaultUriBuilderFactory;

import io.micrometer.core.aop.TimedAspect;
import io.micrometer.core.instrument.MeterRegistry;
import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.provider.jdbctemplate.JdbcTemplateLockProvider;
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;

@SpringBootApplication
@EnableScheduling
@EnableRetry
@EnableCaching(proxyTargetClass = true)
@EnableSchedulerLock(defaultLockAtMostFor = "5m")
public class RegistryApplication {

    public static void main(String[] args) {
        SpringApplication.run(RegistryApplication.class, args);
    }

    @Bean
    public TimedAspect timedAspect(MeterRegistry registry) {
        return new TimedAspect(registry);
    }

	@Bean
	public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
            .setConnectTimeout(Duration.ofSeconds(10))
            .setReadTimeout(Duration.ofSeconds(10))
            .messageConverters(
                new ByteArrayHttpMessageConverter(),
                new StringHttpMessageConverter(),
                new MappingJackson2HttpMessageConverter())
            .build();
    }

    @Bean
    public RestTemplate nonRedirectingRestTemplate(RestTemplateBuilder builder) {
        return builder
            .setConnectTimeout(Duration.ofSeconds(10))
            .setReadTimeout(Duration.ofSeconds(10))
            .requestFactory(() -> new SimpleClientHttpRequestFactory() {
                @Override
                protected void prepareConnection(HttpURLConnection connection, String httpMethod) {
                    connection.setInstanceFollowRedirects(false);
                }})
            .build();
    }

	@Bean
	public RestTemplate contentRestTemplate(RestTemplateBuilder builder) {
        DefaultUriBuilderFactory defaultUriBuilderFactory = new DefaultUriBuilderFactory();
        defaultUriBuilderFactory.setEncodingMode(DefaultUriBuilderFactory.EncodingMode.NONE);
        return builder
            .uriTemplateHandler(defaultUriBuilderFactory)
            .setConnectTimeout(Duration.ofSeconds(30))
            .setReadTimeout(Duration.ofMinutes(5))
            .messageConverters(
                new ByteArrayHttpMessageConverter(),
                new StringHttpMessageConverter(),
                new MappingJackson2HttpMessageConverter())
            .build();
    }

	@Bean
	public RestTemplate contentNonRedirectingRestTemplate(RestTemplateBuilder builder) {
        DefaultUriBuilderFactory defaultUriBuilderFactory = new DefaultUriBuilderFactory();
        defaultUriBuilderFactory.setEncodingMode(DefaultUriBuilderFactory.EncodingMode.NONE);
        return builder
            .uriTemplateHandler(defaultUriBuilderFactory)
            .setConnectTimeout(Duration.ofSeconds(10))
            .setReadTimeout(Duration.ofSeconds(10))
            .requestFactory(() -> new SimpleClientHttpRequestFactory() {
                @Override
                protected void prepareConnection(HttpURLConnection connection, String httpMethod) {
                    connection.setInstanceFollowRedirects(false);
                }})
            .build();
    }
    
    @Bean
    public TaskScheduler taskScheduler() {
        return new ThreadPoolTaskScheduler();
    }

    @Bean
    public LockProvider lockProvider(DataSource dataSource) {
        return new JdbcTemplateLockProvider(
                JdbcTemplateLockProvider.Configuration.builder()
                        .withJdbcTemplate(new JdbcTemplate(dataSource))
                        .usingDbTime()
                        .build()
        );
    }

    @Bean
    public UpstreamRegistryService upstream(RestTemplate restTemplate, UrlConfigService urlConfigService) {
        return new UpstreamRegistryService(restTemplate, urlConfigService);
    }

    @Bean
    public FilterRegistrationBean<ShallowEtagHeaderFilter> shallowEtagHeaderFilter() {
        var registrationBean = new FilterRegistrationBean<ShallowEtagHeaderFilter>();
        registrationBean.setFilter(new ShallowEtagHeaderFilter());
        registrationBean.addUrlPatterns("/api/*");
        registrationBean.setOrder(Ordered.LOWEST_PRECEDENCE);

        return registrationBean;
    }

    @Bean
    @ConditionalOnProperty(value = "ovsx.data.mirror.enabled", havingValue = "true")
    public FilterRegistrationBean<ReadOnlyRequestFilter> readOnlyRequestFilter(
            @Value("${ovsx.data.mirror.read-only.allowed-endpoints}") String[] allowedEndpoints,
            @Value("${ovsx.data.mirror.read-only.disallowed-methods}") String[] disallowedMethods
    ) {
        var registrationBean = new FilterRegistrationBean<ReadOnlyRequestFilter>();
        registrationBean.setFilter(new ReadOnlyRequestFilter(allowedEndpoints, disallowedMethods));
        registrationBean.setOrder(Ordered.LOWEST_PRECEDENCE);

        return registrationBean;
    }

    @Bean
    @Qualifier("mirror")
    @ConditionalOnProperty(value = "ovsx.data.mirror.enabled", havingValue = "true")
    public IExtensionRegistry mirror(
            RestTemplate restTemplate,
            UrlConfigService urlConfigService,
            @Value("${ovsx.data.mirror.requests-per-second:-1}") double requestsPerSecond
    ) {
        IExtensionRegistry registry = new UpstreamRegistryService(restTemplate, urlConfigService);
        if(requestsPerSecond != -1) {
            registry = new RateLimitedRegistryService(registry, requestsPerSecond);
        }

        return registry;
    }
}
