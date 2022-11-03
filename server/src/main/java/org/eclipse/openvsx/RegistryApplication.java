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

import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
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
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.security.web.firewall.HttpStatusRequestRejectedHandler;
import org.springframework.security.web.firewall.RequestRejectedHandler;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.DefaultUriBuilderFactory;

import io.micrometer.core.aop.TimedAspect;
import io.micrometer.core.instrument.MeterRegistry;

@SpringBootApplication
@EnableScheduling
@EnableRetry
@EnableAsync
@EnableCaching(proxyTargetClass = true)
public class RegistryApplication {

    public static void main(String[] args) {
        SpringApplication.run(RegistryApplication.class, args);
    }

    @Bean
    public TimedAspect timedAspect(MeterRegistry registry) {
        return new TimedAspect(registry);
    }

    /**
     * Use to serve requests to ensure that response is given within 30 seconds.
     * VS Code does not wait more than it and will timeout a request.
     */
	@Bean
    public HttpConnPollConfiguration foregroundHttpConnPool(
        @Value("${ovsx.foregroundHttpConnPool.maxTotal:20}") Integer maxTotal,
        @Value("${ovsx.foregroundHttpConnPool.defaultMaxPerRoute:20}") Integer defaultMaxPerRoute,
        @Value("${ovsx.foregroundHttpConnPool.connectionRequestTimeout:10000}") Integer connectionRequestTimeout,
        @Value("${ovsx.foregroundHttpConnPool.connectTimeout:10000}") Integer connectTimeout,
        @Value("${ovsx.foregroundHttpConnPool.socketTimeout:10000}") Integer socketTimeout
    ) {
        return createHttpConnnPoolConfiguration(maxTotal, defaultMaxPerRoute, connectionRequestTimeout, connectTimeout, socketTimeout);
    }

    /**
     * Use to download files in background processing for requests not requiring redirects.
     * Never use to serve requests. Overall response time should be withitn 30secs.
     */
	@Bean
    public HttpConnPollConfiguration backgroundHttpConnPool(
        @Value("${ovsx.backgroundHttpConnPool.maxTotal:20}") Integer maxTotal,
        @Value("${ovsx.backgroundHttpConnPool.defaultMaxPerRoute:20}") Integer defaultMaxPerRoute,
        @Value("${ovsx.backgroundHttpConnPool.connectionRequestTimeout:30000}") Integer connectionRequestTimeout,
        @Value("${ovsx.backgroundHttpConnPool.connectTimeout:30000}") Integer connectTimeout,
        @Value("${ovsx.backgroundHttpConnPool.socketTimeout:60000}") Integer socketTimeout
    ) {
        return createHttpConnnPoolConfiguration(maxTotal, defaultMaxPerRoute, connectionRequestTimeout, connectTimeout, socketTimeout);
    }

    private HttpConnPollConfiguration createHttpConnnPoolConfiguration(Integer maxTotal, Integer defaultMaxPerRoute,
            Integer connectionRequestTimeout, Integer connectTimeout, Integer socketTimeout) {
        PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
        connectionManager.setMaxTotal(maxTotal);
        connectionManager.setDefaultMaxPerRoute(defaultMaxPerRoute);
        return new HttpConnPollConfiguration(
            connectionManager,
            connectionRequestTimeout,
            connectTimeout,
            socketTimeout
        );
    }

    private HttpClientBuilder createHttpClientBuilder(HttpConnPollConfiguration httpConnPollConfiguration) {
        var requestConfig = RequestConfig.custom()
            .setConnectionRequestTimeout(httpConnPollConfiguration.getConnectionRequestTimeout())
            .setConnectTimeout(httpConnPollConfiguration.getConnectTimeout())
            .setSocketTimeout(httpConnPollConfiguration.getSocketTimeout())
            .build();
        return HttpClientBuilder
            .create()
            .setConnectionManager(httpConnPollConfiguration.getConnectionManager())
            .setDefaultRequestConfig(requestConfig);
    }

    @Bean
	public RestTemplate restTemplate(RestTemplateBuilder builder, HttpConnPollConfiguration foregroundHttpConnPool) {
        var httpClient = createHttpClientBuilder(foregroundHttpConnPool).build();
        return builder
            .requestFactory(() -> {
                HttpComponentsClientHttpRequestFactory f = new HttpComponentsClientHttpRequestFactory();
                f.setHttpClient(httpClient);
                return f;
            })
            .messageConverters(
                new ByteArrayHttpMessageConverter(),
                new StringHttpMessageConverter(),
                new MappingJackson2HttpMessageConverter())
            .build();
    }

    @Bean
    public RestTemplate nonRedirectingRestTemplate(RestTemplateBuilder builder, HttpConnPollConfiguration foregroundHttpConnPool) {
        var httpClient = createHttpClientBuilder(foregroundHttpConnPool).disableRedirectHandling().build();
        return builder
            .requestFactory(() -> {
                HttpComponentsClientHttpRequestFactory f = new HttpComponentsClientHttpRequestFactory();
                f.setHttpClient(httpClient);
                return f;
            })
            .build();
    }
    
    @Bean
	public RestTemplate backgroundRestTemplate(RestTemplateBuilder builder, HttpConnPollConfiguration backgroundHttpConnPool) {
        var httpClient = createHttpClientBuilder(backgroundHttpConnPool).build();
        DefaultUriBuilderFactory defaultUriBuilderFactory = new DefaultUriBuilderFactory();
        defaultUriBuilderFactory.setEncodingMode(DefaultUriBuilderFactory.EncodingMode.NONE);
        return builder
            .uriTemplateHandler(defaultUriBuilderFactory)
            .messageConverters(
                new ByteArrayHttpMessageConverter(),
                new StringHttpMessageConverter(),
                new MappingJackson2HttpMessageConverter())
            .requestFactory(() -> {
                HttpComponentsClientHttpRequestFactory f = new HttpComponentsClientHttpRequestFactory();
                f.setHttpClient(httpClient);
                return f;
            })
            .build();
    }
    
    @Bean
	public RestTemplate backgroundNonRedirectingRestTemplate(RestTemplateBuilder builder, HttpConnPollConfiguration backgroundHttpConnPool) {
        var httpClient = createHttpClientBuilder(backgroundHttpConnPool).disableRedirectHandling().build();
        DefaultUriBuilderFactory defaultUriBuilderFactory = new DefaultUriBuilderFactory();
        defaultUriBuilderFactory.setEncodingMode(DefaultUriBuilderFactory.EncodingMode.NONE);
        return builder
            .uriTemplateHandler(defaultUriBuilderFactory)
            .requestFactory(() -> {
                HttpComponentsClientHttpRequestFactory f = new HttpComponentsClientHttpRequestFactory();
                f.setHttpClient(httpClient);
                return f;
            })
            .build();
    }
    
    @Bean
    public TaskScheduler taskScheduler() {
        return new ThreadPoolTaskScheduler();
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
    public RequestRejectedHandler requestRejectedHandler() {
        return new HttpStatusRequestRejectedHandler();
    }
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
            UpstreamRegistryService upstream,
            @Value("${ovsx.data.mirror.requests-per-second:-1}") double requestsPerSecond
    ) {
        if(requestsPerSecond != -1) {
            return new RateLimitedRegistryService(upstream, requestsPerSecond);
        }
        return upstream;
    }
}
