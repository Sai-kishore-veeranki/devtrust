package com.vsk.devtrust.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * Four different classes used to each create their own `new RestTemplate()` —
 * three of them (GitHub webhook file-fetch, GitHub PR comment, Prometheus
 * scraper) with no timeout at all. A slow or hanging response from GitHub or
 * Prometheus would then block that thread indefinitely — a webhook request
 * thread in two cases, a @Scheduled thread in the third. One shared bean with
 * explicit timeouts closes that gap everywhere at once.
 */
@Configuration
public class HttpClientConfig {

    @Bean
    public RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5_000);
        factory.setReadTimeout(10_000);
        return new RestTemplate(factory);
    }
}
