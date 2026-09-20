package com.studyshield.admin.ui;

import com.studyshield.admin.service.BackendDataService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dialog.Dialog;
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
import jakarta.annotation.security.RolesAllowed;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Route(value = "packs", layout = MainLayout.class)
@PageTitle("Packs")
@RolesAllowed("ADMIN")
public class PackManagementView extends VerticalLayout {

    private static final List<String> TYPES = List.of(
            "FREEMIUM", "PREMIUM", "LIBRARY", "PROMOTIONAL", "SEASONAL", "COMPLEMENTARY");

    private final BackendDataService api;
    private final ComboBox<Map<String, Object>> boardSelect = new ComboBox<>("Board");
    private final ComboBox<Map<String, Object>> boardClassSelect = new ComboBox<>("Board class");
    private final ComboBox<Map<String, Object>> offeringSelect = new ComboBox<>("Offering");
    private final Grid<Map<String, Object>> grid = new Grid<>();

    public PackManagementView(BackendDataService api) {
        this.api = api;
        setSizeFull();
        setPadding(false);

        Button create = AdminUi.primary("New pack");
        create.addClickListener(e -> openDialog(null));
        VerticalLayout page = AdminUi.page("Content packs",
                "Freemium packs are what the kid app downloads. Each pack belongs to an offering (board class + subject). Double-click a row to edit.",
                create);

        boardSelect.setItemLabelGenerator(item -> AdminUi.label(item, "name", "code"));
        boardSelect.setItems(api.list("boards"));
        boardSelect.setWidth("220px");
        boardSelect.addValueChangeListener(e -> loadBoardClasses());

        boardClassSelect.setItemLabelGenerator(item -> AdminUi.label(item, "displayName") + " (ord " + AdminUi.str(item, "ordinal") + ")");
        boardClassSelect.setWidth("300px");
        boardClassSelect.addValueChangeListener(e -> loadOfferings());

        offeringSelect.setItemLabelGenerator(item -> AdminUi.str(item, "subjectCode") + " - " + AdminUi.str(item, "className"));
        offeringSelect.setWidth("320px");
        offeringSelect.addValueChangeListener(e -> refresh());

        grid.addColumn(r -> AdminUi.str(r, "id")).setHeader("ID").setAutoWidth(true);
        grid.addColumn(r -> AdminUi.str(r, "name")).setHeader("Name").setFlexGrow(1);
        grid.addColumn(r -> AdminUi.str(r, "packType")).setHeader("Type");
        grid.addColumn(r -> AdminUi.str(r, "active")).setHeader("Enabled");
        grid.addColumn(r -> AdminUi.str(r, "validFrom")).setHeader("From");
        grid.addColumn(r -> AdminUi.str(r, "validTo")).setHeader("To");
        grid.setSizeFull();
        grid.setMinHeight("320px");
        grid.addItemDoubleClickListener(e -> openDialog(e.getItem()));

        HorizontalLayout selectors = new HorizontalLayout(boardSelect, boardClassSelect, offeringSelect);
        selectors.setAlignItems(Alignment.END);
        com.vaadin.flow.component.Component card = AdminUi.card(grid);
        page.add(selectors, card);
        page.setFlexGrow(1, card);
        add(page);
        setFlexGrow(1, page);
        refresh();
    }

    private void loadBoardClasses() {
        Long boardId = AdminUi.id(boardSelect.getValue());
        boardClassSelect.setItems(boardId == null ? List.of() : api.listBy("board-classes", "board", boardId));
        offeringSelect.setItems(List.of());
        refresh();
    }

    private void loadOfferings() {
        Long boardClassId = AdminUi.id(boardClassSelect.getValue());
        offeringSelect.setItems(boardClassId == null ? List.of() : api.listBy("offerings", "board-class", boardClassId));
        refresh();
    }

    private void refresh() {
        Long offeringId = AdminUi.id(offeringSelect.getValue());
        if (offeringId == null) {
            grid.setItems(api.list("content-packs"));
            return;
        }
        grid.setItems(api.listBy("content-packs", "offering", offeringId));
    }

    private void openDialog(Map<String, Object> row) {
        Long id = AdminUi.id(row);
        boolean isNew = id == null;

        TextField name = new TextField("Pack name");
        name.setRequired(true);
        name.setWidthFull();
        TextArea description = new TextArea("Description");
        description.setWidthFull();
        ComboBox<String> packType = new ComboBox<>("Pack type");
        packType.setItems(TYPES);
        packType.setWidthFull();
        IntegerField version = new IntegerField("Version");
        version.setWidthFull();
        Checkbox active = new Checkbox("Enabled (kids can receive this pack)");
        DatePicker validFrom = new DatePicker("Valid from");
        validFrom.setWidthFull();
        DatePicker validTo = new DatePicker("Valid to");
        validTo.setWidthFull();
        if (row != null) {
            name.setValue(AdminUi.str(row, "name"));
            description.setValue(AdminUi.str(row, "description"));
            String type = AdminUi.str(row, "packType");
            packType.setValue(TYPES.contains(type) ? type : "FREEMIUM");
            try {
                version.setValue(Integer.parseInt(AdminUi.str(row, "version")));
            } catch (NumberFormatException ex) {
                version.setValue(1);
            }
            active.setValue(Boolean.parseBoolean(AdminUi.str(row, "active")));
            validFrom.setValue(parseDate(AdminUi.str(row, "validFrom")));
            validTo.setValue(parseDate(AdminUi.str(row, "validTo")));
        } else {
            packType.setValue("FREEMIUM");
            version.setValue(1);
            active.setValue(true);
        }

        FormLayout form = AdminUi.entityForm(name, packType, version, validFrom, validTo, description, active);
        form.setColspan(description, 2);

        Dialog dialog = AdminUi.entityDialog(isNew ? "New pack" : "Edit pack", form);

        Button save = AdminUi.primary("Save");
        save.addClickListener(e -> {
            Long offeringId = AdminUi.id(offeringSelect.getValue());
            if (offeringId == null) {
                Notification.show("Choose an offering first");
                return;
            }
            if (name.getValue().isBlank()) {
                Notification.show("Name is required");
                name.focus();
                return;
            }
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("name", name.getValue().trim());
            payload.put("description", description.getValue());
            payload.put("offeringId", offeringId);
            payload.put("version", version.getValue() == null ? 1 : version.getValue());
            payload.put("active", active.getValue());
            payload.put("packType", packType.getValue());
            payload.put("validFrom", validFrom.getValue());
            payload.put("validTo", validTo.getValue());
            try {
                if (!isNew) {
                    api.update("content-packs", id, payload);
                    Notification.show("Pack updated");
                } else {
                    api.create("content-packs", payload);
                    Notification.show("Pack created");
                }
                dialog.close();
                refresh();
            } catch (Exception ex) {
                Notification.show(ex.getMessage());
            }
        });
        Button delete = AdminUi.danger("Delete");
        delete.setVisible(!isNew);
        delete.addClickListener(e -> AdminUi.confirmDelete("Delete pack?", "Quizzes inside the pack will be deleted with it.", () -> {
            api.delete("content-packs", id);
            dialog.close();
            refresh();
        }));
        AdminUi.dialogFooter(dialog, delete, save);

        dialog.open();
        name.focus();
    }

    private static LocalDate parseDate(String raw) {
        if (raw == null || raw.isBlank() || "null".equals(raw)) {
            return null;
        }
        try {
            return LocalDate.parse(raw.substring(0, Math.min(10, raw.length())));
        } catch (Exception ex) {
            return null;
        }
    }
}
