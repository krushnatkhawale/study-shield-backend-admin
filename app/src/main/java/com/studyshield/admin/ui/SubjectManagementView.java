package com.studyshield.admin.ui;

import com.studyshield.admin.service.BackendDataService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;
import jakarta.annotation.security.RolesAllowed;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Route(value = "subjects", layout = MainLayout.class)
@RouteAlias(value = "subject-order", layout = MainLayout.class)
@PageTitle("Subjects")
@RolesAllowed("ADMIN")
public class SubjectManagementView extends VerticalLayout {

    private final BackendDataService api;
    private final ComboBox<Map<String, Object>> classSelect = new ComboBox<>("Class");
    private final Grid<Map<String, Object>> grid = new Grid<>();
    private final TextField name = new TextField("Name");
    private final TextField code = new TextField("Code");
    private final TextArea description = new TextArea("Description");
    private final IntegerField displayOrder = new IntegerField("Display order");
    private final Checkbox active = new Checkbox("Active");
    private final List<Map<String, Object>> rows = new ArrayList<>();
    private Map<String, Object> selected;

    public SubjectManagementView(BackendDataService api) {
        this.api = api;
        setSizeFull();
        setPadding(false);

        Button create = AdminUi.primary("New subject");
        create.addClickListener(e -> clear());
        VerticalLayout page = AdminUi.page("Subjects",
                "Subjects belong to a class. Display order is how they appear in the kid app.", create);

        classSelect.setItemLabelGenerator(item -> AdminUi.label(item, "name"));
        classSelect.setItems(api.list("class-grades"));
        classSelect.setWidth("280px");
        classSelect.addValueChangeListener(e -> refresh());

        grid.addColumn(r -> AdminUi.str(r, "displayOrder")).setHeader("#").setWidth("70px");
        grid.addColumn(r -> AdminUi.str(r, "name")).setHeader("Subject").setFlexGrow(1);
        grid.addColumn(r -> AdminUi.str(r, "code")).setHeader("Code");
        grid.addColumn(r -> AdminUi.str(r, "active")).setHeader("Active");
        grid.addComponentColumn(this::moveButtons).setHeader("Order").setAutoWidth(true);
        grid.setSizeFull();
        grid.asSingleSelect().addValueChangeListener(e -> edit(e.getValue()));

        FormLayout form = new FormLayout(classSelect, name, code, displayOrder, description, active);
        Button save = AdminUi.primary("Save");
        save.addClickListener(e -> save());
        Button delete = AdminUi.danger("Delete");
        delete.addClickListener(e -> {
            if (AdminUi.id(selected) == null) {
                Notification.show("Select a subject");
                return;
            }
            AdminUi.confirmDelete("Delete subject?", "Packs and quizzes under this subject may break.", () -> {
                api.delete("subjects", AdminUi.id(selected));
                clear();
                refresh();
            });
        });

        page.add(AdminUi.card(grid), AdminUi.card(form, new HorizontalLayout(save, delete)));
        add(page);
    }

    private HorizontalLayout moveButtons(Map<String, Object> item) {
        Button up = new Button("↑", e -> move(item, -1));
        Button down = new Button("↓", e -> move(item, 1));
        return new HorizontalLayout(up, down);
    }

    private void move(Map<String, Object> item, int delta) {
        int index = rows.indexOf(item);
        int target = index + delta;
        if (index < 0 || target < 0 || target >= rows.size()) {
            return;
        }
        rows.remove(index);
        rows.add(target, item);
        for (int i = 0; i < rows.size(); i++) {
            rows.get(i).put("displayOrder", i + 1);
            persistOrder(rows.get(i));
        }
        grid.getDataProvider().refreshAll();
    }

    private void persistOrder(Map<String, Object> item) {
        try {
            Map<String, Object> payload = new LinkedHashMap<>(item);
            payload.remove("id");
            api.update("subjects", AdminUi.id(item), Map.of(
                    "name", item.get("name"),
                    "code", item.get("code"),
                    "description", item.get("description"),
                    "classGradeId", item.get("classGradeId"),
                    "active", item.get("active"),
                    "displayOrder", item.get("displayOrder")));
        } catch (Exception ex) {
            Notification.show("Could not save order: " + ex.getMessage());
        }
    }

    private void refresh() {
        rows.clear();
        if (AdminUi.id(classSelect.getValue()) == null) {
            grid.setItems(List.of());
            return;
        }
        rows.addAll(api.listBy("subjects", "class-grade", AdminUi.id(classSelect.getValue())));
        grid.setItems(rows);
    }

    private void edit(Map<String, Object> row) {
        selected = row;
        if (row == null) {
            return;
        }
        name.setValue(AdminUi.str(row, "name"));
        code.setValue(AdminUi.str(row, "code"));
        description.setValue(AdminUi.str(row, "description"));
        try {
            displayOrder.setValue(Integer.parseInt(AdminUi.str(row, "displayOrder")));
        } catch (NumberFormatException ex) {
            displayOrder.setValue(1);
        }
        active.setValue(Boolean.parseBoolean(AdminUi.str(row, "active")));
    }

    private void clear() {
        selected = null;
        name.clear();
        code.clear();
        description.clear();
        displayOrder.setValue(rows.size() + 1);
        active.setValue(true);
        grid.deselectAll();
    }

    private void save() {
        if (AdminUi.id(classSelect.getValue()) == null) {
            Notification.show("Choose a class");
            return;
        }
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("name", name.getValue());
        payload.put("code", code.getValue().isBlank() ? name.getValue().toUpperCase().replace(' ', '_') : code.getValue());
        payload.put("description", description.getValue());
        payload.put("classGradeId", AdminUi.id(classSelect.getValue()));
        payload.put("active", active.getValue());
        payload.put("displayOrder", displayOrder.getValue() == null ? 1 : displayOrder.getValue());
        try {
            if (AdminUi.id(selected) != null) {
                api.update("subjects", AdminUi.id(selected), payload);
                Notification.show("Subject updated");
            } else {
                api.create("subjects", payload);
                Notification.show("Subject created");
            }
            clear();
            refresh();
        } catch (Exception ex) {
            Notification.show(ex.getMessage());
        }
    }
}
