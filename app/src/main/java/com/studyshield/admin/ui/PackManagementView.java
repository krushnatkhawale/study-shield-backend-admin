package com.studyshield.admin.ui;

import com.studyshield.admin.service.BackendDataService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
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
    private final ComboBox<Map<String, Object>> subjectSelect = new ComboBox<>("Subject");
    private final Grid<Map<String, Object>> grid = new Grid<>();
    private final TextField name = new TextField("Pack name");
    private final TextArea description = new TextArea("Description");
    private final ComboBox<String> packType = new ComboBox<>("Pack type");
    private final IntegerField version = new IntegerField("Version");
    private final Checkbox active = new Checkbox("Enabled (kids can receive this pack)");
    private final DatePicker validFrom = new DatePicker("Valid from");
    private final DatePicker validTo = new DatePicker("Valid to");
    private Map<String, Object> selected;

    public PackManagementView(BackendDataService api) {
        this.api = api;
        setSizeFull();
        setPadding(false);

        Button create = AdminUi.primary("New pack");
        create.addClickListener(e -> clear());
        VerticalLayout page = AdminUi.page("Content packs",
                "Freemium packs are what the kid app downloads. Disable a pack to hide it without deleting questions.",
                create);

        subjectSelect.setItemLabelGenerator(item -> AdminUi.label(item, "name"));
        subjectSelect.setItems(api.list("subjects"));
        subjectSelect.setWidth("320px");
        subjectSelect.addValueChangeListener(e -> refresh());

        packType.setItems(TYPES);
        packType.setValue("FREEMIUM");
        version.setValue(1);
        active.setValue(true);

        grid.addColumn(r -> AdminUi.str(r, "id")).setHeader("ID").setAutoWidth(true);
        grid.addColumn(r -> AdminUi.str(r, "name")).setHeader("Name").setFlexGrow(1);
        grid.addColumn(r -> AdminUi.str(r, "packType")).setHeader("Type");
        grid.addColumn(r -> AdminUi.str(r, "active")).setHeader("Enabled");
        grid.addColumn(r -> AdminUi.str(r, "validFrom")).setHeader("From");
        grid.addColumn(r -> AdminUi.str(r, "validTo")).setHeader("To");
        grid.setSizeFull();
        grid.asSingleSelect().addValueChangeListener(e -> edit(e.getValue()));

        FormLayout form = new FormLayout(subjectSelect, name, packType, version, validFrom, validTo, description, active);
        Button save = AdminUi.primary("Save pack");
        save.addClickListener(e -> save());
        Button disable = new Button("Disable", e -> setActive(false));
        Button enable = new Button("Enable", e -> setActive(true));
        Button delete = AdminUi.danger("Delete");
        delete.addClickListener(e -> {
            if (AdminUi.id(selected) == null) {
                Notification.show("Select a pack");
                return;
            }
            AdminUi.confirmDelete("Delete pack?", "Quizzes inside the pack will be deleted with it.", () -> {
                api.delete("content-packs", AdminUi.id(selected));
                clear();
                refresh();
            });
        });

        page.add(AdminUi.card(grid), AdminUi.card(form, new HorizontalLayout(save, enable, disable, delete)));
        add(page);
    }

    private void refresh() {
        if (AdminUi.id(subjectSelect.getValue()) == null) {
            grid.setItems(List.of());
            return;
        }
        grid.setItems(api.listBy("content-packs", "subject", AdminUi.id(subjectSelect.getValue())));
    }

    private void edit(Map<String, Object> row) {
        selected = row;
        if (row == null) {
            return;
        }
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
    }

    private void clear() {
        selected = null;
        name.clear();
        description.clear();
        packType.setValue("FREEMIUM");
        version.setValue(1);
        active.setValue(true);
        validFrom.clear();
        validTo.clear();
        grid.deselectAll();
    }

    private void setActive(boolean value) {
        if (AdminUi.id(selected) == null) {
            Notification.show("Select a pack");
            return;
        }
        active.setValue(value);
        save();
    }

    private void save() {
        if (AdminUi.id(subjectSelect.getValue()) == null) {
            Notification.show("Choose a subject");
            return;
        }
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("name", name.getValue());
        payload.put("description", description.getValue());
        payload.put("subjectId", AdminUi.id(subjectSelect.getValue()));
        payload.put("version", version.getValue() == null ? 1 : version.getValue());
        payload.put("active", active.getValue());
        payload.put("packType", packType.getValue());
        payload.put("validFrom", validFrom.getValue());
        payload.put("validTo", validTo.getValue());
        try {
            if (AdminUi.id(selected) != null) {
                api.update("content-packs", AdminUi.id(selected), payload);
                Notification.show("Pack saved");
            } else {
                api.create("content-packs", payload);
                Notification.show("Pack created");
            }
            refresh();
        } catch (Exception ex) {
            Notification.show(ex.getMessage());
        }
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
