package com.studyshield.admin.ui;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.studyshield.admin.service.BackendDataService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.data.renderer.TextRenderer;
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
            "class-grades",
            "subjects",
            "content-packs",
            "quizzes",
            "questions",
            "quiz-bundles",
            "users",
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
    private final TextArea recordEditor = new TextArea("Record JSON");
    private Map<String, Object> selectedRecord;

    public SettingsView(BackendDataService backendDataService, ObjectMapper objectMapper) {
        this.backendDataService = backendDataService;
        this.objectMapper = objectMapper;

        setPadding(true);
        setSpacing(true);
        setSizeFull();

        add(new H2("Backend table browser"));

        collectionSelect.setItems(COLLECTIONS);
        collectionSelect.setValue("boards");
        collectionSelect.setLabel("Table");
        collectionSelect.addValueChangeListener(event -> loadCollection(event.getValue()));

        Button refreshButton = new Button("Refresh", event -> loadCollection(collectionSelect.getValue()));
        Button newButton = new Button("New record", event -> { selectedRecord = new LinkedHashMap<>(); recordEditor.setValue("{}\n"); });
        Button saveButton = new Button("Save", event -> saveRecord());
        Button deleteButton = new Button("Delete", event -> deleteRecord());

        HorizontalLayout controls = new HorizontalLayout(collectionSelect, refreshButton, newButton, saveButton, deleteButton);
        controls.setAlignItems(Alignment.END);
        add(controls);

        grid.addColumn(item -> item.getOrDefault("id", "-"))
                .setHeader("ID");
        grid.addColumn(item -> summarize(item))
                .setHeader("Summary");
        grid.setSelectionMode(Grid.SelectionMode.SINGLE);
        grid.asSingleSelect().addValueChangeListener(event -> {
            selectedRecord = event.getValue();
            if (selectedRecord != null) {
                try {
                    recordEditor.setValue(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(selectedRecord));
                } catch (JsonProcessingException e) {
                    recordEditor.setValue(selectedRecord.toString());
                }
            }
        });
        grid.setHeight("350px");

        recordEditor.setHeight("220px");
        recordEditor.setWidthFull();

        add(grid, recordEditor);
        loadCollection("boards");
    }

    private void loadCollection(String collection) {
        List<Map<String, Object>> records = backendDataService.list(collection);
        grid.setItems(records);
        if (!records.isEmpty()) {
            grid.select(records.getFirst());
        } else {
            selectedRecord = new LinkedHashMap<>();
            recordEditor.setValue("{}\n");
        }
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

    private void saveRecord() {
        String collection = collectionSelect.getValue();
        String json = recordEditor.getValue();
        try {
            Map<String, Object> payload = objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
            if (selectedRecord != null && selectedRecord.get("id") != null) {
                Long id = Long.valueOf(String.valueOf(selectedRecord.get("id")));
                backendDataService.update(collection, id, payload);
                Notification.show("Record updated");
            } else {
                backendDataService.create(collection, payload);
                Notification.show("Record created");
            }
            loadCollection(collection);
        } catch (Exception ex) {
            Notification.show("Invalid JSON or backend request failed");
        }
    }

    private void deleteRecord() {
        if (selectedRecord == null || selectedRecord.get("id") == null) {
            Notification.show("Select a record to delete");
            return;
        }
        String collection = collectionSelect.getValue();
        Long id = Long.valueOf(String.valueOf(selectedRecord.get("id")));
        backendDataService.delete(collection, id);
        Notification.show("Record deleted");
        loadCollection(collection);
    }
}
