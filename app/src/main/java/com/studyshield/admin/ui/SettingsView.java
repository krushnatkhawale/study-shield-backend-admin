package com.studyshield.admin.ui;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.studyshield.admin.service.BackendDataService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Route(value = "settings", layout = MainLayout.class)
@PageTitle("Data management")
@RolesAllowed("ADMIN")
public class SettingsView extends VerticalLayout {

    private static final List<String> COLLECTIONS = List.of(
            "boards",
            "class-levels",
            "board-classes",
            "subjects",
            "offerings",
            "content-packs",
            "quizzes",
            "questions",
            "quiz-bundles",
            "users",
            "admin-users",
            "parents",
            "students",
            "children",
            "quiz-attempts",
            "quiz-results",
            "tv-users",
            "wifi-networks",
            "connected-tvs"
    );

    private final BackendDataService backendDataService;
    private final ObjectMapper objectMapper;
    private final Select<String> collectionSelect = new Select<>();
    private final Grid<Map<String, Object>> grid = new Grid<>();

    public SettingsView(BackendDataService backendDataService, ObjectMapper objectMapper) {
        this.backendDataService = backendDataService;
        this.objectMapper = objectMapper;
        setSizeFull();
        setPadding(false);

        Button create = AdminUi.primary("New record");
        create.addClickListener(e -> openDialog(null));
        VerticalLayout page = AdminUi.page("Backend table browser",
                "Low-level JSON view of every backend table. Double-click a row to edit.",
                create);

        collectionSelect.setItems(COLLECTIONS);
        collectionSelect.setValue("boards");
        collectionSelect.setLabel("Table");
        collectionSelect.setWidth("240px");
        collectionSelect.addValueChangeListener(event -> loadCollection(event.getValue()));

        Button refreshButton = new Button("Refresh", event -> loadCollection(collectionSelect.getValue()));

        grid.addColumn(item -> AdminUi.str(item, "id")).setHeader("ID").setAutoWidth(true);
        grid.addColumn(item -> summarize(item)).setHeader("Summary").setFlexGrow(1);
        grid.setSizeFull();
        grid.setMinHeight("320px");
        grid.addItemDoubleClickListener(event -> openDialog(event.getItem()));

        HorizontalLayout controls = new HorizontalLayout(collectionSelect, refreshButton);
        controls.setAlignItems(Alignment.END);
        com.vaadin.flow.component.Component card = AdminUi.card(grid);
        page.add(controls, card);
        page.setFlexGrow(1, card);
        add(page);
        setFlexGrow(1, page);
        loadCollection("boards");
    }

    private void openDialog(Map<String, Object> row) {
        Long id = AdminUi.id(row);
        boolean isNew = id == null;

        TextArea recordEditor = new TextArea("Record JSON");
        recordEditor.setWidthFull();
        recordEditor.setHeight("220px");
        try {
            Map<String, Object> initial = row != null ? row : new LinkedHashMap<>(Map.of());
            recordEditor.setValue(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(initial));
        } catch (Exception ex) {
            recordEditor.setValue(row != null ? row.toString() : "{}\n");
        }

        FormLayout form = AdminUi.entityForm(recordEditor);

        Dialog dialog = AdminUi.entityDialog(isNew ? "New record" : "Edit record", form);

        Button save = AdminUi.primary("Save");
        save.addClickListener(e -> {
            if (recordEditor.getValue().isBlank()) {
                Notification.show("Name is required");
                recordEditor.focus();
                return;
            }
            String collection = collectionSelect.getValue();
            try {
                Map<String, Object> payload = objectMapper.readValue(recordEditor.getValue(),
                        new TypeReference<Map<String, Object>>() {});
                if (!isNew) {
                    backendDataService.update(collection, id, payload);
                    Notification.show("Record updated");
                } else {
                    backendDataService.create(collection, payload);
                    Notification.show("Record created");
                }
                dialog.close();
                loadCollection(collection);
            } catch (Exception ex) {
                Notification.show("Invalid JSON or backend request failed");
            }
        });
        Button delete = AdminUi.danger("Delete");
        delete.setVisible(!isNew);
        delete.addClickListener(e -> AdminUi.confirmDelete("Delete record?", "This cannot be undone.", () -> {
            backendDataService.delete(collectionSelect.getValue(), id);
            Notification.show("Record deleted");
            dialog.close();
            loadCollection(collectionSelect.getValue());
        }));
        AdminUi.dialogFooter(dialog, delete, save);

        dialog.open();
        recordEditor.focus();
    }

    private void loadCollection(String collection) {
        List<Map<String, Object>> records = backendDataService.list(collection);
        grid.setItems(records);
    }

    private String summarize(Map<String, Object> row) {
        if (row == null || row.isEmpty()) {
            return "New record";
        }
        List<String> parts = new ArrayList<>();
        row.forEach((key, value) -> {
            if (!"id".equalsIgnoreCase(key) && value != null && !value.toString().isBlank()) {
                parts.add(key + "=" + value);
            }
        });
        return String.join(", ", parts.subList(0, Math.min(4, parts.size())));
    }
}
