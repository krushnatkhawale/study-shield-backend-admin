package com.studyshield.admin.ui;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.studyshield.admin.config.BackendApiProperties;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.Map;

@Route("forgot-password")
@PageTitle("Reset Password — StudyShield Admin")
@AnonymousAllowed
public class ForgotPasswordView extends VerticalLayout {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public ForgotPasswordView(BackendApiProperties properties, ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.restClient = RestClient.builder()
                .baseUrl(properties.getBaseUrl())
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .build();

        setSizeFull();
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.CENTER);

        H2 title = new H2("Reset Admin Password");
        title.getStyle().set("margin-bottom", "0.5rem");

        EmailField emailField = new EmailField("Email");
        emailField.setWidth("300px");
        emailField.setPlaceholder("admin@studyshield.local");
        emailField.setValue("admin@studyshield.local");

        PasswordField passwordField = new PasswordField("New Password");
        passwordField.setWidth("300px");
        passwordField.setHelperText("Minimum 6 characters");

        Button resetButton = new Button("Reset Password");
        resetButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        resetButton.setWidth("300px");
        resetButton.addClickListener(e -> {
            String email = emailField.getValue();
            String password = passwordField.getValue();

            if (email == null || email.isBlank() || password == null || password.isBlank()) {
                Notification.show("Please enter both email and new password",
                        3000, Notification.Position.MIDDLE);
                return;
            }
            if (password.length() < 6) {
                Notification.show("Password must be at least 6 characters",
                        3000, Notification.Position.MIDDLE);
                return;
            }

            resetButton.setEnabled(false);
            try {
                String json = objectMapper.writeValueAsString(Map.of(
                        "email", email, "newPassword", password));
                restClient.post()
                        .uri("/api/auth/admin-reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(json)
                        .retrieve()
                        .body(String.class);

                Notification success = new Notification(
                        "Password reset successful. You can now sign in.", 5000,
                        Notification.Position.MIDDLE);
                success.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                success.open();
                passwordField.clear();
            } catch (RestClientResponseException ex) {
                String body = ex.getResponseBodyAsString();
                String message = "Password reset failed";
                try {
                    JsonNode node = objectMapper.readTree(body);
                    message = node.path("message").asText(message);
                } catch (Exception ignored) {
                }
                Notification error = new Notification(message, 5000,
                        Notification.Position.MIDDLE);
                error.addThemeVariants(NotificationVariant.LUMO_ERROR);
                error.open();
            } catch (Exception ex) {
                Notification error = new Notification(
                        "Could not reach the backend server", 5000,
                        Notification.Position.MIDDLE);
                error.addThemeVariants(NotificationVariant.LUMO_ERROR);
                error.open();
            } finally {
                resetButton.setEnabled(true);
            }
        });

        Button backButton = new Button("Back to Login", e -> getUI().ifPresent(
                ui -> ui.navigate("login")));
        backButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        FormLayout form = new FormLayout(emailField, passwordField, resetButton);
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));
        form.setMaxWidth("400px");

        add(title, form, backButton);
    }
}
