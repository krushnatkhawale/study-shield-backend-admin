package com.studyshield.admin.ui;

import com.studyshield.admin.service.BackendDataService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;

import java.util.List;
import java.util.Map;

@Route(value = "offerings", layout = MainLayout.class)
@PageTitle("Subject mappings")
@RolesAllowed("ADMIN")
public class OfferingManagementView extends VerticalLayout {

    private final BackendDataService api;
    private final ComboBox<Map<String, Object>> boardSelect = new ComboBox<>("Board");
    private final ComboBox<Map<String, Object>> boardClassSelect = new ComboBox<>("Board class");

    private final ComboBox<Map<String, Object>> subjectSelect = new ComboBox<>("Subject");
    private final Grid<Map<String, Object>> grid = new Grid<>();

    public OfferingManagementView(BackendDataService api) {
        this.api = api;
        setSizeFull();
        setPadding(false);

        Button create = AdminUi.primary("New subject mapping");
        create.addClickListener(e -> openDialog());
        Button bulk = new Button("Bulk assign subjects");
        bulk.addClickListener(e -> openBulkDialog());
        VerticalLayout page = AdminUi.page("Subject mappings",
                "Which subjects each board class offers. A subject mapping connects a board class (e.g. CBSE Class 3) to a subject (e.g. MATH). Content packs attach to subject mappings. Double-click a row to remove it.",
                create, bulk);

        boardSelect.setItemLabelGenerator(item -> AdminUi.label(item, "name", "code"));
        boardSelect.setItems(api.list("boards"));
        boardSelect.setWidth("240px");
        boardSelect.addValueChangeListener(e -> loadBoardClasses());

        boardClassSelect.setItemLabelGenerator(item -> AdminUi.label(item, "displayName") + " (ordinal " + AdminUi.str(item, "ordinal") + ")");
        boardClassSelect.setWidth("300px");
        boardClassSelect.addValueChangeListener(e -> loadOfferings());

        subjectSelect.setItemLabelGenerator(item -> AdminUi.label(item, "name"));
        subjectSelect.setItems(api.list("subjects"));
        subjectSelect.setWidth("240px");

        grid.addColumn(r -> AdminUi.str(r, "id")).setHeader("ID").setAutoWidth(true);
        grid.addColumn(r -> AdminUi.str(r, "displayName")).setHeader("Board class").setFlexGrow(1);
        grid.addColumn(r -> AdminUi.str(r, "ordinal")).setHeader("Ordinal").setWidth("80px");
        grid.addColumn(r -> AdminUi.str(r, "subjectCode")).setHeader("Subject").setAutoWidth(true);
        grid.addColumn(r -> AdminUi.str(r, "subjectName")).setHeader("Subject name").setAutoWidth(true);
        grid.setSizeFull();
        grid.setMinHeight("320px");
        grid.addItemDoubleClickListener(e -> confirmDeleteOffering(e.getItem()));

        com.vaadin.flow.component.orderedlayout.HorizontalLayout filters =
                new com.vaadin.flow.component.orderedlayout.HorizontalLayout(boardSelect, boardClassSelect);
        filters.setAlignItems(Alignment.END);

        com.vaadin.flow.component.Component card = AdminUi.card(grid);
        page.add(filters, card);
        page.setFlexGrow(1, card);
        add(page);
        setFlexGrow(1, page);
        loadOfferings();
    }

    private void openDialog() {
        ComboBox<Map<String, Object>> boardClassField = new ComboBox<>("Board class");
        boardClassField.setItemLabelGenerator(item -> AdminUi.label(item, "displayName") + " (ordinal " + AdminUi.str(item, "ordinal") + ")");
        boardClassField.setWidthFull();
        // Seed options from the current filter selection when available.
        if (boardClassSelect.getValue() != null) {
            boardClassField.setItems(List.of(boardClassSelect.getValue()));
            boardClassField.setValue(boardClassSelect.getValue());
        } else if (boardSelect.getValue() != null) {
            boardClassField.setItems(api.listBy("board-classes", "board", AdminUi.id(boardSelect.getValue())));
        }
        ComboBox<Map<String, Object>> subjectField = new ComboBox<>("Subject");
        subjectField.setItemLabelGenerator(item -> AdminUi.label(item, "name"));
        subjectField.setItems(api.list("subjects"));
        subjectField.setWidthFull();

        FormLayout form = AdminUi.entityForm(boardClassField, subjectField);

        Dialog dialog = AdminUi.entityDialog("New subject mapping", form);

        Button save = AdminUi.primary("Save");
        save.addClickListener(e -> {
            if (boardClassField.getValue() == null) {
                Notification.show("Choose a board class");
                boardClassField.focus();
                return;
            }
            if (subjectField.getValue() == null) {
                Notification.show("Choose a subject");
                subjectField.focus();
                return;
            }
            try {
                Map<String, Object> payload = Map.of(
                        "boardClassId", AdminUi.id(boardClassField.getValue()),
                        "subjectId", AdminUi.id(subjectField.getValue()));
                api.create("offerings", payload);
                Notification.show("Subject mapping created");
                dialog.close();
                loadOfferings();
            } catch (Exception ex) {
                Notification.show(ex.getMessage());
            }
        });
        Button delete = AdminUi.danger("Delete");
        delete.setVisible(false);
        AdminUi.dialogFooter(dialog, delete, save);

        dialog.open();
        boardClassField.focus();
    }

    private void openBulkDialog() {
        ComboBox<Map<String, Object>> boardClassField = new ComboBox<>("Board class");
        boardClassField.setItemLabelGenerator(item -> AdminUi.label(item, "displayName") + " (ordinal " + AdminUi.str(item, "ordinal") + ")");
        boardClassField.setWidthFull();
        boardClassField.setItems(api.list("board-classes"));
        if (boardClassSelect.getValue() != null) {
            boardClassField.setValue(boardClassSelect.getValue());
        } else if (boardSelect.getValue() != null) {
            boardClassField.setItems(api.listBy("board-classes", "board", AdminUi.id(boardSelect.getValue())));
        }

        List<Map<String, Object>> subjects = api.list("subjects");
        com.vaadin.flow.component.checkbox.CheckboxGroup<Map<String, Object>> subjectGroup =
                new com.vaadin.flow.component.checkbox.CheckboxGroup<>("Subjects");
        subjectGroup.setItems(subjects);
        subjectGroup.setItemLabelGenerator(item -> AdminUi.label(item, "name") + " (" + AdminUi.str(item, "code") + ")");
        subjectGroup.setWidthFull();

        // Pre-check subjects already mapped for the selected board class.
        java.util.Map<Long, Long> offeringBySubject = new java.util.HashMap<>();
        Runnable reloadMapped = () -> {
            offeringBySubject.clear();
            subjectGroup.clear();
            Long bcId = AdminUi.id(boardClassField.getValue());
            if (bcId == null) return;
            for (Map<String, Object> o : api.listBy("offerings", "board-class", bcId)) {
                try {
                    Long sid = Long.valueOf(String.valueOf(o.get("subjectId")));
                    offeringBySubject.put(sid, AdminUi.id(o));
                } catch (Exception ignored) {
                }
            }
            java.util.Set<Map<String, Object>> mapped = new java.util.HashSet<>();
            for (Map<String, Object> s : subjects) {
                if (offeringBySubject.containsKey(AdminUi.id(s))) mapped.add(s);
            }
            subjectGroup.setValue(mapped);
        };
        boardClassField.addValueChangeListener(e -> reloadMapped.run());
        reloadMapped.run();

        FormLayout form = AdminUi.entityForm(boardClassField);
        VerticalLayout content = new VerticalLayout(form, subjectGroup);
        content.setPadding(false);
        content.setWidthFull();

        Dialog dialog = AdminUi.entityDialog("Bulk assign subjects", content);
        dialog.setWidth("720px");

        Button save = AdminUi.primary("Save");
        save.addClickListener(e -> {
            Long bcId = AdminUi.id(boardClassField.getValue());
            if (bcId == null) {
                Notification.show("Choose a board class");
                boardClassField.focus();
                return;
            }
            java.util.Set<Long> selected = new java.util.HashSet<>();
            for (Map<String, Object> s : subjectGroup.getValue()) selected.add(AdminUi.id(s));
            java.util.Set<Long> mapped = new java.util.HashSet<>(offeringBySubject.keySet());
            java.util.List<Map<String, Object>> toAdd = new java.util.ArrayList<>();
            for (Map<String, Object> s : subjects) {
                if (selected.contains(AdminUi.id(s)) && !mapped.contains(AdminUi.id(s))) toAdd.add(s);
            }
            java.util.List<Long> toRemoveOfferingIds = new java.util.ArrayList<>();
            for (Map.Entry<Long, Long> en : offeringBySubject.entrySet()) {
                if (!selected.contains(en.getKey()) && en.getValue() != null) toRemoveOfferingIds.add(en.getValue());
            }
            Runnable apply = () -> {
                try {
                    for (Map<String, Object> s : toAdd) {
                        api.create("offerings", Map.of("boardClassId", bcId, "subjectId", AdminUi.id(s)));
                    }
                    for (Long oid : toRemoveOfferingIds) api.delete("offerings", oid);
                    Notification.show("Subject mappings updated");
                    dialog.close();
                    loadOfferings();
                } catch (Exception ex) {
                    Notification.show(ex.getMessage());
                }
            };
            if (!toRemoveOfferingIds.isEmpty()) {
                AdminUi.confirmDelete("Remove " + toRemoveOfferingIds.size() + " mapping(s)?",
                        "Removed subjects will lose their mappings for this board class.", apply);
            } else {
                apply.run();
            }
        });
        Button delete = AdminUi.danger("Delete");
        delete.setVisible(false);
        AdminUi.dialogFooter(dialog, delete, save);

        dialog.open();
        boardClassField.focus();
    }

    private void confirmDeleteOffering(Map<String, Object> item) {        Long id = AdminUi.id(item);
        if (id == null) {
            Notification.show("Subject mapping has no id — cannot delete");
            return;
        }
        AdminUi.confirmDelete("Delete subject mapping?", "Packs attached to this subject mapping may break.", () -> {
            api.delete("offerings", id);
            loadOfferings();
        });
    }

    private void loadBoardClasses() {
        Long boardId = AdminUi.id(boardSelect.getValue());
        boardClassSelect.setItems(boardId == null ? List.of() : api.listBy("board-classes", "board", boardId));
        grid.setItems(List.of());
    }

    private void loadOfferings() {
        Long boardClassId = AdminUi.id(boardClassSelect.getValue());
        grid.setItems(boardClassId == null ? api.list("offerings") : api.listBy("offerings", "board-class", boardClassId));
    }
}
