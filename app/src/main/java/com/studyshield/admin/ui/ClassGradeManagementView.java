package com.studyshield.admin.ui;

import com.studyshield.admin.service.BackendDataService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Manage class grades: create, rename and delete a class, and add subjects to it. New classes are
 * attached to a board so the freemium catalog can seed them.
 */
@Route(value = "classes", layout = MainLayout.class)
@PageTitle("Class management")
@RolesAllowed("ADMIN")
public class ClassGradeManagementView extends VerticalLayout {

    private final BackendDataService backendDataService;

    private final ComboBox<Map<String, Object>> boardSelect = new ComboBox<>();
    private final Grid<Map<String, Object>> grid = new Grid<>();
    private final FormLayout editor = new FormLayout();

    private final TextField nameField = new TextField("Class name");
    private final TextArea descriptionField = new TextArea("Description");

    private final Button saveButton = new Button("Save class");
    private final Button newButton = new Button("New class");
    private final Button subjectsButton = new Button("Manage subjects");

    private Map<String, Object> selected;

    public ClassGradeManagementView(BackendDataService backendDataService) {
        this.backendDataService = backendDataService;

        setPadding(true);
        setSpacing(true);
        setWidthFull();
        add(new H2("Class management"));

        boardSelect.setLabel("Board");
        boardSelect.setItemLabelGenerator(item -> String.valueOf(item.getOrDefault("name", "?")));
        boardSelect.setItems(backendDataService.list("boards"));

        grid.addColumn(item -> item.getOrDefault("id", "-")).setHeader("ID").setWidth("70px");
        grid.addColumn(item -> item.getOrDefault("name", "-")).setHeader("Class");
        grid.addColumn(item -> item.getOrDefault("boardName", "-")).setHeader("Board");
        grid.setSelectionMode(Grid.SelectionMode.SINGLE);
        grid.asSingleSelect().addValueChangeListener(event -> loadIntoEditor(event.getValue()));
        grid.setHeight("300px");
        grid.setItems(backendDataService.list("class-grades"));

        editor.add(nameField, descriptionField);
        editor.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));

        newButton.addClickListener(e -> resetEditor());
        saveButton.addClickListener(e -> saveClass());
        subjectsButton.addClickListener(e -> {
            if (grid.getSelectedItems().isEmpty()) {
                Notification.show("Select a class to manage its subjects");
                return;
            }
            getUI().ifPresent(ui -> ui.navigate(SubjectOrderView.class));
        });

        add(grid, boardSelect, editor, new HorizontalLayout(newButton, saveButton, subjectsButton));
    }

    private void loadIntoEditor(Map<String, Object> classGrade) {
        if (classGrade == null || classGrade.isEmpty()) {
            return;
        }
        selected = classGrade;
        nameField.setValue(String.valueOf(classGrade.getOrDefault("name", "")));
        descriptionField.setValue(String.valueOf(classGrade.getOrDefault("description", "")));
    }

    private void resetEditor() {
        selected = null;
        nameField.clear();
        descriptionField.clear();
    }

    private void saveClass() {
        if (boardSelect.getValue() == null || boardSelect.getValue().get("id") == null) {
            Notification.show("Choose a board for new classes");
            return;
        }
        Long boardId = Long.valueOf(String.valueOf(boardSelect.getValue().get("id")));
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("name", nameField.getValue());
        payload.put("description", descriptionField.getValue());
        payload.put("boardId", boardId);

        try {
            if (selected != null && selected.get("id") != null) {
                Long id = Long.valueOf(String.valueOf(selected.get("id")));
                backendDataService.update("class-grades", id, payload);
                Notification.show("Class updated");
            } else {
                backendDataService.create("class-grades", payload);
                Notification.show("Class created");
            }
            resetEditor();
            grid.setItems(backendDataService.list("class-grades"));
        } catch (Exception ex) {
            Notification.show("Save failed: " + ex.getMessage());
        }
    }
}
