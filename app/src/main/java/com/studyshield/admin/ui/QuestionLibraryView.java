package com.studyshield.admin.ui;

import com.studyshield.admin.service.BackendDataService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;
import jakarta.annotation.security.RolesAllowed;

import java.util.List;
import java.util.Map;

@Route(value = "questions", layout = MainLayout.class)
@RouteAlias(value = "question-bank", layout = MainLayout.class)
@PageTitle("Question bank")
@RolesAllowed("ADMIN")
public class QuestionLibraryView extends VerticalLayout {

    private final BackendDataService api;
    private final ComboBox<Map<String, Object>> boardSelect = new ComboBox<>("Board");
    private final ComboBox<Map<String, Object>> boardClassSelect = new ComboBox<>("Board class");
    private final ComboBox<Map<String, Object>> offeringSelect = new ComboBox<>("Offering");
    private final Grid<Map<String, Object>> grid = new Grid<>();

    public QuestionLibraryView(BackendDataService api) {
        this.api = api;
        setSizeFull();
        setPadding(false);

        Button create = AdminUi.primary("New question");
        create.addClickListener(e -> openEditor(null));
        VerticalLayout page = AdminUi.page("Question bank",
                "Edits create a new version. Quizzes always fetch the latest version of a question. Double-click a row to edit.",
                create);

        boardSelect.setItemLabelGenerator(item -> AdminUi.label(item, "name", "code"));
        boardSelect.setItems(api.list("boards"));
        boardSelect.setWidth("220px");
        boardSelect.addValueChangeListener(e -> loadBoardClasses());

        boardClassSelect.setItemLabelGenerator(item -> AdminUi.label(item, "displayName") + " (ord " + AdminUi.str(item, "ordinal") + ")");
        boardClassSelect.setWidth("300px");
        boardClassSelect.addValueChangeListener(e -> loadOfferings());

        offeringSelect.setItemLabelGenerator(item -> AdminUi.str(item, "subjectCode") + " - " + AdminUi.str(item, "className"));
        offeringSelect.setWidth("300px");
        offeringSelect.addValueChangeListener(e -> refresh());

        grid.addColumn(r -> AdminUi.str(r, "id")).setHeader("ID").setAutoWidth(true);
        grid.addColumn(r -> AdminUi.str(r, "questionText")).setHeader("Question").setFlexGrow(1);
        grid.addColumn(r -> AdminUi.str(r, "questionType")).setHeader("Type");
        grid.addColumn(r -> "v" + AdminUi.str(r, "version")).setHeader("Version").setWidth("90px");
        grid.addColumn(r -> AdminUi.str(r, "quizId")).setHeader("Quiz");
        grid.setSizeFull();
        grid.addItemDoubleClickListener(e -> openEditor(e.getItem()));

        HorizontalLayout selectors = new HorizontalLayout(boardSelect, boardClassSelect, offeringSelect);
        selectors.setAlignItems(Alignment.END);
        page.add(selectors, AdminUi.card(grid));
        page.setFlexGrow(1, page.getComponentAt(1));
        add(page);
        setFlexGrow(1, page);
        refresh();
    }

    private void openEditor(Map<String, Object> question) {
        Long quizId = question != null ? parseLong(question.get("quizId")) : null;
        if (quizId == null) {
            Long offeringId = AdminUi.id(offeringSelect.getValue());
            if (offeringId == null) {
                Notification.show("Pick an offering first so the question can live in that offering's bank");
                return;
            }
            Map<String, Object> library = api.postPath("quizzes/library/" + offeringId, Map.of());
            quizId = AdminUi.id(library);
        }
        new QuestionEditorDialog(api, quizId, question, this::refresh).open();
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
        Long subjectId = getOfferingSubjectId();
        if (subjectId == null) {
            grid.setItems(api.list("questions"));
            return;
        }
        grid.setItems(api.listByPath("questions", "subject", subjectId));
    }

    /** Read the subjectId from the currently selected offering row. */
    private Long getOfferingSubjectId() {
        Map<String, Object> offering = offeringSelect.getValue();
        if (offering == null) return null;
        Object val = offering.get("subjectId");
        if (val instanceof Number n) return n.longValue();
        try { return Long.parseLong(String.valueOf(val)); } catch (Exception e) { return null; }
    }

    private static Long parseLong(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return Long.valueOf(String.valueOf(value));
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
