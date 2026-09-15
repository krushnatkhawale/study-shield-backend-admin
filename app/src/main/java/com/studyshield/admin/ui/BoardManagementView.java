package com.studyshield.admin.ui;

import com.studyshield.admin.service.BackendDataService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
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

    public BoardManagementView(BackendDataService api) {
        this.api = api;
        setSizeFull();
        setPadding(false);

        Button create = AdminUi.primary("New board");
        create.addClickListener(e -> openDialog(null));
        VerticalLayout page = AdminUi.page("Boards", "CBSE, ICSE, state boards, or the shared ALL board. Double-click a row to edit.", create);

        grid.addColumn(r -> AdminUi.str(r, "id")).setHeader("ID").setAutoWidth(true);
        grid.addColumn(r -> AdminUi.str(r, "name")).setHeader("Name").setFlexGrow(1);
        grid.addColumn(r -> AdminUi.str(r, "code")).setHeader("Code");
        grid.addColumn(r -> AdminUi.str(r, "active")).setHeader("Active");
        grid.setSizeFull();
        grid.addItemDoubleClickListener(e -> openDialog(e.getItem()));
        refresh();

        page.add(AdminUi.card(grid));
        page.setFlexGrow(1, page.getComponentAt(1));
        add(page);
        setFlexGrow(1, page);
    }

    private void refresh() {
        grid.setItems(api.list("boards"));
    }

    private void openDialog(Map<String, Object> row) {
        Long id = AdminUi.id(row);
        boolean isNew = id == null;

        TextField name = new TextField("Name");
        name.setRequired(true);
        TextField code = new TextField("Code");
        TextArea description = new TextArea("Description");
        description.setWidthFull();
        Checkbox active = new Checkbox("Active");
        if (row != null) {
            name.setValue(AdminUi.str(row, "name"));
            code.setValue(AdminUi.str(row, "code"));
            description.setValue(AdminUi.str(row, "description"));
            active.setValue(Boolean.parseBoolean(AdminUi.str(row, "active")));
        } else {
            active.setValue(true);
        }

        FormLayout form = new FormLayout(name, code, description, active);
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1, FormLayout.ResponsiveStep.LabelsPosition.TOP),
                new FormLayout.ResponsiveStep("480px", 2, FormLayout.ResponsiveStep.LabelsPosition.TOP));
        form.setColspan(description, 2);
        form.setWidthFull();

        Dialog dialog = new Dialog();
        dialog.setHeaderTitle(isNew ? "New board" : "Edit board");
        dialog.setModal(true);
        dialog.setDraggable(false);
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
            payload.put("code", code.getValue().trim());
            payload.put("description", description.getValue());
            payload.put("active", active.getValue());
            try {
                if (!isNew) {
                    api.update("boards", id, payload);
                    Notification.show("Board updated");
                } else {
                    api.create("boards", payload);
                    Notification.show("Board created");
                }
                dialog.close();
                refresh();
            } catch (Exception ex) {
                Notification.show(ex.getMessage());
            }
        });
        Button delete = AdminUi.danger("Delete");
        delete.setVisible(!isNew);
        delete.addClickListener(e -> AdminUi.confirmDelete("Delete board?", "Classes on this board may fail to load.", () -> {
            api.delete("boards", id);
            dialog.close();
            refresh();
        }));
        Button cancel = new Button("Cancel", e -> dialog.close());
        cancel.addClickShortcut(com.vaadin.flow.component.Key.ESCAPE);

        HorizontalLayout footer = new HorizontalLayout(delete, save, cancel);
        footer.setWidthFull();
        footer.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
        footer.setAlignItems(FlexComponent.Alignment.CENTER);
        dialog.getFooter().add(footer);

        dialog.open();
        name.focus();
    }
}
