package com.studyshield.admin.ui;

import com.studyshield.admin.service.BackendDataService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.notification.Notification;
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
    private final ComboBox<Map<String, Object>> boardSelect = new ComboBox<>("Board");
    private final Grid<Map<String, Object>> grid = new Grid<>();
    private final IntegerField ordinal = new IntegerField("Ordinal");
    private final TextField displayName = new TextField("Display name");

    public BoardClassManagementView(BackendDataService api) {
        this.api = api;
        setSizeFull();
        setPadding(false);

        Button create = AdminUi.primary("New board class");
        create.addClickListener(e -> clear());
        VerticalLayout page = AdminUi.page("Board classes",
                "Mapping from a board to a global ordinal with a board-specific display name (e.g. CBSE → 'Class 3' at ordinal 7).",
                create);

        boardSelect.setItemLabelGenerator(item -> AdminUi.label(item, "name", "code"));
        boardSelect.setItems(api.list("boards"));
        boardSelect.setWidth("280px");
        boardSelect.addValueChangeListener(e -> refresh());

        grid.addColumn(r -> AdminUi.str(r, "displayName")).setHeader("Display name").setFlexGrow(1);
        grid.addColumn(r -> AdminUi.str(r, "ordinal")).setHeader("Ordinal").setWidth("90px");
        grid.addColumn(r -> AdminUi.str(r, "boardCode")).setHeader("Board").setAutoWidth(true);
        grid.setSizeFull();

        FormLayout form = new FormLayout(boardSelect, ordinal, displayName);
        Button save = AdminUi.primary("Create board class");
        save.addClickListener(e -> save());

        page.add(AdminUi.card(grid), AdminUi.card(form, save));
        add(page);
        refresh();
    }

    private void refresh() {
        Long boardId = AdminUi.id(boardSelect.getValue());
        if (boardId == null) {
            grid.setItems(List.of());
            return;
        }
        grid.setItems(api.listBy("board-classes", "board", boardId));
    }

    private void save() {
        if (boardSelect.getValue() == null) {
            Notification.show("Choose a board");
            return;
        }
        if (ordinal.getValue() == null) {
            Notification.show("Ordinal is required");
            return;
        }
        if (displayName.getValue().isBlank()) {
            Notification.show("Display name is required");
            return;
        }
        try {
            Map<String, Object> payload = Map.of(
                    "boardId", AdminUi.id(boardSelect.getValue()),
                    "ordinal", ordinal.getValue(),
                    "displayName", displayName.getValue().trim());
            api.create("board-classes", payload);
            Notification.show("Board class created");
            clear();
            refresh();
        } catch (Exception ex) {
            Notification.show(ex.getMessage());
        }
    }

    private void clear() {
        ordinal.clear();
        displayName.clear();
    }
}
