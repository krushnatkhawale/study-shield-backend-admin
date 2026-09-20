package com.studyshield.admin.ui;

import com.studyshield.admin.service.BackendDataService;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
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
    private final Grid<Map<String, Object>> grid = new Grid<>();
    private final List<Map<String, Object>> rows = new ArrayList<>();

    public SubjectManagementView(BackendDataService api) {
        this.api = api;
        setSizeFull();
        setPadding(false);

        Button create = AdminUi.primary("New subject");
        create.addClickListener(e -> openDialog(null));
        VerticalLayout page = AdminUi.page("Subjects",
                "Global subjects across all boards. Display order is how they appear in the kid app. Double-click a row to edit.", create);

        grid.addColumn(r -> AdminUi.str(r, "id")).setHeader("ID").setAutoWidth(true);
        grid.addColumn(r -> AdminUi.str(r, "displayOrder")).setHeader("#").setWidth("70px");
        grid.addColumn(r -> AdminUi.str(r, "name")).setHeader("Subject").setFlexGrow(1);
        grid.addColumn(r -> AdminUi.str(r, "code")).setHeader("Code");
        grid.addColumn(r -> AdminUi.str(r, "active")).setHeader("Active");
        grid.addComponentColumn(this::moveButtons).setHeader("Order").setAutoWidth(true);
        grid.setSizeFull();
        grid.setMinHeight("320px");
        grid.addItemDoubleClickListener(e -> openDialog(e.getItem()));

        com.vaadin.flow.component.Component card = AdminUi.card(grid);
        page.add(card);
        page.setFlexGrow(1, card);
        add(page);
        setFlexGrow(1, page);
        refresh();
    }

    private HorizontalLayout moveButtons(Map<String, Object> item) {
        Button up = new Button("\u2191", e -> move(item, -1));
        Button down = new Button("\u2193", e -> move(item, 1));
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
            api.update("subjects", AdminUi.id(item), Map.of(
                    "name", item.get("name"),
                    "code", item.get("code"),
                    "description", item.get("description") != null ? item.get("description") : "",
                    "active", item.get("active") != null ? item.get("active") : true,
                    "displayOrder", item.get("displayOrder")));
        } catch (Exception ex) {
            Notification.show("Could not save order: " + ex.getMessage());
        }
    }

    private void refresh() {
        rows.clear();
        rows.addAll(api.list("subjects"));
        grid.setItems(rows);
    }

    private void openDialog(Map<String, Object> row) {
        Long id = AdminUi.id(row);
        boolean isNew = id == null;

        TextField name = new TextField("Name");
        name.setRequired(true);
        name.setWidthFull();
        TextField code = new TextField("Code");
        code.setWidthFull();
        code.setHelperText("Leave blank to auto-derive from the name.");
        IntegerField displayOrder = new IntegerField("Display order");
        displayOrder.setWidthFull();
        TextArea description = new TextArea("Description");
        description.setWidthFull();
        Checkbox active = new Checkbox("Active");
        if (row != null) {
            name.setValue(AdminUi.str(row, "name"));
            code.setValue(AdminUi.str(row, "code"));
            description.setValue(AdminUi.str(row, "description"));
            try {
                displayOrder.setValue(Integer.parseInt(AdminUi.str(row, "displayOrder")));
            } catch (NumberFormatException ex) {
                displayOrder.setValue(1);
            }
            active.setValue(Boolean.parseBoolean(AdminUi.str(row, "active")));
        } else {
            displayOrder.setValue(rows.size() + 1);
            active.setValue(true);
        }

        FormLayout form = new FormLayout(name, code, displayOrder, description, active);
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1, FormLayout.ResponsiveStep.LabelsPosition.TOP),
                new FormLayout.ResponsiveStep("480px", 2, FormLayout.ResponsiveStep.LabelsPosition.TOP));
        form.setColspan(description, 2);
        form.setWidthFull();

        Dialog dialog = new Dialog();
        dialog.setHeaderTitle(isNew ? "New subject" : "Edit subject");
        dialog.setModal(true);
        dialog.setWidth("560px");
        dialog.setMaxWidth("95vw");
        dialog.add(form);

        Button save = AdminUi.primary("Save");
        save.addClickListener(e -> {
            if (name.getValue().isBlank()) {
                Notification.show("Name is required");
                name.focus();
                return;
            }
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("name", name.getValue().trim());
            payload.put("code", code.getValue().isBlank() ? name.getValue().trim().toUpperCase().replace(' ', '_') : code.getValue().trim());
            payload.put("description", description.getValue());
            payload.put("active", active.getValue());
            payload.put("displayOrder", displayOrder.getValue() == null ? 1 : displayOrder.getValue());
            try {
                if (!isNew) {
                    api.update("subjects", id, payload);
                    Notification.show("Subject updated");
                } else {
                    api.create("subjects", payload);
                    Notification.show("Subject created");
                }
                dialog.close();
                refresh();
            } catch (Exception ex) {
                Notification.show(ex.getMessage());
            }
        });
        Button delete = AdminUi.danger("Delete");
        delete.setVisible(!isNew);
        delete.addClickListener(e -> AdminUi.confirmDelete("Delete subject?", "Offerings and packs under this subject may break.", () -> {
            api.delete("subjects", id);
            dialog.close();
            refresh();
        }));
        Button cancel = new Button("Cancel", e -> dialog.close());
        cancel.addClickShortcut(Key.ESCAPE);

        HorizontalLayout footer = new HorizontalLayout(delete, save, cancel);
        footer.setWidthFull();
        footer.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
        footer.setAlignItems(FlexComponent.Alignment.CENTER);
        dialog.getFooter().add(footer);

        dialog.open();
        name.focus();
    }
}
