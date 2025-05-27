package com.ns.secrets.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.Ssl;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.File;

@Slf4j
@Configuration
public class TlsConfig {

    @Value("${SSL_ENABLED:false}")
    private boolean sslEnabled;

    @Value("${SSL_KEY_STORE:}")
    private String keyStore;

    @Value("${SSL_KEY_STORE_PASSWORD:}")
    private String keyStorePassword;

    @Bean
    public WebServerFactoryCustomizer<TomcatServletWebServerFactory> sslCustomizer() {
        return factory -> {
            if (sslEnabled) {
                if (keyStore.isEmpty() || keyStorePassword.isEmpty()) {
                    log.warn("Invalid KeyStore or password.");
                    factory.setSsl(null);
                    return;
                }

                File keyStoreFile = new File(keyStore);
                if (!keyStoreFile.exists()) {
                    log.warn("KeyStore does not exist: {}.", keyStore);
                    factory.setSsl(null);
                    return;
                }

                Ssl ssl = new Ssl();
                ssl.setEnabled(true);
                ssl.setKeyStore(keyStore);
                ssl.setKeyStorePassword(keyStorePassword);
                ssl.setKeyStoreType("PKCS12");

                factory.setSsl(ssl);
                log.info("Enabled SSL: {}", keyStore);
            } else {
                log.info("Disabled SSL.");
            }
        };
    }
}
