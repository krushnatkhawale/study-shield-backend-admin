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
    private final ComboBox<Map<String, Object>> classSelect = new ComboBox<>("Class");
    private final ComboBox<Map<String, Object>> subjectSelect = new ComboBox<>("Subject");
    private final Grid<Map<String, Object>> grid = new Grid<>();

    public QuestionLibraryView(BackendDataService api) {
        this.api = api;
        setSizeFull();
        setPadding(false);

        Button create = AdminUi.primary("New question");
        create.addClickListener(e -> openEditor(null));
        VerticalLayout page = AdminUi.page("Question bank",
                "Edits create a new version. Quizzes always fetch the latest version of a question.",
                create);

        classSelect.setItemLabelGenerator(item -> AdminUi.label(item, "name"));
        classSelect.setItems(api.list("class-grades"));
        classSelect.setWidth("240px");
        classSelect.addValueChangeListener(e -> {
            Long classId = AdminUi.id(e.getValue());
            subjectSelect.setItems(classId == null ? List.of() : api.listBy("subjects", "class-grade", classId));
            refresh();
        });
        subjectSelect.setItemLabelGenerator(item -> AdminUi.label(item, "name"));
        subjectSelect.setWidth("240px");
        subjectSelect.addValueChangeListener(e -> refresh());

        grid.addColumn(r -> AdminUi.str(r, "id")).setHeader("ID").setAutoWidth(true);
        grid.addColumn(r -> AdminUi.str(r, "questionText")).setHeader("Question").setFlexGrow(1);
        grid.addColumn(r -> AdminUi.str(r, "questionType")).setHeader("Type");
        grid.addColumn(r -> "v" + AdminUi.str(r, "version")).setHeader("Version").setWidth("90px");
        grid.addColumn(r -> AdminUi.str(r, "quizId")).setHeader("Quiz");
        grid.addComponentColumn(this::actions).setHeader("").setAutoWidth(true);
        grid.setSizeFull();

        page.add(new HorizontalLayout(classSelect, subjectSelect), AdminUi.card(grid));
        add(page);
    }

    private HorizontalLayout actions(Map<String, Object> question) {
        Button edit = new Button("Edit", e -> openEditor(question));
        Button delete = AdminUi.danger("Delete");
        delete.addClickListener(e -> AdminUi.confirmDelete(
                "Delete question?",
                "All versions of this question will be removed.",
                () -> {
                    api.delete("questions", AdminUi.id(question));
                    refresh();
                }));
        return new HorizontalLayout(edit, delete);
    }

    private void openEditor(Map<String, Object> question) {
        Long quizId = question != null ? parseLong(question.get("quizId")) : null;
        if (quizId == null) {
            Long subjectId = AdminUi.id(subjectSelect.getValue());
            if (subjectId == null) {
                Notification.show("Pick a subject first so the question can live in that subject's bank");
                return;
            }
            Map<String, Object> library = api.postPath("quizzes/library/" + subjectId, Map.of());
            quizId = AdminUi.id(library);
        }
        new QuestionEditorDialog(api, quizId, question, this::refresh).open();
    }

    private void refresh() {
        Long subjectId = AdminUi.id(subjectSelect.getValue());
        if (subjectId == null) {
            grid.setItems(List.of());
            return;
        }
        grid.setItems(api.listByPath("questions", "subject", subjectId));
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
