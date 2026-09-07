# StudyShield Backend Admin

A Java-based admin console for StudyShield's backend configuration. It is built with Spring Boot 3 and Vaadin so operators can manage application settings through a secure UI without writing configuration files by hand.

## Why Vaadin?

Vaadin is a strong fit for an internal admin panel because it keeps the UI in Java, gives a rich component library, and matches the admin workflow of editing structured configuration values. This repo is designed to expose the config model used by the StudyShield backend in a secure, maintainable way.

## Features

- secure admin login (`admin` / `admin123` by default)
- dashboard overview of backend settings
- editable configuration form for app, security, and catalog settings
- persistent config storage in H2 for local development
- basic Spring Boot tests covering context startup, security, and config storage

## Run locally

```bash
cd /Users/hulk/.buzz/REPOS/study-shield-backend-admin
./gradlew :app:bootRun
```

Then open:

- http://localhost:8081/login
- default credentials: `admin` / `admin123`

## Default admin account

The application uses an in-memory Spring Security user for local development. You can change it in `SecurityConfiguration.java` when wiring to a real identity provider.

## Notes

The config is intentionally backed by a local H2 database so the admin can update values in the browser without modifying YAML files directly. The values are based on the StudyShield backend architecture described in the project docs, including JWT expiration and catalog seeding toggles.
