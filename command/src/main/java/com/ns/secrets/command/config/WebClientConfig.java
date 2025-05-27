package com.ns.secrets.command.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {
    @Value("${server.url:http://localhost:8080}")
    protected String serverUrl;

    @Bean
    public WebClient webClient() {
        return WebClient.builder()
                .baseUrl(serverUrl)
                .build();
    }
}

