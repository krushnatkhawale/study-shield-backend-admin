package com.studyshield.admin.repository;

import com.studyshield.admin.domain.ApplicationSettings;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApplicationSettingsRepository extends JpaRepository<ApplicationSettings, Long> {
}
