package com.studyshield.admin.config;

import com.studyshield.admin.service.ApplicationSettingsService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner initializeSettings(ApplicationSettingsService service) {
        return args -> service.getOrCreateSettings();
    }
}
