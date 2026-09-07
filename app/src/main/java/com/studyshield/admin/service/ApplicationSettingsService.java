package com.studyshield.admin.service;

import com.studyshield.admin.domain.ApplicationSettings;
import com.studyshield.admin.repository.ApplicationSettingsRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ApplicationSettingsService {

    private final ApplicationSettingsRepository repository;

    public ApplicationSettingsService(ApplicationSettingsRepository repository) {
        this.repository = repository;
    }

    public ApplicationSettings getOrCreateSettings() {
        List<ApplicationSettings> settings = repository.findAll();
        if (settings.isEmpty()) {
            return repository.save(new ApplicationSettings());
        }
        return settings.getFirst();
    }

    public ApplicationSettings updateSettings(ApplicationSettings incoming) {
        ApplicationSettings current = getOrCreateSettings();

        current.setAppName(incoming.getAppName());
        current.setEnvironment(incoming.getEnvironment());
        current.setApiBaseUrl(incoming.getApiBaseUrl());
        current.setJwtExpirationMs(incoming.getJwtExpirationMs());
        current.setCatalogSeedingEnabled(incoming.getCatalogSeedingEnabled());
        current.setGuestAccessEnabled(incoming.getGuestAccessEnabled());
        current.setMaxStudentsPerParent(incoming.getMaxStudentsPerParent());
        current.setSupportEmail(incoming.getSupportEmail());
        current.setDefaultLanguage(incoming.getDefaultLanguage());
        current.setMaintenanceMessage(incoming.getMaintenanceMessage());
        current.setUpdatedBy(incoming.getUpdatedBy() == null ? "admin" : incoming.getUpdatedBy());
        current.setUpdatedAt(LocalDateTime.now());

        return repository.save(current);
    }
}
