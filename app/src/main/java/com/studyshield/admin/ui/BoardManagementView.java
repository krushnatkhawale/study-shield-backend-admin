package com.studyshield.admin.ui;

import com.studyshield.admin.service.BackendDataService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;

import java.util.LinkedHashMap;
import java.util.Map;

@Route(value = "boards", layout = MainLayout.class)
@PageTitle("Boards")
@RolesAllowed("ADMIN")
public class BoardManagementView extends VerticalLayout {

    private final BackendDataService api;
    private final Grid<Map<String, Object>> grid = new Grid<>();
    private final TextField name = new TextField("Name");
    private final TextField code = new TextField("Code");
    private final TextArea description = new TextArea("Description");
    private final Checkbox active = new Checkbox("Active");
    private Map<String, Object> selected;

    public BoardManagementView(BackendDataService api) {
        this.api = api;
        setSizeFull();
        setPadding(false);

        Button create = AdminUi.primary("New board");
        create.addClickListener(e -> clear());
        VerticalLayout page = AdminUi.page("Boards", "CBSE, ICSE, state boards, or the shared ALL board.", create);

        grid.addColumn(r -> AdminUi.str(r, "id")).setHeader("ID").setAutoWidth(true);
        grid.addColumn(r -> AdminUi.str(r, "name")).setHeader("Name").setFlexGrow(1);
        grid.addColumn(r -> AdminUi.str(r, "code")).setHeader("Code");
        grid.addColumn(r -> AdminUi.str(r, "active")).setHeader("Active");
        grid.setSizeFull();
        grid.asSingleSelect().addValueChangeListener(e -> edit(e.getValue()));
        refresh();

        FormLayout form = new FormLayout(name, code, description, active);
        Button save = AdminUi.primary("Save");
        save.addClickListener(e -> save());
        Button delete = AdminUi.danger("Delete");
        delete.addClickListener(e -> {
            if (AdminUi.id(selected) == null) {
                Notification.show("Select a board");
                return;
            }
            AdminUi.confirmDelete("Delete board?", "Classes on this board may fail to load.", () -> {
                api.delete("boards", AdminUi.id(selected));
                clear();
                refresh();
            });
        });

        page.add(AdminUi.card(grid), AdminUi.card(form, new HorizontalLayout(save, delete)));
        add(page);
        setFlexGrow(1, page);
    }

    private void refresh() {
        grid.setItems(api.list("boards"));
    }

    private void edit(Map<String, Object> row) {
        selected = row;
        if (row == null) {
            return;
        }
        name.setValue(AdminUi.str(row, "name"));
        code.setValue(AdminUi.str(row, "code"));
        description.setValue(AdminUi.str(row, "description"));
        active.setValue(Boolean.parseBoolean(AdminUi.str(row, "active")));
    }

    private void clear() {
        selected = null;
        name.clear();
        code.clear();
        description.clear();
        active.setValue(true);
        grid.deselectAll();
    }

    private void save() {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("name", name.getValue());
        payload.put("code", code.getValue());
        payload.put("description", description.getValue());
        payload.put("active", active.getValue());
        try {
            if (AdminUi.id(selected) != null) {
                api.update("boards", AdminUi.id(selected), payload);
                Notification.show("Board updated");
            } else {
                api.create("boards", payload);
                Notification.show("Board created");
            }
            clear();
            refresh();
        } catch (Exception ex) {
            Notification.show(ex.getMessage());
        }
    }
}
