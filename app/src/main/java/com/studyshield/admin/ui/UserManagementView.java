package com.studyshield.admin.ui;

import com.studyshield.admin.service.BackendDataService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * List, create and edit users. Passwords are intentionally never exposed or editable here —
 * a user's password is set only at signup / via the backend, not through this view.
 */
@Route(value = "users", layout = MainLayout.class)
@PageTitle("User management")
@RolesAllowed("ADMIN")
public class UserManagementView extends VerticalLayout {

    private static final List<String> USER_ROLES = List.of("PARENT", "ADMIN");

    private final BackendDataService backendDataService;

    private final Grid<Map<String, Object>> grid = new Grid<>();
    private final FormLayout editor = new FormLayout();

    private final TextField emailField = new TextField("Email");
    private final TextField nameField = new TextField("Name");
    private final TextField phoneField = new TextField("Phone");
    private final ComboBox<String> roleField = new ComboBox<>("Role");
    private final Checkbox activeField = new Checkbox("Active");

    private final Button saveButton = new Button("Save user");
    private final Button newButton = new Button("New user");
    private final Button deleteButton = new Button("Delete user");

    private Map<String, Object> selected;

    public UserManagementView(BackendDataService backendDataService) {
        this.backendDataService = backendDataService;

        setPadding(true);
        setSpacing(true);
        setSizeFull();
        add(new H2("User management"));

        grid.addColumn(item -> item.getOrDefault("id", "-")).setHeader("ID").setWidth("70px");
        grid.addColumn(item -> item.getOrDefault("email", "-")).setHeader("Email");
        grid.addColumn(item -> item.getOrDefault("name", "-")).setHeader("Name");
        grid.addColumn(item -> item.getOrDefault("phone", "-")).setHeader("Phone");
        grid.addColumn(item -> item.getOrDefault("role", "-")).setHeader("Role");
        grid.addColumn(item -> item.getOrDefault("active", "-")).setHeader("Active");
        grid.setSelectionMode(Grid.SelectionMode.SINGLE);
        grid.asSingleSelect().addValueChangeListener(event -> loadIntoEditor(event.getValue()));
        grid.setHeight("350px");
        grid.setItems(loadUsers());

        roleField.setItems(USER_ROLES);
        roleField.setValue("PARENT");
        activeField.setValue(true);

        editor.add(emailField, nameField, phoneField, roleField, activeField);
        editor.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 2));

        newButton.addClickListener(e -> resetEditor());
        saveButton.addClickListener(e -> saveUser());
        deleteButton.addClickListener(e -> deleteUser());

        add(grid, editor, new HorizontalLayout(newButton, saveButton, deleteButton));
    }

    private List<Map<String, Object>> loadUsers() {
        return backendDataService.list("users");
    }

    private void loadIntoEditor(Map<String, Object> user) {
        if (user == null || user.isEmpty()) {
            return;
        }
        selected = user;
        emailField.setValue(String.valueOf(user.getOrDefault("email", "")));
        nameField.setValue(String.valueOf(user.getOrDefault("name", "")));
        phoneField.setValue(String.valueOf(user.getOrDefault("phone", "")));
        roleField.setValue(String.valueOf(user.getOrDefault("role", "PARENT")));
        activeField.setValue(Boolean.parseBoolean(String.valueOf(user.getOrDefault("active", true))));
    }

    private void resetEditor() {
        selected = null;
        emailField.clear();
        nameField.clear();
        phoneField.clear();
        roleField.setValue("PARENT");
        activeField.setValue(true);
    }

    private void saveUser() {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("email", emailField.getValue());
        payload.put("name", nameField.getValue());
        payload.put("phone", phoneField.getValue());
        payload.put("role", roleField.getValue());
        payload.put("active", activeField.getValue());

        try {
            if (selected != null && selected.get("id") != null) {
                Long id = Long.valueOf(String.valueOf(selected.get("id")));
                backendDataService.update("users", id, payload);
                Notification.show("User updated");
            } else {
                backendDataService.create("users", payload);
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
            backendDataService.delete("users", id);
            Notification.show("User deleted");
            grid.setItems(loadUsers());
            resetEditor();
        } catch (Exception ex) {
            Notification.show("Delete failed: " + ex.getMessage());
        }
    }
}
