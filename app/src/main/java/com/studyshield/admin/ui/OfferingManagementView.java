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
        VerticalLayout page = AdminUi.page("Subject mappings",
                "Which subjects each board class offers. A subject mapping connects a board class (e.g. CBSE Class 3) to a subject (e.g. MATH). Content packs attach to subject mappings. Double-click a row to remove it.",
                create);

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
        grid.addItemDoubleClickListener(e -> confirmDeleteOffering(e.getItem()));

        com.vaadin.flow.component.orderedlayout.HorizontalLayout filters =
                new com.vaadin.flow.component.orderedlayout.HorizontalLayout(boardSelect, boardClassSelect);
        filters.setAlignItems(Alignment.END);

        page.add(filters, AdminUi.card(grid));
        page.setFlexGrow(1, page.getComponentAt(1));
        add(page);
        setFlexGrow(1, page);
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
                        "boardCode", AdminUi.str(boardClassField.getValue(), "boardCode"),
                        "className", AdminUi.str(boardClassField.getValue(), "displayName"),
                        "subject", AdminUi.str(subjectField.getValue(), "code"));
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

    private void confirmDeleteOffering(Map<String, Object> item) {
        Long id = AdminUi.id(item);
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
        grid.setItems(boardClassId == null ? List.of() : api.listBy("offerings", "board-class", boardClassId));
    }
}
