package com.studyshield.admin.ui;

import com.studyshield.admin.service.BackendDataService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * List, create and edit admin-app accounts (backend {@code /api/v1/admin-users}).
 * All accounts here are ADMIN type; passwords are write-only (set on create,
 * optionally reset on update, never returned by the backend).
 */
@Route(value = "users", layout = MainLayout.class)
@PageTitle("User management")
@RolesAllowed("ADMIN")
public class UserManagementView extends VerticalLayout {

    private static final String COLLECTION = "admin-users";

    private final BackendDataService backendDataService;

    private final Grid<Map<String, Object>> grid = new Grid<>();
    private final FormLayout editor = new FormLayout();

    private final TextField emailField = new TextField("Email");
    private final TextField nameField = new TextField("Name");
    private final TextField phoneField = new TextField("Phone");
    private final PasswordField passwordField = new PasswordField("Password");
    private final Checkbox activeField = new Checkbox("Active");

    private final Button saveButton = new Button("Save user");
    private final Button newButton = new Button("New user");
    private final Button deleteButton = new Button("Delete user");

    private Map<String, Object> selected;

    public UserManagementView(BackendDataService backendDataService) {
        this.backendDataService = backendDataService;

        setPadding(true);
        setSpacing(true);
        setWidthFull();
        add(new H2("User management"));

        grid.addColumn(item -> item.getOrDefault("id", "-")).setHeader("ID").setWidth("70px");
        grid.addColumn(item -> item.getOrDefault("email", "-")).setHeader("Email");
        grid.addColumn(item -> item.getOrDefault("name", "-")).setHeader("Name");
        grid.addColumn(item -> item.getOrDefault("phone", "-")).setHeader("Phone");
        grid.addColumn(item -> item.getOrDefault("active", "-")).setHeader("Active");
        grid.setSelectionMode(Grid.SelectionMode.SINGLE);
        grid.asSingleSelect().addValueChangeListener(event -> loadIntoEditor(event.getValue()));
        grid.setHeight("350px");
        grid.setItems(loadUsers());

        passwordField.setPlaceholder("Only needed when creating or resetting");
        activeField.setValue(true);

        editor.add(emailField, nameField, phoneField, passwordField, activeField);
        editor.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 2));

        newButton.addClickListener(e -> resetEditor());
        saveButton.addClickListener(e -> saveUser());
        deleteButton.addClickListener(e -> deleteUser());

        add(grid, editor, new HorizontalLayout(newButton, saveButton, deleteButton));
    }

    private List<Map<String, Object>> loadUsers() {
        return backendDataService.list(COLLECTION);
    }

    private void loadIntoEditor(Map<String, Object> user) {
        if (user == null || user.isEmpty()) {
            return;
        }
        selected = user;
        emailField.setValue(String.valueOf(user.getOrDefault("email", "")));
        nameField.setValue(String.valueOf(user.getOrDefault("name", "")));
        phoneField.setValue(String.valueOf(user.getOrDefault("phone", "")));
        passwordField.clear();
        activeField.setValue(Boolean.parseBoolean(String.valueOf(user.getOrDefault("active", true))));
    }

    private void resetEditor() {
        selected = null;
        emailField.clear();
        nameField.clear();
        phoneField.clear();
        passwordField.clear();
        activeField.setValue(true);
    }

    private void saveUser() {
        String email = emailField.getValue().trim();
        if (email.isBlank()) {
            Notification.show("Email is required");
            return;
        }
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("email", email);
        payload.put("name", nameField.getValue());
        payload.put("phone", phoneField.getValue());
        payload.put("active", activeField.getValue());

        try {
            if (selected != null && selected.get("id") != null) {
                Long id = Long.valueOf(String.valueOf(selected.get("id")));
                if (passwordField.getValue() != null && !passwordField.getValue().isBlank()) {
                    payload.put("password", passwordField.getValue());
                }
                backendDataService.update(COLLECTION, id, payload);
                Notification.show("User updated");
            } else {
                if (passwordField.getValue() == null || passwordField.getValue().isBlank()) {
                    Notification.show("Password is required when creating a user");
                    return;
                }
                payload.put("password", passwordField.getValue());
                backendDataService.create(COLLECTION, payload);
                Notification.show("User created");
            }
            grid.setItems(loadUsers());
            resetEditor();
        } catch (Exception ex) {
            Notification.show("Save failed: " + ex.getMessage());
        }
    }

    private void deleteUser() {
        if (selected == null || selected.get("id") == null) {
            Notification.show("Select a user to delete first");
            return;
        }
        Long id = Long.valueOf(String.valueOf(selected.get("id")));
        try {
            backendDataService.delete(COLLECTION, id);
            Notification.show("User deleted");
            grid.setItems(loadUsers());
            resetEditor();
        } catch (Exception ex) {
            Notification.show("Delete failed: " + ex.getMessage());
        }
    }
}
