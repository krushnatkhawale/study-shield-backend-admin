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
        grid.setMinHeight("320px");
        grid.addItemDoubleClickListener(e -> openDialog(e.getItem()));

        com.vaadin.flow.component.Component card = AdminUi.card(grid);
        page.add(boardFilter, card);
        page.setFlexGrow(1, card);
        add(page);
        setFlexGrow(1, page);
        refresh();
    }

    private void refresh() {
        Long boardId = AdminUi.id(boardFilter.getValue());
        grid.setItems(boardId == null ? api.list("board-classes") : api.listBy("board-classes", "board", boardId));
    }

    private void openDialog(Map<String, Object> row) {
        Long id = AdminUi.id(row);
        boolean isNew = id == null;

        ComboBox<Map<String, Object>> boardSelect = new ComboBox<>("Board");
        boardSelect.setItems(api.list("boards"));
        boardSelect.setItemLabelGenerator(item -> AdminUi.label(item, "name", "code"));
        boardSelect.setWidthFull();
        List<Map<String, Object>> levels = api.list("class-levels");
        ComboBox<Map<String, Object>> levelSelect = new ComboBox<>("Class level");
        levelSelect.setItems(levels);
        levelSelect.setItemLabelGenerator(BoardClassManagementView::levelLabel);
        levelSelect.setWidthFull();
        TextField displayName = new TextField("Display name");
        displayName.setWidthFull();
        // Auto-suggest displayName from board + level; never overwrite user edits.
        boardSelect.addValueChangeListener(e -> suggestDisplayName(boardSelect, levelSelect, displayName));
        levelSelect.addValueChangeListener(e -> suggestDisplayName(boardSelect, levelSelect, displayName));
        if (row != null) {
            // Preselect the board matching the row's boardCode, if present.
            String boardCode = AdminUi.str(row, "boardCode");
            boardSelect.getListDataView().getItems()
                    .filter(b -> boardCode.equalsIgnoreCase(AdminUi.str(b, "code")))
                    .findFirst().ifPresent(boardSelect::setValue);
            // Preselect the level matching classLevelId, falling back to ordinal.
            Object levelId = row.get("classLevelId");
            boolean matched = false;
            if (levelId != null) {
                String want = String.valueOf(levelId);
                matched = levelSelect.getListDataView().getItems()
                        .filter(l -> want.equals(String.valueOf(l.get("id"))))
                        .findFirst().map(l -> { levelSelect.setValue(l); return true; }).orElse(false);
            }
            if (!matched) {
                String ord = AdminUi.str(row, "ordinal");
                levelSelect.getListDataView().getItems()
                        .filter(l -> ord.equals(AdminUi.str(l, "ordinal")))
                        .findFirst().ifPresent(levelSelect::setValue);
            }
            displayName.setValue(AdminUi.str(row, "displayName"));
        } else if (boardFilter.getValue() != null) {
            boardSelect.setValue(boardFilter.getValue());
        }

        FormLayout form = AdminUi.entityForm(boardSelect, levelSelect, displayName);
        form.setColspan(displayName, 2);

        Dialog dialog = AdminUi.entityDialog(isNew ? "New board class" : "Edit board class", form);

        Button save = AdminUi.primary("Save");
        save.addClickListener(e -> {
            if (boardSelect.getValue() == null) {
                Notification.show("Choose a board");
                boardSelect.focus();
                return;
            }
            if (levelSelect.getValue() == null) {
                Notification.show("Class level is required");
                levelSelect.focus();
                return;
            }
            if (displayName.getValue().isBlank()) {
                Notification.show("Display name is required");
                displayName.focus();
                return;
            }
            try {
                Long classLevelId = AdminUi.id(levelSelect.getValue());
                Map<String, Object> payload = Map.of(
                        "boardId", AdminUi.id(boardSelect.getValue()),
                        "classLevelId", classLevelId,
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

    private static String levelLabel(Map<String, Object> level) {
        String ordinal = AdminUi.str(level, "ordinal");
        String name = AdminUi.str(level, "canonicalName");
        if (name.isBlank()) name = AdminUi.str(level, "canonical_name");
        String min = AdminUi.str(level, "ageMinYears");
        if (min.isBlank()) min = AdminUi.str(level, "age_min_years");
        String max = AdminUi.str(level, "ageMaxYears");
        if (max.isBlank()) max = AdminUi.str(level, "age_max_years");
        String stage = AdminUi.str(level, "stage");
        StringBuilder label = new StringBuilder("Level ").append(ordinal.isBlank() ? "?" : ordinal);
        if (!name.isBlank()) label.append(" — ").append(name);
        if (!min.isBlank() || !max.isBlank()) {
            label.append(" (ages ").append(min.isBlank() ? "?" : min)
                    .append("–").append(max.isBlank() ? "?" : max).append(")");
        } else if (!stage.isBlank()) {
            label.append(" (").append(stage).append(")");
        } else if (!name.isBlank()) {
            // name already shown; keep short
        }
        if (!stage.isBlank() && (min.isBlank() && max.isBlank()) == false) label.append(" ").append(stage);
        else if (!stage.isBlank() && name.isBlank()) label.append(" — ").append(stage);
        return label.toString().replace("() ", "").trim();
    }

    private static void suggestDisplayName(ComboBox<Map<String, Object>> boardSelect,
            ComboBox<Map<String, Object>> levelSelect, TextField displayName) {
        if (displayName == null || !displayName.getValue().isBlank()) return;
        if (boardSelect.getValue() == null || levelSelect.getValue() == null) return;
        int ordinal;
        try {
            ordinal = Integer.parseInt(AdminUi.str(levelSelect.getValue(), "ordinal"));
        } catch (NumberFormatException ex) {
            return;
        }
        String code = AdminUi.str(boardSelect.getValue(), "code").toUpperCase();
        displayName.setValue(suggestName(code, ordinal));
    }

    static String suggestName(String boardCode, int ordinal) {
        return switch (boardCode) {
            case "ENG" -> switch (ordinal) {
                case 2, 3 -> "Nursery";
                case 4 -> "Reception";
                default -> (ordinal >= 5 && ordinal <= 16) ? "Year " + (ordinal - 4) : "";
            };
            case "US" -> switch (ordinal) {
                case 2, 3 -> "Preschool";
                case 4 -> "Kindergarten";
                default -> (ordinal >= 5 && ordinal <= 16) ? "Grade " + (ordinal - 4) : "";
            };
            default -> switch (ordinal) { // ALL, CBSE, MH, ICSE and others (India-style)
                case 1 -> "Playgroup";
                case 2 -> "Nursery";
                case 3 -> "Junior KG";
                case 4 -> "Senior KG";
                default -> "Class " + (ordinal - 4);
            };
        };
    }
}
