package com.studyshield.admin.ui;

import com.studyshield.admin.service.BackendDataService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Manage the questions of a quiz. Questions are listed in a table with per-row Edit / Delete
 * buttons. Edit opens the question in a popup; saving an edit creates a new version of the
 * question, and the latest version is always the one a quiz serves. The popup also lists every
 * revision of the question for inspection or restore.
 */
@Route(value = "questions", layout = MainLayout.class)
@PageTitle("Question management")
@RolesAllowed("ADMIN")
public class QuestionManagementView extends VerticalLayout {

    private final BackendDataService backendDataService;

    private final ComboBox<Map<String, Object>> subjectSelect = new ComboBox<>();
    private final ComboBox<Map<String, Object>> quizSelect = new ComboBox<>();
    private final Grid<Map<String, Object>> grid = new Grid<>();

    private final Button newButton = new Button("New question");

    private List<Map<String, Object>> quizList = new ArrayList<>();

    public QuestionManagementView(BackendDataService backendDataService) {
        this.backendDataService = backendDataService;

        setPadding(true);
        setSpacing(true);
        setWidthFull();
        add(new H2("Question management"));

        subjectSelect.setLabel("Subject");
        subjectSelect.setItemLabelGenerator(item -> String.valueOf(item.getOrDefault("name", "?")));
        subjectSelect.setItems(backendDataService.list("subjects"));
        subjectSelect.addValueChangeListener(e -> loadQuizzes(e.getValue()));

        quizSelect.setLabel("Quiz");
        quizSelect.setItemLabelGenerator(item -> String.valueOf(item.getOrDefault("title", "?")));
        quizSelect.addValueChangeListener(e -> loadQuestions(e.getValue()));

        grid.addColumn(item -> item.getOrDefault("id", "-")).setHeader("ID").setWidth("70px");
        grid.addColumn(item -> item.getOrDefault("questionText", "-")).setHeader("Question");
        grid.addColumn(item -> item.getOrDefault("questionType", "-")).setHeader("Type");
        grid.addColumn(item -> item.getOrDefault("difficulty", "-")).setHeader("Difficulty");
        grid.addColumn(item -> "v" + item.getOrDefault("version", "1")).setHeader("Version").setWidth("90px");
        grid.addComponentColumn(this::actionsFor).setHeader("Actions").setWidth("180px");
        grid.setHeight("400px");

        newButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        newButton.addClickListener(e -> openEditor(null));

        HorizontalLayout toolbar = new HorizontalLayout(subjectSelect, quizSelect, newButton);
        toolbar.setVerticalComponentAlignment(Alignment.END, newButton);

        add(toolbar, grid);
    }

    private HorizontalLayout actionsFor(Map<String, Object> question) {
        Button edit = new Button("Edit");
        edit.addClickListener(e -> openEditor(question));

        Button delete = new Button("Delete");
        delete.addThemeVariants(ButtonVariant.LUMO_ERROR);
        delete.addClickListener(e -> confirmDelete(question));

        HorizontalLayout actions = new HorizontalLayout(edit, delete);
        actions.setPadding(false);
        return actions;
    }

    private void loadQuizzes(Map<String, Object> subject) {
        quizList.clear();
        quizSelect.clear();
        grid.setItems(List.of());
        if (subject == null || subject.get("id") == null) {
            return;
        }
        Long subjectId = Long.valueOf(String.valueOf(subject.get("id")));
        List<Map<String, Object>> packs = backendDataService.listBy("content-packs", "subject", subjectId);
        for (Map<String, Object> pack : packs) {
            Object packId = pack.get("id");
            if (packId != null) {
                quizList.addAll(backendDataService.listByPath("quizzes", "content-pack",
                        Long.valueOf(String.valueOf(packId))));
            }
        }
        quizSelect.setItems(quizList);
    }

    private void loadQuestions(Map<String, Object> quiz) {
        grid.setItems(List.of());
        if (quiz == null || quiz.get("id") == null) {
            return;
        }
        Long quizId = Long.valueOf(String.valueOf(quiz.get("id")));
        grid.setItems(backendDataService.listByPath("questions", "quiz", quizId));
    }

    private void openEditor(Map<String, Object> question) {
        Map<String, Object> quiz = quizSelect.getValue();
        if (quiz == null || quiz.get("id") == null) {
            Notification.show("Choose a quiz first");
            return;
        }
        Long quizId = Long.valueOf(String.valueOf(quiz.get("id")));
        new QuestionEditorDialog(backendDataService, quizId, question, () -> loadQuestions(quiz)).open();
    }

    private void confirmDelete(Map<String, Object> question) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Delete question?");
        dialog.setModal(true);

        Button delete = new Button("Delete question and all its versions");
        delete.addThemeVariants(ButtonVariant.LUMO_ERROR);
        delete.addClickListener(e -> {
            dialog.close();
            deleteQuestion(question);
        });
        Button cancel = new Button("Cancel", e -> dialog.close());

        HorizontalLayout actions = new HorizontalLayout();
        actions.setWidthFull();
        actions.setJustifyContentMode(JustifyContentMode.END);
        actions.add(delete, cancel);

        dialog.add(actions);
        dialog.open();
    }

    private void deleteQuestion(Map<String, Object> question) {
        try {
            Long id = Long.valueOf(String.valueOf(question.get("id")));
            backendDataService.delete("questions", id);
            Notification.show("Question deleted");
            loadQuestions(quizSelect.getValue());
        } catch (Exception ex) {
            Notification.show("Delete failed: " + ex.getMessage());
        }
    }
}