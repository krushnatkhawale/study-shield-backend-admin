package com.studyshield.admin.ui;

import com.studyshield.admin.service.BackendDataService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.notification.Notification;
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

    private boolean adminMode = true;

    public UserManagementView(BackendDataService backendDataService) {
        this.backendDataService = backendDataService;
        setSizeFull();
        setPadding(false);

        Button create = AdminUi.primary("New user");
        create.addClickListener(e -> openDialog(null));
        VerticalLayout page = AdminUi.page("User management",
                "Admin users sign in here; client users sign in on the mobile app. Double-click a row to edit.",
                create);
        page.add(tabs);

        tabs.setSelectedTab(adminTab);
        configureAdminGrid();
        configureClientGrid();
        tabs.addSelectedChangeListener(e -> switchTab());

        adminGrid.setSizeFull();
        clientGrid.setSizeFull();
        adminGrid.addItemDoubleClickListener(e -> openDialog(e.getItem()));
        clientGrid.addItemDoubleClickListener(e -> openDialog(e.getItem()));

        page.add(AdminUi.card(activeGrid()));
        page.setFlexGrow(1, page.getComponentAt(2));
        add(page);
        setFlexGrow(1, page);
        reload();
    }

    private Grid<Map<String, Object>> activeGrid() {
        return adminMode ? adminGrid : clientGrid;
    }

    private void configureAdminGrid() {
        adminGrid.addColumn(item -> AdminUi.str(item, "id")).setHeader("ID").setAutoWidth(true);
        adminGrid.addColumn(item -> AdminUi.str(item, "email")).setHeader("Email").setFlexGrow(1);
        adminGrid.addColumn(item -> AdminUi.str(item, "name")).setHeader("Name");
        adminGrid.addColumn(item -> AdminUi.str(item, "phone")).setHeader("Phone");
        adminGrid.addColumn(item -> AdminUi.str(item, "active")).setHeader("Active");
    }

    private void configureClientGrid() {
        clientGrid.addColumn(item -> AdminUi.str(item, "id")).setHeader("ID").setAutoWidth(true);
        clientGrid.addColumn(item -> AdminUi.str(item, "email")).setHeader("Email").setFlexGrow(1);
        clientGrid.addColumn(item -> AdminUi.str(item, "name")).setHeader("Name");
        clientGrid.addColumn(item -> AdminUi.str(item, "phone")).setHeader("Phone");
        clientGrid.addColumn(item -> AdminUi.str(item, "active")).setHeader("Active");
        clientGrid.addColumn(item -> AdminUi.str(item, "createdAt")).setHeader("Created");
        clientGrid.setVisible(false);
    }

    private String activeCollection() {
        return adminMode ? ADMIN_COLLECTION : CLIENT_COLLECTION;
    }

    private void switchTab() {
        adminMode = tabs.getSelectedTab() == adminTab;
        adminGrid.setVisible(adminMode);
        clientGrid.setVisible(!adminMode);
        reload();
    }

    private void reload() {
        if (adminMode) {
            adminGrid.setItems(loadAdminUsers());
        } else {
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

    private void openDialog(Map<String, Object> row) {
        Long id = AdminUi.id(row);
        boolean isNew = id == null;

        TextField emailField = new TextField("Email");
        emailField.setRequired(true);
        emailField.setWidthFull();
        TextField nameField = new TextField("Name");
        nameField.setWidthFull();
        TextField phoneField = new TextField("Phone");
        phoneField.setWidthFull();
        PasswordField passwordField = new PasswordField("Password");
        passwordField.setWidthFull();
        passwordField.setPlaceholder("Only needed when creating or resetting");
        Checkbox activeField = new Checkbox("Active");
        if (row != null) {
            emailField.setValue(AdminUi.str(row, "email"));
            nameField.setValue(AdminUi.str(row, "name"));
            phoneField.setValue(AdminUi.str(row, "phone"));
            activeField.setValue(Boolean.parseBoolean(AdminUi.str(row, "active")));
        } else {
            activeField.setValue(true);
        }

        FormLayout form = AdminUi.entityForm(emailField, nameField, phoneField, passwordField, activeField);

        Dialog dialog = AdminUi.entityDialog(isNew ? "New user" : "Edit user", form);

        Button save = AdminUi.primary("Save");
        save.addClickListener(e -> {
            String email = emailField.getValue().trim();
            if (email.isBlank()) {
                Notification.show("Email is required");
                emailField.focus();
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
                if (!isNew) {
                    if (passwordField.getValue() != null && !passwordField.getValue().isBlank()) {
                        payload.put("password", passwordField.getValue());
                    }
                    backendDataService.update(collection, id, payload);
                    Notification.show("User updated");
                } else {
                    if (passwordField.getValue() == null || passwordField.getValue().isBlank()) {
                        Notification.show("Password is required when creating a user");
                        passwordField.focus();
                        return;
                    }
                    payload.put("password", passwordField.getValue());
                    backendDataService.create(collection, payload);
                    Notification.show("User created");
                }
                dialog.close();
                reload();
            } catch (Exception ex) {
                Notification.show("Save failed: " + ex.getMessage());
            }
        });
        Button delete = AdminUi.danger("Delete");
        delete.setVisible(!isNew);
        delete.addClickListener(e -> AdminUi.confirmDelete("Delete user?", "This user will lose access immediately.", () -> {
            backendDataService.delete(activeCollection(), id);
            Notification.show("User deleted");
            dialog.close();
            reload();
        }));
        AdminUi.dialogFooter(dialog, delete, save);

        dialog.open();
        emailField.focus();
    }
}
