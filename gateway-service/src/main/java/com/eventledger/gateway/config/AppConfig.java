package com.eventledger.gateway.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;

@Configuration
@EnableConfigurationProperties(GatewayProperties.class)
public class AppConfig {

    @Bean
    public RestClient.Builder restClientBuilder() {
        // Pin to HTTP/1.1: the JDK client otherwise attempts HTTP/2, which some servers
        // (e.g. WireMock/Jetty in tests) reject with RST_STREAM on cleartext upgrade.
        HttpClient httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .build();
        return RestClient.builder()
                .requestFactory(new JdkClientHttpRequestFactory(httpClient));
    }
}
