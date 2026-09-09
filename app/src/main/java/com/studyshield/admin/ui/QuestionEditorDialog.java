package com.studyshield.admin.ui;

import com.studyshield.admin.service.BackendDataService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Popup editor for a question inside a quiz. Saving an existing question creates a new version
 * on the backend (PUT /api/v1/questions/{id}); the latest version of a question is always the
 * one served for its quiz. "Revisions" lists every historical version and can load an old
 * revision back into the editor — saving then publishes it as a new version.
 */
public class QuestionEditorDialog extends Dialog {

    private static final List<String> QUESTION_TYPES = List.of("SINGLE_CHOICE", "MULTIPLE_CHOICE", "TRUE_FALSE");
    private static final List<String> DIFFICULTIES = List.of("EASY", "MEDIUM", "HARD");
    private static final List<String> OPTION_IDS = List.of("a", "b", "c", "d");

    private final BackendDataService backendDataService;
    private final Long quizId;
    private Map<String, Object> question;
    private final Runnable onSaved;

    private final TextArea questionText = new TextArea("Question text");
    private final ComboBox<String> questionType = new ComboBox<>("Type");
    private final TextArea optionsText = new TextArea("Options (one per line, A-D)");
    private final ComboBox<String> correctAnswer = new ComboBox<>("Correct answer option");
    private final ComboBox<String> difficulty = new ComboBox<>("Difficulty");
    private final IntegerField points = new IntegerField("Points");
    private final IntegerField orderIndex = new IntegerField("Order");
    private final Checkbox blacklisted = new Checkbox("Blacklisted");

    public QuestionEditorDialog(BackendDataService backendDataService, Long quizId,
                                Map<String, Object> question, Runnable onSaved) {
        this.backendDataService = backendDataService;
        this.quizId = quizId;
        this.question = question;
        this.onSaved = onSaved;

        setHeaderTitle(question != null ? "Edit question" : "New question");
        setModal(true);
        setMinWidth("680px");

        questionText.setWidthFull();
        optionsText.setWidthFull();
        questionType.setItems(QUESTION_TYPES);
        correctAnswer.setItems(OPTION_IDS);
        difficulty.setItems(DIFFICULTIES);

        FormLayout form = new FormLayout();
        form.add(questionText, questionType, optionsText, correctAnswer, difficulty, points, orderIndex);
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 2));

        Button revisionsButton = new Button("View revisions");
        revisionsButton.setVisible(question != null);
        revisionsButton.addClickListener(e -> showRevisions());

        Button saveButton = new Button(question != null ? "Save as new version" : "Create question");
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        saveButton.addClickListener(e -> save());

        Button cancelButton = new Button("Cancel", e -> close());

        HorizontalLayout footer = new HorizontalLayout();
        footer.setWidthFull();
        footer.setJustifyContentMode(FlexComponent.JustifyContentMode.START);
        footer.add(revisionsButton);
        footer.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
        footer.add(saveButton, cancelButton);
        footer.setAlignItems(FlexComponent.Alignment.CENTER);

        add(form, blacklisted, footer);
        if (question != null) {
            fillFrom(question);
        }
    }

    @SuppressWarnings("unchecked")
    private void fillFrom(Map<String, Object> q) {
        if (q == null || q.isEmpty()) {
            return;
        }
        questionText.setValue(String.valueOf(q.getOrDefault("questionText", "")));
        questionType.setValue(String.valueOf(q.getOrDefault("questionType", "SINGLE_CHOICE")));
        difficulty.setValue(String.valueOf(q.getOrDefault("difficulty", "EASY")));
        points.setValue(toInt(q.get("points")));
        orderIndex.setValue(toInt(q.get("orderIndex")));
        blacklisted.setValue(Boolean.parseBoolean(String.valueOf(q.getOrDefault("blacklisted", false))));

        optionsText.clear();
        Object optionsObj = q.get("options");
        if (optionsObj instanceof List<?> optionsList) {
            for (Object o : optionsList) {
                if (o instanceof Map<?, ?> option) {
                    String value = optionsText.getValue() + String.valueOf(option.get("text")) + "\n";
                    optionsText.setValue(value);
                }
            }
        }
        correctAnswer.clear();
        Object correctObj = q.get("correctAnswers");
        if (correctObj instanceof List<?> correctList && !correctList.isEmpty()) {
            String first = String.valueOf(correctList.getFirst());
            if (OPTION_IDS.contains(first)) {
                correctAnswer.setValue(first);
            }
        }
    }

    private void save() {
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
        payload.put("points", points.getValue() > 0 ? points.getValue() : 1);
        payload.put("orderIndex", orderIndex.getValue());
        payload.put("blacklisted", blacklisted.getValue());
        payload.put("quizId", quizId);

        try {
            if (question != null && question.get("id") != null) {
                Long id = Long.valueOf(String.valueOf(question.get("id")));
                backendDataService.update("questions", id, payload);
                Notification.show("Question updated — new version saved");
            } else {
                backendDataService.create("questions", payload);
                Notification.show("Question created");
            }
            if (onSaved != null) {
                onSaved.run();
            }
            close();
        } catch (Exception ex) {
            Notification.show("Save failed: " + ex.getMessage());
        }
    }

    private void showRevisions() {
        Long questionId = question != null ? Long.valueOf(String.valueOf(question.get("id"))) : null;
        if (questionId == null) {
            return;
        }
        List<Map<String, Object>> revisions = backendDataService.listRevisions(questionId);
        if (revisions.isEmpty()) {
            Notification.show("No revisions available");
            return;
        }

        Dialog revisionsDialog = new Dialog();
        revisionsDialog.setHeaderTitle("Question revisions");
        revisionsDialog.setModal(true);
        revisionsDialog.setMinWidth("720px");

        Grid<Map<String, Object>> grid = new Grid<>();
        grid.addColumn(item -> item.getOrDefault("version", "-")).setHeader("Version").setWidth("90px");
        grid.addColumn(item -> String.valueOf(item.getOrDefault("updatedAt", item.getOrDefault("createdAt", "-"))))
                .setHeader("Updated");
        grid.addColumn(item -> item.getOrDefault("questionText", "-")).setHeader("Question text");
        grid.addComponentColumn(item -> {
            Button button = new Button("View / restore");
            button.addClickListener(e -> {
                revisionsDialog.close();
                viewRevision(item);
            });
            return button;
        }).setHeader("Actions");
        grid.setItems(revisions);
        grid.setHeight("320px");

        Button closeButton = new Button("Close", e -> revisionsDialog.close());
        revisionsDialog.add(grid, closeButton);
        revisionsDialog.open();
    }

    private void viewRevision(Map<String, Object> revision) {
        Dialog detail = new Dialog();
        detail.setHeaderTitle("Version " + revision.getOrDefault("version", "-"));
        detail.setModal(true);
        detail.setMinWidth("600px");

        TextArea text = new TextArea("Question text");
        text.setValue(String.valueOf(revision.getOrDefault("questionText", "")));
        text.setReadOnly(true);

        StringBuilder options = new StringBuilder();
        Object optionsObj = revision.get("options");
        if (optionsObj instanceof List<?> optionsList) {
            for (Object o : optionsList) {
                if (o instanceof Map<?, ?> option) {
                    options.append(String.valueOf(option.get("text"))).append('\n');
                }
            }
        }
        TextArea optionsField = new TextArea("Options");
        optionsField.setValue(options.toString());
        optionsField.setReadOnly(true);

        TextField correct = new TextField("Correct answers");
        Object correctObj = revision.get("correctAnswers");
        correct.setValue(correctObj instanceof List<?> list ? String.valueOf(list) : "");
        correct.setReadOnly(true);

        Button restoreButton = new Button("Restore as new version");
        restoreButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        restoreButton.addClickListener(e -> {
            fillFrom(revision);
            detail.close();
            Notification.show("Revision loaded — save to publish as a new version");
        });
        Button closeButton = new Button("Close", e -> detail.close());

        HorizontalLayout actions = new HorizontalLayout();
        actions.setWidthFull();
        actions.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
        actions.add(restoreButton, closeButton);

        detail.add(text, optionsField, correct, actions);
        detail.open();
    }

    private static int toInt(Object value) {
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (Exception ex) {
            return 0;
        }
    }
}