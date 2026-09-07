package com.studyshield.admin;

import com.studyshield.admin.domain.ApplicationSettings;
import com.studyshield.admin.repository.ApplicationSettingsRepository;
import com.studyshield.admin.service.ApplicationSettingsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class ApplicationSettingsServiceTests {

    @Autowired
    private ApplicationSettingsService settingsService;

    @Autowired
    private ApplicationSettingsRepository repository;

    @Test
    void createsDefaultSettingsWhenMissing() {
        repository.deleteAll();

        ApplicationSettings settings = settingsService.getOrCreateSettings();

        assertThat(settings.getAppName()).isEqualTo("StudyShield");
        assertThat(settings.getJwtExpirationMs()).isEqualTo(86_400_000);
        assertThat(settings.getCatalogSeedingEnabled()).isFalse();
    }

    @Test
    void updatesSettings() {
        ApplicationSettings settings = settingsService.getOrCreateSettings();
        settings.setAppName("StudyShield QA");
        settings.setEnvironment("PRODUCTION");
        settings.setUpdatedBy("ops-user");

        ApplicationSettings updated = settingsService.updateSettings(settings);

        assertThat(updated.getAppName()).isEqualTo("StudyShield QA");
        assertThat(updated.getEnvironment()).isEqualTo("PRODUCTION");
        assertThat(updated.getUpdatedBy()).isEqualTo("ops-user");
    }
}
