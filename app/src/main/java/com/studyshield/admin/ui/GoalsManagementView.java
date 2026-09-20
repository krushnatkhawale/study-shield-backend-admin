package com.studyshield.admin.ui;

import com.studyshield.admin.service.BackendDataService;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Weekly goals per kid (e.g. 5 quizzes/week). Progress is computed live by the
 * backend from quiz results; the grid shows current/target next to each goal.
 */
@Route(value = "goals", layout = MainLayout.class)
@PageTitle("Goals")
@RolesAllowed("ADMIN")
public class GoalsManagementView extends VerticalLayout {

    private static final List<String> TYPES = List.of("WEEKLY_QUIZZES", "WEEKLY_BEST");

    private final BackendDataService api;
    private final Grid<Map<String, Object>> grid = new Grid<>();
    private final List<Map<String, Object>> rows = new ArrayList<>();

    public GoalsManagementView(BackendDataService api) {
        this.api = api;
        setSizeFull();
        setPadding(false);

        Button create = AdminUi.primary("New goal");
        create.addClickListener(e -> openDialog(null));
        VerticalLayout page = AdminUi.page("Goals",
                "Weekly targets per kid. Progress counts this week's attempts (Monday start). Double-click a row to edit.", create);

        grid.addColumn(r -> AdminUi.str(r, "id")).setHeader("ID").setAutoWidth(true);
        grid.addColumn(r -> AdminUi.str(r, "childName")).setHeader("Kid").setFlexGrow(1);
        grid.addColumn(r -> AdminUi.str(r, "type")).setHeader("Type").setAutoWidth(true);
        grid.addColumn(r -> AdminUi.str(r, "target")).setHeader("Target").setWidth("90px");
        grid.addColumn(this::progressLabel).setHeader("Progress").setAutoWidth(true);
        grid.addColumn(r -> AdminUi.str(r, "active")).setHeader("Active");
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

    private String progressLabel(Map<String, Object> row) {
        Object current = row.get("current");
        if (current == null) {
            return "—";
        }
        boolean achieved = Boolean.parseBoolean(AdminUi.str(row, "achieved"));
        return current + "/" + AdminUi.str(row, "target") + (achieved ? " ✓" : "");
    }

    private void refresh() {
        rows.clear();
        List<Map<String, Object>> goals = api.list("goals");
        // Enrich each goal with live progress (best-effort; offline backend keeps plain rows).
        Map<String, List<Map<String, Object>>> progressByChild = new LinkedHashMap<>();
        for (Map<String, Object> goal : goals) {
            Map<String, Object> enriched = new LinkedHashMap<>(goal);
            try {
                String child = AdminUi.str(goal, "childName");
                List<Map<String, Object>> progress = progressByChild.computeIfAbsent(
                        child, c -> api.listByQuery("goals/progress", "childName", c));
                progress.stream()
                        .filter(p -> String.valueOf(p.get("goalId")).equals(String.valueOf(goal.get("id"))))
                        .findFirst()
                        .ifPresent(p -> {
                            enriched.put("current", p.get("current"));
                            enriched.put("achieved", p.get("achieved"));
                        });
            } catch (Exception ignored) {
            }
            rows.add(enriched);
        }
        grid.setItems(rows);
    }

    private void openDialog(Map<String, Object> row) {
        Long id = AdminUi.id(row);
        boolean isNew = id == null;

        TextField childName = new TextField("Kid name");
        childName.setRequired(true);
        childName.setWidthFull();
        ComboBox<String> type = new ComboBox<>("Type");
        type.setItems(TYPES);
        type.setWidthFull();
        type.setHelperText("WEEKLY_QUIZZES counts attempts; WEEKLY_BEST counts scores ≥80%.");
        IntegerField target = new IntegerField("Target per week");
        target.setWidthFull();
        target.setMin(1);
        Checkbox active = new Checkbox("Active");
        if (row != null) {
            childName.setValue(AdminUi.str(row, "childName"));
            String t = AdminUi.str(row, "type");
            type.setValue(TYPES.contains(t) ? t : TYPES.get(0));
            try {
                target.setValue(Integer.parseInt(AdminUi.str(row, "target")));
            } catch (NumberFormatException ex) {
                target.setValue(5);
            }
            active.setValue(Boolean.parseBoolean(AdminUi.str(row, "active")));
        } else {
            type.setValue(TYPES.get(0));
            target.setValue(5);
            active.setValue(true);
        }

        FormLayout form = AdminUi.entityForm(childName, type, target, active);
        Dialog dialog = AdminUi.entityDialog(isNew ? "New goal" : "Edit goal", form);

        Button save = AdminUi.primary("Save");
        save.addClickListener(e -> {
            if (childName.getValue().isBlank()) {
                Notification.show("Kid name is required");
                childName.focus();
                return;
            }
            if (target.getValue() == null || target.getValue() < 1) {
                Notification.show("Target must be at least 1");
                target.focus();
                return;
            }
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("childName", childName.getValue().trim());
            payload.put("type", type.getValue());
            payload.put("target", target.getValue());
            payload.put("active", active.getValue());
            try {
                if (!isNew) {
                    api.update("goals", id, payload);
                    Notification.show("Goal updated");
                } else {
                    api.create("goals", payload);
                    Notification.show("Goal created");
                }
                dialog.close();
                refresh();
            } catch (Exception ex) {
                Notification.show(ex.getMessage());
            }
        });
        Button delete = AdminUi.danger("Delete");
        delete.setVisible(!isNew);
        delete.addClickListener(e -> AdminUi.confirmDelete("Delete goal?", "The kid's progress history stays.", () -> {
            api.delete("goals", id);
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
        childName.focus();
    }
}
