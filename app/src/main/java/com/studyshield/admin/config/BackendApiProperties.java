package com.studyshield.admin.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ConfigurationProperties(prefix = "studyshield.backend")
public class BackendApiProperties {
    public static final String ENV_DEV = "dev";
    public static final String ENV_PROD = "prod";

    private String baseUrl = "http://localhost:8080";
    private String prodBaseUrl = "http://localhost:8082";
    private String username = "";
    private String password = "";
    private int connectTimeoutMs = 5000;
    private int readTimeoutMs = 10000;

    public boolean isAuthConfigured() {
        return username != null && !username.isBlank()
                && password != null && !password.isBlank();
    }

    public List<String> environments() {
        return List.of(ENV_DEV, ENV_PROD);
    }

    public String baseUrlFor(String environment) {
        if (ENV_DEV.equals(environment)) {
            return baseUrl;
        }
        if (ENV_PROD.equals(environment)) {
            return prodBaseUrl;
        }
        throw new IllegalArgumentException("Unknown environment: " + environment);
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getProdBaseUrl() {
        return prodBaseUrl;
    }

    public void setProdBaseUrl(String prodBaseUrl) {
        this.prodBaseUrl = prodBaseUrl;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public int getConnectTimeoutMs() {
        return connectTimeoutMs;
    }

    public void setConnectTimeoutMs(int connectTimeoutMs) {
        this.connectTimeoutMs = connectTimeoutMs;
    }

    public int getReadTimeoutMs() {
        return readTimeoutMs;
    }

    public void setReadTimeoutMs(int readTimeoutMs) {
        this.readTimeoutMs = readTimeoutMs;
    }
}
