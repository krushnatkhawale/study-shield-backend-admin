package com.studyshield.admin.domain;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "application_settings")
public class ApplicationSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String appName = "StudyShield";

    @Column(nullable = false)
    private String environment = "STAGING";

    @Column(nullable = false)
    private String apiBaseUrl = "https://api.studyshield.local";

    @Column(nullable = false)
    private Integer jwtExpirationMs = 86_400_000;

    @Column(nullable = false)
    private Boolean catalogSeedingEnabled = false;

    @Column(nullable = false)
    private Boolean guestAccessEnabled = false;

    @Column(nullable = false)
    private Integer maxStudentsPerParent = 5;

    @Column(nullable = false)
    private String supportEmail = "support@studyshield.local";

    @Column(nullable = false)
    private String defaultLanguage = "en";

    @Column(nullable = false)
    private String maintenanceMessage = "All systems operational";

    @Column(nullable = false)
    private String updatedBy = "admin";

    @Column(nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    public ApplicationSettings() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public String getEnvironment() {
        return environment;
    }

    public void setEnvironment(String environment) {
        this.environment = environment;
    }

    public String getApiBaseUrl() {
        return apiBaseUrl;
    }

    public void setApiBaseUrl(String apiBaseUrl) {
        this.apiBaseUrl = apiBaseUrl;
    }

    public Integer getJwtExpirationMs() {
        return jwtExpirationMs;
    }

    public void setJwtExpirationMs(Integer jwtExpirationMs) {
        this.jwtExpirationMs = jwtExpirationMs;
    }

    public Boolean getCatalogSeedingEnabled() {
        return catalogSeedingEnabled;
    }

    public void setCatalogSeedingEnabled(Boolean catalogSeedingEnabled) {
        this.catalogSeedingEnabled = catalogSeedingEnabled;
    }

    public Boolean getGuestAccessEnabled() {
        return guestAccessEnabled;
    }

    public void setGuestAccessEnabled(Boolean guestAccessEnabled) {
        this.guestAccessEnabled = guestAccessEnabled;
    }

    public Integer getMaxStudentsPerParent() {
        return maxStudentsPerParent;
    }

    public void setMaxStudentsPerParent(Integer maxStudentsPerParent) {
        this.maxStudentsPerParent = maxStudentsPerParent;
    }

    public String getSupportEmail() {
        return supportEmail;
    }

    public void setSupportEmail(String supportEmail) {
        this.supportEmail = supportEmail;
    }

    public String getDefaultLanguage() {
        return defaultLanguage;
    }

    public void setDefaultLanguage(String defaultLanguage) {
        this.defaultLanguage = defaultLanguage;
    }

    public String getMaintenanceMessage() {
        return maintenanceMessage;
    }

    public void setMaintenanceMessage(String maintenanceMessage) {
        this.maintenanceMessage = maintenanceMessage;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
