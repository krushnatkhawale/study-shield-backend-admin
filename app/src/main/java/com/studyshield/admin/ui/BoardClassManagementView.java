package com.studyshield.admin.ui;

import com.studyshield.admin.service.BackendDataService;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.button.Button;
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

import java.util.List;
import java.util.Map;

@Route(value = "board-classes", layout = MainLayout.class)
@PageTitle("Board classes")
@RolesAllowed("ADMIN")
public class BoardClassManagementView extends VerticalLayout {

    private final BackendDataService api;
    private final ComboBox<Map<String, Object>> boardFilter = new ComboBox<>("Board");
    private final Grid<Map<String, Object>> grid = new Grid<>();

    public BoardClassManagementView(BackendDataService api) {
        this.api = api;
        setSizeFull();
        setPadding(false);

        Button create = AdminUi.primary("New board class");
        create.addClickListener(e -> openDialog(null));
        VerticalLayout page = AdminUi.page("Board classes",
                "Mapping from a board to a global ordinal with a board-specific display name (e.g. CBSE → 'Class 3' at ordinal 7). Double-click a row to edit.",
                create);

        boardFilter.setItemLabelGenerator(item -> AdminUi.label(item, "name", "code"));
        boardFilter.setItems(api.list("boards"));
        boardFilter.setWidth("280px");
        boardFilter.setPlaceholder("Filter by board");
        boardFilter.addValueChangeListener(e -> refresh());

        grid.addColumn(r -> AdminUi.str(r, "id")).setHeader("ID").setAutoWidth(true);
        grid.addColumn(r -> AdminUi.str(r, "displayName")).setHeader("Display name").setFlexGrow(1);
        grid.addColumn(r -> AdminUi.str(r, "ordinal")).setHeader("Ordinal").setWidth("90px");
        grid.addColumn(r -> AdminUi.str(r, "boardCode")).setHeader("Board").setAutoWidth(true);
        grid.setSizeFull();
        grid.addItemDoubleClickListener(e -> openDialog(e.getItem()));

        page.add(boardFilter, AdminUi.card(grid));
        add(page);
        refresh();
    }

    private void refresh() {
        Long boardId = AdminUi.id(boardFilter.getValue());
        if (boardId == null) {
            grid.setItems(List.of());
            return;
        }
        grid.setItems(api.listBy("board-classes", "board", boardId));
    }

    private void openDialog(Map<String, Object> row) {
        Long id = AdminUi.id(row);
        boolean isNew = id == null;

        ComboBox<Map<String, Object>> boardSelect = new ComboBox<>("Board");
        boardSelect.setItems(api.list("boards"));
        boardSelect.setItemLabelGenerator(item -> AdminUi.label(item, "name", "code"));
        boardSelect.setWidthFull();
        IntegerField ordinal = new IntegerField("Ordinal");
        ordinal.setWidthFull();
        TextField displayName = new TextField("Display name");
        displayName.setWidthFull();
        if (row != null) {
            // Preselect the board matching the row's boardCode, if present.
            String boardCode = AdminUi.str(row, "boardCode");
            boardSelect.getListDataView().getItems()
                    .filter(b -> boardCode.equalsIgnoreCase(AdminUi.str(b, "code")))
                    .findFirst().ifPresent(boardSelect::setValue);
            try {
                ordinal.setValue(Integer.parseInt(AdminUi.str(row, "ordinal")));
            } catch (NumberFormatException ignored) {
            }
            displayName.setValue(AdminUi.str(row, "displayName"));
        } else if (boardFilter.getValue() != null) {
            boardSelect.setValue(boardFilter.getValue());
        }

        FormLayout form = AdminUi.entityForm(boardSelect, ordinal, displayName);
        form.setColspan(displayName, 2);

        Dialog dialog = AdminUi.entityDialog(isNew ? "New board class" : "Edit board class", form);

        Button save = AdminUi.primary("Save");
        save.addClickListener(e -> {
            if (boardSelect.getValue() == null) {
                Notification.show("Choose a board");
                boardSelect.focus();
                return;
            }
            if (ordinal.getValue() == null) {
                Notification.show("Ordinal is required");
                ordinal.focus();
                return;
            }
            if (displayName.getValue().isBlank()) {
                Notification.show("Display name is required");
                displayName.focus();
                return;
            }
            try {
                Map<String, Object> payload = Map.of(
                        "boardId", AdminUi.id(boardSelect.getValue()),
                        "ordinal", ordinal.getValue(),
                        "displayName", displayName.getValue().trim());
                if (!isNew) {
                    api.update("board-classes", id, payload);
                    Notification.show("Board class updated");
                } else {
                    api.create("board-classes", payload);
                    Notification.show("Board class created");
                }
                dialog.close();
                refresh();
            } catch (Exception ex) {
                Notification.show(ex.getMessage());
            }
        });
        Button delete = AdminUi.danger("Delete");
        delete.setVisible(!isNew);
        delete.addClickListener(e -> AdminUi.confirmDelete("Delete board class?", "Offerings under this class may break.", () -> {
            api.delete("board-classes", id);
            dialog.close();
            refresh();
        }));
        AdminUi.dialogFooter(dialog, delete, save);

        dialog.open();
        boardSelect.focus();
    }
}
