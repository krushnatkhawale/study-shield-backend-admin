package com.studyshield.admin.ui;

import com.studyshield.admin.service.BackendDataService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * List, create and edit quizzes. A subject is chosen first; the active content pack for that
 * subject is loaded; quizzes under the pack are listed and editable. New quizzes are created
 * FREEMIUM tier by default so they join the class bundle.
 */
@Route(value = "quizzes", layout = MainLayout.class)
@PageTitle("Quiz management")
@RolesAllowed("ADMIN")
public class QuizManagementView extends VerticalLayout {

    private static final List<String> QUIZ_TYPES = List.of("STANDARD", "RAPID_FIRE", "ASSESSMENT");
    private static final List<String> CONTENT_TIERS = List.of("FREEMIUM", "PREMIUM");

    private final BackendDataService backendDataService;

    private final ComboBox<Map<String, Object>> subjectSelect = new ComboBox<>();
    private final Grid<Map<String, Object>> grid = new Grid<>();
    private final FormLayout editor = new FormLayout();

    private final TextField titleField = new TextField("Title");
    private final TextArea descriptionField = new TextArea("Description");
    private final ComboBox<String> quizTypeField = new ComboBox<>("Quiz type");
    private final IntegerField questionCountField = new IntegerField("Question count");
    private final ComboBox<String> contentTierField = new ComboBox<>("Content tier");
    private final IntegerField freemiumIndexField = new IntegerField("Freemium index");
    private final TextField languageField = new TextField("Language");
    private final Checkbox activeField = new Checkbox("Active");

    private final Button saveButton = new Button("Save quiz");
    private final Button newButton = new Button("New quiz");

    private final Grid<Map<String, Object>> questionGrid = new Grid<>();
    private final Button addQuestionButton = new Button("Add question");

    private Map<String, Object> selected;

    public QuizManagementView(BackendDataService backendDataService) {
        this.backendDataService = backendDataService;

        setPadding(true);
        setSpacing(true);
        setWidthFull();
        add(new H2("Quiz management"));

        subjectSelect.setLabel("Subject");
        subjectSelect.setItemLabelGenerator(item -> String.valueOf(item.getOrDefault("name", "?")));
        subjectSelect.setItems(backendDataService.list("subjects"));
        subjectSelect.addValueChangeListener(e -> loadQuizzes(e.getValue()));

        grid.addColumn(item -> item.getOrDefault("id", "-")).setHeader("ID").setWidth("70px");
        grid.addColumn(item -> item.getOrDefault("title", "-")).setHeader("Title");
        grid.addColumn(item -> item.getOrDefault("contentTier", "-")).setHeader("Tier");
        grid.addColumn(item -> item.getOrDefault("questionCount", "-")).setHeader("Qs");
        grid.addColumn(item -> item.getOrDefault("active", "-")).setHeader("Active");
        grid.setSelectionMode(Grid.SelectionMode.SINGLE);
        grid.asSingleSelect().addValueChangeListener(event -> loadIntoEditor(event.getValue()));
        grid.setHeight("300px");

        quizTypeField.setItems(QUIZ_TYPES);
        contentTierField.setItems(CONTENT_TIERS);
        contentTierField.setValue("FREEMIUM");
        languageField.setValue("English");
        questionCountField.setValue(10);
        activeField.setValue(true);

        editor.add(titleField, descriptionField, quizTypeField, questionCountField,
                contentTierField, freemiumIndexField, languageField, activeField);
        editor.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 2));

        newButton.addClickListener(e -> resetEditor(null));
        saveButton.addClickListener(e -> saveQuiz());

        questionGrid.addColumn(item -> item.getOrDefault("id", "-")).setHeader("ID").setWidth("70px");
        questionGrid.addColumn(item -> item.getOrDefault("questionText", "-")).setHeader("Question");
        questionGrid.addColumn(item -> "v" + item.getOrDefault("version", "1")).setHeader("Version").setWidth("90px");
        questionGrid.addComponentColumn(this::questionActions).setHeader("Actions").setWidth("180px");
        questionGrid.setHeight("500px");

        addQuestionButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        addQuestionButton.addClickListener(e -> openAddQuestion());

        HorizontalLayout questionSection = new HorizontalLayout(new H4("Questions in this quiz"), addQuestionButton);
        questionSection.setAlignItems(Alignment.CENTER);

        add(subjectSelect, grid, editor, new HorizontalLayout(newButton, saveButton),
                questionSection, questionGrid);
    }

    private void loadQuizzes(Map<String, Object> subject) {
        if (subject == null || subject.get("id") == null) {
            grid.setItems(List.of());
            return;
        }
        Long subjectId = Long.valueOf(String.valueOf(subject.get("id")));
        List<Map<String, Object>> packs = backendDataService.listBy("content-packs", "subject", subjectId);
        List<Map<String, Object>> quizzes = new ArrayList<>();
        for (Map<String, Object> pack : packs) {
            Object packId = pack.get("id");
            if (packId != null) {
                quizzes.addAll(backendDataService.listByPath("quizzes", "content-pack",
                        Long.valueOf(String.valueOf(packId))));
            }
        }
        grid.setItems(quizzes);
        if (!quizzes.isEmpty()) {
            grid.select(quizzes.getFirst());
        }
    }

    @SuppressWarnings("unchecked")
    private void loadIntoEditor(Map<String, Object> quiz) {
        if (quiz == null || quiz.isEmpty()) {
            return;
        }
        selected = quiz;
        titleField.setValue(String.valueOf(quiz.getOrDefault("title", "")));
        descriptionField.setValue(String.valueOf(quiz.getOrDefault("description", "")));
        quizTypeField.setValue(String.valueOf(quiz.getOrDefault("quizType", "STANDARD")));
        questionCountField.setValue(toInt(quiz.get("questionCount")));
        contentTierField.setValue(String.valueOf(quiz.getOrDefault("contentTier", "FREEMIUM")));
        freemiumIndexField.setValue(toIntObj(quiz.get("freemiumIndex")));
        languageField.setValue(String.valueOf(quiz.getOrDefault("language", "English")));
        activeField.setValue(Boolean.parseBoolean(String.valueOf(quiz.getOrDefault("active", true))));
        loadQuizQuestions(quiz);
    }

    private void loadQuizQuestions(Map<String, Object> quiz) {
        questionGrid.setItems(List.of());
        if (quiz == null || quiz.get("id") == null) {
            return;
        }
        Long quizId = Long.valueOf(String.valueOf(quiz.get("id")));
        questionGrid.setItems(backendDataService.listByPath("questions", "quiz", quizId));
    }

    private HorizontalLayout questionActions(Map<String, Object> question) {
        Button edit = new Button("Edit");
        edit.addClickListener(e -> openEditQuestion(question));

        Button remove = new Button("Remove");
        remove.addThemeVariants(ButtonVariant.LUMO_ERROR);
        remove.addClickListener(e -> confirmRemoveQuestion(question));

        HorizontalLayout actions = new HorizontalLayout(edit, remove);
        actions.setSpacing(true);
        actions.setPadding(false);
        return actions;
    }

    private void openEditQuestion(Map<String, Object> question) {
        if (selected == null || selected.get("id") == null) {
            Notification.show("Select a quiz first to edit a question");
            return;
        }
        Long quizId = Long.valueOf(String.valueOf(selected.get("id")));
        new QuestionEditorDialog(backendDataService, quizId, question,
                () -> loadQuizQuestions(selected)).open();
    }

    private void confirmRemoveQuestion(Map<String, Object> question) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Remove question from quiz?");
        dialog.setModal(true);

        Button remove = new Button("Remove question (deletes all its versions)");
        remove.addThemeVariants(ButtonVariant.LUMO_ERROR);
        remove.addClickListener(e -> {
            dialog.close();
            removeQuestion(question);
        });
        Button cancel = new Button("Cancel", e -> dialog.close());

        HorizontalLayout actions = new HorizontalLayout();
        actions.setWidthFull();
        actions.setJustifyContentMode(JustifyContentMode.END);
        actions.add(remove, cancel);

        dialog.add(actions);
        dialog.open();
    }

    private void removeQuestion(Map<String, Object> question) {
        try {
            Long id = Long.valueOf(String.valueOf(question.get("id")));
            backendDataService.delete("questions", id);
            Notification.show("Question removed");
            loadQuizQuestions(selected);
        } catch (Exception ex) {
            Notification.show("Remove failed: " + ex.getMessage());
        }
    }

    private void openAddQuestion() {
        if (selected == null || selected.get("id") == null) {
            Notification.show("Select a quiz first to add a question");
            return;
        }
        Long quizId = Long.valueOf(String.valueOf(selected.get("id")));
        new QuestionEditorDialog(backendDataService, quizId, null,
                () -> loadQuizQuestions(selected)).open();
    }

    private void resetEditor(Map<String, Object> subject) {
        selected = null;
        titleField.clear();
        descriptionField.clear();
        quizTypeField.setValue("STANDARD");
        questionCountField.setValue(10);
        contentTierField.setValue("FREEMIUM");
        freemiumIndexField.setValue(1);
        languageField.setValue("English");
        activeField.setValue(true);
        questionGrid.setItems(List.of());
    }

    private void saveQuiz() {
        if (subjectSelect.getValue() == null || subjectSelect.getValue().get("id") == null) {
            Notification.show("Choose a subject first");
            return;
        }
        Long subjectId = Long.valueOf(String.valueOf(subjectSelect.getValue().get("id")));
        List<Map<String, Object>> packs = backendDataService.listBy("content-packs", "subject", subjectId);
        if (packs.isEmpty()) {
            Notification.show("No content pack for this subject — create one first");
            return;
        }
        Map<String, Object> pack = picksActivePack(packs);
        Long packId = Long.valueOf(String.valueOf(pack.get("id")));

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("title", titleField.getValue());
        payload.put("description", descriptionField.getValue());
        payload.put("contentPackId", packId);
        payload.put("quizType", quizTypeField.getValue());
        payload.put("questionCount", questionCountField.getValue());
        payload.put("contentTier", contentTierField.getValue());
        payload.put("freemiumIndex", freemiumIndexField.getValue());
        payload.put("language", languageField.getValue());
        payload.put("active", activeField.getValue());

        try {
            if (selected != null && selected.get("id") != null) {
                Long id = Long.valueOf(String.valueOf(selected.get("id")));
                backendDataService.update("quizzes", id, payload);
                Notification.show("Quiz updated");
            } else {
                backendDataService.create("quizzes", payload);
                Notification.show("Quiz created");
            }
            loadQuizzes(subjectSelect.getValue());
        } catch (Exception ex) {
            Notification.show("Save failed: " + ex.getMessage());
        }
    }

    private Map<String, Object> picksActivePack(List<Map<String, Object>> packs) {
        for (Map<String, Object> pack : packs) {
            if (Boolean.parseBoolean(String.valueOf(pack.getOrDefault("active", false)))) {
                return pack;
            }
        }
        return packs.getFirst();
    }

    private static int toInt(Object value) {
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (Exception ex) {
            return 0;
        }
    }

    private static Integer toIntObj(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (Exception ex) {
            return null;
        }
    }
}
