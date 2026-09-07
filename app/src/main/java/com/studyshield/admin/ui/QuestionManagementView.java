package com.studyshield.admin.ui;

import com.studyshield.admin.service.BackendDataService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Add and edit questions inside a quiz. Pick a subject, then a quiz, then edit its questions.
 * Options are entered as lines; the option (A/B/C/D) holding the correct answer is picked by
 * selecting the matching option id below.
 */
@Route(value = "questions", layout = MainLayout.class)
@PageTitle("Question management")
@RolesAllowed("ADMIN")
public class QuestionManagementView extends VerticalLayout {

    private static final List<String> QUESTION_TYPES = List.of("SINGLE_CHOICE", "MULTIPLE_CHOICE", "TRUE_FALSE");
    private static final List<String> DIFFICULTIES = List.of("EASY", "MEDIUM", "HARD");
    private static final List<String> OPTION_IDS = List.of("a", "b", "c", "d");

    private final BackendDataService backendDataService;

    private final ComboBox<Map<String, Object>> subjectSelect = new ComboBox<>();
    private final ComboBox<Map<String, Object>> quizSelect = new ComboBox<>();
    private final Grid<Map<String, Object>> grid = new Grid<>();
    private final FormLayout editor = new FormLayout();

    private final TextArea questionText = new TextArea("Question text");
    private final ComboBox<String> questionType = new ComboBox<>("Type");
    private final TextArea optionsText = new TextArea("Options (one per line, A-D)");
    private final ComboBox<String> correctAnswer = new ComboBox<>("Correct answer option");
    private final ComboBox<String> difficulty = new ComboBox<>("Difficulty");
    private final IntegerField points = new IntegerField("Points");
    private final IntegerField orderIndex = new IntegerField("Order");
    private final Checkbox blacklisted = new Checkbox("Blacklisted");

    private final Button saveButton = new Button("Save question");
    private final Button newButton = new Button("New question");

    private List<Map<String, Object>> quizList = new ArrayList<>();
    private Map<String, Object> selected;

    public QuestionManagementView(BackendDataService backendDataService) {
        this.backendDataService = backendDataService;

        setPadding(true);
        setSpacing(true);
        setSizeFull();
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
        grid.setSelectionMode(Grid.SelectionMode.SINGLE);
        grid.asSingleSelect().addValueChangeListener(event -> loadIntoEditor(event.getValue()));
        grid.setHeight("300px");

        questionType.setItems(QUESTION_TYPES);
        correctAnswer.setItems(OPTION_IDS);
        difficulty.setItems(DIFFICULTIES);
        questionType.setValue("SINGLE_CHOICE");
        difficulty.setValue("EASY");
        points.setValue(1);
        orderIndex.setValue(0);

        editor.add(questionText, questionType, optionsText, correctAnswer, difficulty, points, orderIndex, blacklisted);
        editor.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 2));

        newButton.addClickListener(e -> resetEditor());
        saveButton.addClickListener(e -> saveQuestion());

        add(subjectSelect, quizSelect, grid, editor, new HorizontalLayout(newButton, saveButton));
    }

    private void loadQuizzes(Map<String, Object> subject) {
        quizList.clear();
        quizSelect.clear();
        if (subject == null || subject.get("id") == null) {
            grid.setItems(List.of());
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
        List<Map<String, Object>> questions = backendDataService.listByPath("questions", "quiz", quizId);
        grid.setItems(questions);
        if (!questions.isEmpty()) {
            grid.select(questions.getFirst());
        }
    }

    @SuppressWarnings("unchecked")
    private void loadIntoEditor(Map<String, Object> question) {
        if (question == null || question.isEmpty()) {
            return;
        }
        selected = question;
        questionText.setValue(String.valueOf(question.getOrDefault("questionText", "")));
        questionType.setValue(String.valueOf(question.getOrDefault("questionType", "SINGLE_CHOICE")));
        difficulty.setValue(String.valueOf(question.getOrDefault("difficulty", "EASY")));
        points.setValue(toInt(question.get("points")));
        orderIndex.setValue(toInt(question.get("orderIndex")));
        blacklisted.setValue(Boolean.parseBoolean(String.valueOf(question.getOrDefault("blacklisted", false))));

        optionsText.clear();
        Object optionsObj = question.get("options");
        if (optionsObj instanceof List<?> optionsList) {
            for (Object o : optionsList) {
                if (o instanceof Map<?, ?> option) {
                    optionsText.setValue(optionsText.getValue() + String.valueOf(option.get("text")) + "\n");
                }
            }
        }
        Object correctObj = question.get("correctAnswers");
        if (correctObj instanceof List<?> correctList && !correctList.isEmpty()) {
            correctAnswer.setValue(String.valueOf(correctList.getFirst()));
        }
    }

    private void resetEditor() {
        selected = null;
        questionText.clear();
        optionsText.clear();
        questionType.setValue("SINGLE_CHOICE");
        difficulty.setValue("EASY");
        points.setValue(1);
        orderIndex.setValue(0);
        blacklisted.setValue(false);
        correctAnswer.clear();
    }

    private void saveQuestion() {
        if (quizSelect.getValue() == null || quizSelect.getValue().get("id") == null) {
            Notification.show("Choose a quiz first");
            return;
        }
        Long quizId = Long.valueOf(String.valueOf(quizSelect.getValue().get("id")));

        List<Map<String, Object>> options = new ArrayList<>();
        int idx = 0;
        for (String line : optionsText.getValue().split("\\n")) {
            String text = line.trim();
            if (text.isBlank()) {
                continue;
            }
            Map<String, Object> option = new LinkedHashMap<>();
            option.put("id", OPTION_IDS.get(Math.min(idx, OPTION_IDS.size() - 1)));
            option.put("text", text);
            option.put("imageUrl", null);
            options.add(option);
            idx++;
        }

        Set<String> correct = new LinkedHashSet<>();
        if (correctAnswer.getValue() != null) {
            correct.add(correctAnswer.getValue());
        } else if (!options.isEmpty()) {
            correct.add(String.valueOf(options.getFirst().get("id")));
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("questionText", questionText.getValue());
        payload.put("questionType", questionType.getValue());
        payload.put("options", options);
        payload.put("correctAnswers", new ArrayList<>(correct));
        payload.put("difficulty", difficulty.getValue());
        payload.put("points", points.getValue());
        payload.put("orderIndex", orderIndex.getValue());
        payload.put("blacklisted", blacklisted.getValue());
        payload.put("quizId", quizId);
        payload.put("active", true);

        try {
            if (selected != null && selected.get("id") != null) {
                Long id = Long.valueOf(String.valueOf(selected.get("id")));
                backendDataService.update("questions", id, payload);
                Notification.show("Question updated");
            } else {
                backendDataService.create("questions", payload);
                Notification.show("Question created");
            }
            loadQuestions(quizSelect.getValue());
        } catch (Exception ex) {
            Notification.show("Save failed: " + ex.getMessage());
        }
    }

    private static int toInt(Object value) {
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (Exception ex) {
            return 0;
        }
    }
}
