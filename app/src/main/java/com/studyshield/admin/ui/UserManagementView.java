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
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * List, create and edit both user audiences of StudyShield:
 * <ul>
 *   <li>Client users (the mobile app): role PARENT, type MOBILE ({@code /api/v1/users}).</li>
 *   <li>Admin users (the admin app): role ADMIN, type ADMIN ({@code /api/v1/admin-users}).</li>
 * </ul>
 * Passwords are write-only (set on create, optionally reset on update, never returned).
 */
@Route(value = "users", layout = MainLayout.class)
@PageTitle("User management")
@RolesAllowed("ADMIN")
public class UserManagementView extends VerticalLayout {

    private static final String CLIENT_COLLECTION = "users";
    private static final String ADMIN_COLLECTION = "admin-users";

    private final BackendDataService backendDataService;

    private final Tab adminTab = new Tab("Admin users");
    private final Tab clientTab = new Tab("Client users");
    private final Tabs tabs = new Tabs(adminTab, clientTab);

    private final Grid<Map<String, Object>> adminGrid = new Grid<>();
    private final Grid<Map<String, Object>> clientGrid = new Grid<>();
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
    private boolean adminMode = true;

    public UserManagementView(BackendDataService backendDataService) {
        this.backendDataService = backendDataService;

        setPadding(true);
        setSpacing(true);
        setWidthFull();
        add(new H2("User management"));
        add(tabs);

        tabs.setSelectedTab(adminTab);
        configureAdminGrid();
        configureClientGrid();

        passwordField.setPlaceholder("Only needed when creating or resetting");
        activeField.setValue(true);

        editor.add(emailField, nameField, phoneField, passwordField, activeField);
        editor.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 2));

        newButton.addClickListener(e -> resetEditor());
        saveButton.addClickListener(e -> saveUser());
        deleteButton.addClickListener(e -> deleteUser());

        add(adminGrid, clientGrid, editor, new HorizontalLayout(newButton, saveButton, deleteButton));

        reload();
        tabs.addSelectedChangeListener(e -> switchTab());
    }

    private void configureAdminGrid() {
        adminGrid.addColumn(item -> item.getOrDefault("id", "-")).setHeader("ID").setWidth("70px");
        adminGrid.addColumn(item -> item.getOrDefault("email", "-")).setHeader("Email");
        adminGrid.addColumn(item -> item.getOrDefault("name", "-")).setHeader("Name");
        adminGrid.addColumn(item -> item.getOrDefault("phone", "-")).setHeader("Phone");
        adminGrid.addColumn(item -> item.getOrDefault("active", "-")).setHeader("Active");
        adminGrid.setSelectionMode(Grid.SelectionMode.SINGLE);
        adminGrid.asSingleSelect().addValueChangeListener(event -> loadIntoEditor(event.getValue()));
        adminGrid.setHeight("350px");
        adminGrid.setVisible(true);
    }

    private void configureClientGrid() {
        clientGrid.addColumn(item -> item.getOrDefault("id", "-")).setHeader("ID").setWidth("70px");
        clientGrid.addColumn(item -> item.getOrDefault("email", "-")).setHeader("Email");
        clientGrid.addColumn(item -> item.getOrDefault("name", "-")).setHeader("Name");
        clientGrid.addColumn(item -> item.getOrDefault("phone", "-")).setHeader("Phone");
        clientGrid.addColumn(item -> item.getOrDefault("active", "-")).setHeader("Active");
        clientGrid.addColumn(item -> item.getOrDefault("createdAt", "-")).setHeader("Created");
        clientGrid.setSelectionMode(Grid.SelectionMode.SINGLE);
        clientGrid.asSingleSelect().addValueChangeListener(event -> loadIntoEditor(event.getValue()));
        clientGrid.setHeight("350px");
        clientGrid.setVisible(false);
    }

    private String activeCollection() {
        return adminMode ? ADMIN_COLLECTION : CLIENT_COLLECTION;
    }

    private void switchTab() {
        adminMode = tabs.getSelectedTab() == adminTab;
        adminGrid.setVisible(adminMode);
        clientGrid.setVisible(!adminMode);
        resetEditor();
        reload();
    }

    private void reload() {
        if (adminMode) {
            adminGrid.setItems(loadAdminUsers());
            clientGrid.setItems(List.of());
        } else {
            adminGrid.setItems(List.of());
            clientGrid.setItems(loadClientUsers());
        }
    }

    private List<Map<String, Object>> loadAdminUsers() {
        return backendDataService.list(ADMIN_COLLECTION);
    }

    private List<Map<String, Object>> loadClientUsers() {
        List<Map<String, Object>> all = backendDataService.list(CLIENT_COLLECTION);
        List<Map<String, Object>> clients = new ArrayList<>();
        for (Map<String, Object> user : all) {
            String userType = String.valueOf(user.getOrDefault("userType", ""));
            String role = String.valueOf(user.getOrDefault("role", ""));
            if ("MOBILE".equalsIgnoreCase(userType) || "PARENT".equalsIgnoreCase(role)) {
                clients.add(user);
            }
        }
        return clients;
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
        String collection = activeCollection();
        payload.put("email", email);
        payload.put("name", nameField.getValue());
        payload.put("phone", phoneField.getValue());
        payload.put("active", activeField.getValue());
        if (!adminMode) {
            payload.put("role", "PARENT");
            payload.put("userType", "MOBILE");
        }

        try {
            if (selected != null && selected.get("id") != null) {
                Long id = Long.valueOf(String.valueOf(selected.get("id")));
                if (passwordField.getValue() != null && !passwordField.getValue().isBlank()) {
                    payload.put("password", passwordField.getValue());
                }
                backendDataService.update(collection, id, payload);
                Notification.show("User updated");
            } else {
                if (passwordField.getValue() == null || passwordField.getValue().isBlank()) {
                    Notification.show("Password is required when creating a user");
                    return;
                }
                payload.put("password", passwordField.getValue());
                backendDataService.create(collection, payload);
                Notification.show("User created");
            }
            resetEditor();
            reload();
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
            backendDataService.delete(activeCollection(), id);
            Notification.show("User deleted");
            resetEditor();
            reload();
        } catch (Exception ex) {
            Notification.show("Delete failed: " + ex.getMessage());
        }
    }
}