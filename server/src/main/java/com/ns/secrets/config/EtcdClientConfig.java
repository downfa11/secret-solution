package com.ns.secrets.config;

import io.etcd.jetcd.Client;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EtcdClientConfig {

    @Value("${etcd.url}")
    private String etcdUrl;

    @Bean
    public Client etcdClient() {
        return Client.builder()
                .endpoints(etcdUrl)
                .build();
    }
}

