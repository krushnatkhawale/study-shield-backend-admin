package com.studyshield.admin.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class BootStartupLog implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(BootStartupLog.class);

    private final Environment environment;
    private final BackendApiProperties backendApiProperties;

    public BootStartupLog(Environment environment, BackendApiProperties backendApiProperties) {
        this.environment = environment;
        this.backendApiProperties = backendApiProperties;
    }

    @Override
    public void run(ApplicationArguments args) {
        log.info("Database URL: {}", environment.getProperty("spring.datasource.url"));
        log.info("Database username: {}", environment.getProperty("spring.datasource.username"));
        log.info("StudyShield backend base URL: {}", backendApiProperties.getBaseUrl());

        if (backendApiProperties.isAuthConfigured()) {
            log.info("StudyShield backend auth: configured (loginId={})", backendApiProperties.getUsername());
        } else {
            log.warn("StudyShield backend auth: NOT configured - data views stay empty until "
                    + "STUDYSHIELD_BACKEND_USERNAME / STUDYSHIELD_BACKEND_PASSWORD point at a backend account.");
        }
    }
}