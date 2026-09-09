package com.studyshield.admin.ui;

import com.studyshield.admin.service.BackendDataService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.provider.ListDataProvider;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Lets an admin reorder the subjects of a class grade. Because the freemium quiz bundle picks the
 * first {@code QUIZZES_PER_CLASS} active subjects, displayOrder controls which quizzes the app sees.
 */
@Route(value = "subject-order", layout = MainLayout.class)
@PageTitle("Subject order")
@RolesAllowed("ADMIN")
public class SubjectOrderView extends VerticalLayout {

    private final BackendDataService backendDataService;

    private final ComboBox<Map<String, Object>> classGradeSelect = new ComboBox<>();
    private final Grid<Map<String, Object>> grid = new Grid<>();
    private final Button saveButton = new Button("Save order");

    private ListDataProvider<Map<String, Object>> dataProvider;
    private List<Map<String, Object>> subjects = new ArrayList<>();

    public SubjectOrderView(BackendDataService backendDataService) {
        this.backendDataService = backendDataService;

        setPadding(true);
        setSpacing(true);
        setWidthFull();
        add(new H2("Subject display order"));

        classGradeSelect.setLabel("Class grade");
        classGradeSelect.setItemLabelGenerator(item -> String.valueOf(item.getOrDefault("name", "?")));
        classGradeSelect.setItems(backendDataService.list("class-grades"));
        classGradeSelect.addValueChangeListener(event -> loadSubjects(event.getValue()));

        grid.addColumn(item -> item.getOrDefault("displayOrder", 0))
                .setHeader("#")
                .setWidth("60px");
        grid.addColumn(item -> item.getOrDefault("name", "-"))
                .setHeader("Subject");
        grid.addColumn(item -> item.getOrDefault("code", "-"))
                .setHeader("Code");
        grid.addComponentColumn(item -> {
            if (item == null) {
                return new HorizontalLayout();
            }
            Button up = new Button("↑", e -> move(item, -1));
            Button down = new Button("↓", e -> move(item, 1));
            up.getElement().getStyle().set("padding", "0 8px");
            down.getElement().getStyle().set("padding", "0 8px");
            return new HorizontalLayout(up, down);
        }).setHeader("Move");

        dataProvider = new ListDataProvider<>(subjects);
        grid.setDataProvider(dataProvider);
        grid.setHeight("400px");

        saveButton.setEnabled(false);
        saveButton.addClickListener(e -> saveOrder());

        add(classGradeSelect, grid, saveButton);
    }

    @SuppressWarnings("unchecked")
    private void loadSubjects(Map<String, Object> classGrade) {
        subjects.clear();
        if (classGrade == null || classGrade.get("id") == null) {
            dataProvider.refreshAll();
            saveButton.setEnabled(false);
            return;
        }
        Long classGradeId = Long.valueOf(String.valueOf(classGrade.get("id")));
        List<Map<String, Object>> fetched = backendDataService.listBy("subjects", "class-grade", classGradeId);
        for (Map<String, Object> item : fetched) {
            subjects.add(new LinkedHashMap<>(item));
        }
        dataProvider.refreshAll();
        saveButton.setEnabled(true);
    }

    private void move(Map<String, Object> item, int delta) {
        int index = subjects.indexOf(item);
        int target = index + delta;
        if (index < 0 || target < 0 || target >= subjects.size()) {
            return;
        }
        subjects.remove(index);
        subjects.add(target, item);
        renumber();
        dataProvider.refreshAll();
    }

    private void renumber() {
        for (int i = 0; i < subjects.size(); i++) {
            subjects.get(i).put("displayOrder", i + 1);
        }
    }

    private void saveOrder() {
        renumber();
        int failures = 0;
        for (Map<String, Object> item : subjects) {
            Object idObj = item.get("id");
            if (idObj == null) {
                continue;
            }
            Long id = Long.valueOf(String.valueOf(idObj));
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("name", item.get("name"));
            payload.put("code", item.get("code"));
            payload.put("description", item.get("description"));
            payload.put("classGradeId", item.get("classGradeId"));
            payload.put("active", item.get("active"));
            payload.put("displayOrder", item.get("displayOrder"));
            try {
                backendDataService.update("subjects", id, payload);
            } catch (Exception ex) {
                failures++;
            }
        }
        if (failures == 0) {
            Notification.show("Subject order saved");
        } else {
            Notification.show("Saved with " + failures + " failure(s)");
        }
    }
}
