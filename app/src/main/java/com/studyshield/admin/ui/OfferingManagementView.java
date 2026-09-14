package com.studyshield.admin.ui;

import com.studyshield.admin.service.BackendDataService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;

import java.util.List;
import java.util.Map;

@Route(value = "offerings", layout = MainLayout.class)
@PageTitle("Offerings")
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

        Button create = AdminUi.primary("New offering");
        create.addClickListener(e -> clear());
        VerticalLayout page = AdminUi.page("Offerings",
                "An offering connects a board class (e.g. CBSE Class 3) to a subject (e.g. MATH). Content packs attach to offerings.",
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

        grid.addColumn(r -> AdminUi.str(r, "displayName")).setHeader("Board class").setFlexGrow(1);
        grid.addColumn(r -> AdminUi.str(r, "ordinal")).setHeader("Ordinal").setWidth("80px");
        grid.addColumn(r -> AdminUi.str(r, "subjectCode")).setHeader("Subject").setAutoWidth(true);
        grid.addColumn(r -> AdminUi.str(r, "subjectName")).setHeader("Subject name").setAutoWidth(true);
        grid.setSizeFull();

        Button save = AdminUi.primary("Create offering");
        save.addClickListener(e -> save());
        HorizontalLayout selectors = new HorizontalLayout(boardSelect, boardClassSelect, subjectSelect);
        selectors.setAlignItems(Alignment.END);

        page.add(selectors, AdminUi.card(grid), AdminUi.card(save));
        add(page);
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

    private void save() {
        if (boardClassSelect.getValue() == null) {
            Notification.show("Choose a board class");
            return;
        }
        if (subjectSelect.getValue() == null) {
            Notification.show("Choose a subject");
            return;
        }
        try {
            Map<String, Object> payload = Map.of(
                    "boardCode", AdminUi.str(boardClassSelect.getValue(), "boardCode"),
                    "className", AdminUi.str(boardClassSelect.getValue(), "displayName"),
                    "subject", AdminUi.str(subjectSelect.getValue(), "code"));
            api.create("offerings", payload);
            Notification.show("Offering created");
            loadOfferings();
        } catch (Exception ex) {
            Notification.show(ex.getMessage());
        }
    }

    private void clear() {
        subjectSelect.clear();
    }
}
