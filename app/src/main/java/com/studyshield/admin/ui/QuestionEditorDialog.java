package com.studyshield.admin.ui;

import com.studyshield.admin.service.BackendDataService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Create or edit a question. Saving an existing question creates a new version;
 * quizzes always play the latest version.
 */
public class QuestionEditorDialog extends Dialog {

    private static final List<String> QUESTION_TYPES = List.of("SINGLE_CHOICE", "MULTIPLE_CHOICE", "TRUE_FALSE");
    private static final List<String> DIFFICULTIES = List.of("EASY", "MEDIUM", "HARD");
    private static final List<String> OPTION_IDS = List.of("a", "b", "c", "d");

    private final BackendDataService api;
    private final Long quizId;
    private Map<String, Object> question;
    private final Runnable onSaved;

    private final TextArea questionText = new TextArea("Question text");
    private final ComboBox<String> questionType = new ComboBox<>("Type");
    private final TextArea optionsText = new TextArea("Options (one per line, A–D)");
    private final ComboBox<String> correctAnswer = new ComboBox<>("Correct option");
    private final ComboBox<String> difficulty = new ComboBox<>("Difficulty");
    private final IntegerField points = new IntegerField("Points");
    private final IntegerField orderIndex = new IntegerField("Order in quiz");
    private final Checkbox blacklisted = new Checkbox("Hidden (blacklisted)");
    private final MultiSelectComboBox<Map<String, Object>> subjects = new MultiSelectComboBox<>("Linked subjects");
    private final TextField explanation = new TextField("Explanation (optional)");

    public QuestionEditorDialog(BackendDataService api, Long quizId,
                                Map<String, Object> question, Runnable onSaved) {
        this.api = api;
        this.quizId = quizId;
        this.question = question;
        this.onSaved = onSaved;

        setHeaderTitle(question != null ? "Edit question" : "New question");
        setModal(true);
        setDraggable(false);
        setWidth("560px");
        setMaxWidth("95vw");

        questionText.setWidthFull();
        optionsText.setWidthFull();
        questionType.setItems(QUESTION_TYPES);
        questionType.setValue("SINGLE_CHOICE");
        correctAnswer.setItems(OPTION_IDS);
        difficulty.setItems(DIFFICULTIES);
        difficulty.setValue("EASY");
        points.setValue(1);
        orderIndex.setValue(0);

        List<Map<String, Object>> allSubjects = api.list("subjects");
        subjects.setItems(allSubjects);
        subjects.setItemLabelGenerator(item -> AdminUi.label(item, "name"));
        subjects.setWidthFull();

        FormLayout form = new FormLayout(questionText, questionType, optionsText, correctAnswer,
                difficulty, points, orderIndex, explanation, subjects);
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1, FormLayout.ResponsiveStep.LabelsPosition.TOP),
                new FormLayout.ResponsiveStep("480px", 2, FormLayout.ResponsiveStep.LabelsPosition.TOP));
        form.setColspan(questionText, 2);
        form.setColspan(optionsText, 2);
        form.setColspan(subjects, 2);
        form.setWidthFull();

        boolean isNew = question == null;
        Button revisions = new Button("Revisions");
        revisions.setVisible(!isNew);
        revisions.addClickListener(e -> showRevisions());
        Button delete = AdminUi.danger("Delete");
        delete.setVisible(!isNew);
        delete.addClickListener(e -> AdminUi.confirmDelete("Delete question?",
                "All versions of this question will be removed.", () -> {
                    api.delete("questions", AdminUi.id(question));
                    if (onSaved != null) {
                        onSaved.run();
                    }
                    close();
                }));

        Button save = AdminUi.primary("Save");
        save.addClickListener(e -> save());
        Button cancel = new Button("Cancel", e -> close());
        cancel.addClickShortcut(com.vaadin.flow.component.Key.ESCAPE);

        HorizontalLayout footer = new HorizontalLayout(delete, save, cancel);
        footer.setWidthFull();
        footer.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
        footer.setAlignItems(FlexComponent.Alignment.CENTER);

        HorizontalLayout revisionsBar = new HorizontalLayout(revisions);
        revisionsBar.setWidthFull();

        add(form, blacklisted, revisionsBar);
        getFooter().add(footer);
        if (question != null) {
            fillFrom(question);
        }
    }

    public void open() {
        super.open();
        questionText.focus();
    }

    @SuppressWarnings("unchecked")
    private void fillFrom(Map<String, Object> q) {
        if (q == null) {
            return;
        }
        questionText.setValue(AdminUi.str(q, "questionText"));
        String type = AdminUi.str(q, "questionType");
        if (QUESTION_TYPES.contains(type)) {
            questionType.setValue(type);
        }
        String diff = AdminUi.str(q, "difficulty");
        if (DIFFICULTIES.contains(diff)) {
            difficulty.setValue(diff);
        }
        points.setValue(toInt(q.get("points")));
        orderIndex.setValue(toInt(q.get("orderIndex")));
        blacklisted.setValue(Boolean.parseBoolean(AdminUi.str(q, "blacklisted")));
        explanation.setValue(AdminUi.str(q, "explanation"));

        optionsText.clear();
        Object optionsObj = q.get("options");
        if (optionsObj instanceof List<?> optionsList) {
            StringBuilder builder = new StringBuilder();
            for (Object o : optionsList) {
                if (o instanceof Map<?, ?> option) {
                    builder.append(option.get("text")).append('\n');
                }
            }
            optionsText.setValue(builder.toString());
        }
        Object correctObj = q.get("correctAnswers");
        if (correctObj instanceof List<?> correctList && !correctList.isEmpty()) {
            String first = String.valueOf(correctList.getFirst());
            if (OPTION_IDS.contains(first)) {
                correctAnswer.setValue(first);
            }
        }
        Set<Map<String, Object>> selectedSubjects = new LinkedHashSet<>();
        Object ids = q.get("subjectIds");
        if (ids instanceof List<?> list) {
            for (Object id : list) {
                subjects.getListDataView().getItems()
                        .filter(s -> String.valueOf(s.get("id")).equals(String.valueOf(id)))
                        .findFirst()
                        .ifPresent(selectedSubjects::add);
            }
        }
        subjects.select(selectedSubjects);
    }

    private void save() {
        if (quizId == null) {
            Notification.show("A quiz is required so the question can be stored");
            return;
        }
        if (questionText.getValue().isBlank()) {
            Notification.show("Name is required");
            questionText.focus();
            return;
        }
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
        List<String> correct = new ArrayList<>();
        if (correctAnswer.getValue() != null) {
            correct.add(correctAnswer.getValue());
        } else if (!options.isEmpty()) {
            correct.add(String.valueOf(options.getFirst().get("id")));
        }
        List<Long> subjectIds = subjects.getSelectedItems().stream()
                .map(AdminUi::id)
                .filter(id -> id != null)
                .collect(Collectors.toList());

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("questionText", questionText.getValue());
        payload.put("questionType", questionType.getValue());
        payload.put("options", options);
        payload.put("correctAnswers", correct);
        payload.put("difficulty", difficulty.getValue());
        payload.put("points", points.getValue() != null && points.getValue() > 0 ? points.getValue() : 1);
        payload.put("orderIndex", orderIndex.getValue() == null ? 0 : orderIndex.getValue());
        payload.put("blacklisted", blacklisted.getValue());
        payload.put("quizId", quizId);
        payload.put("subjectIds", subjectIds);
        payload.put("explanation", explanation.getValue());
        try {
            if (question != null && AdminUi.id(question) != null) {
                api.update("questions", AdminUi.id(question), payload);
                Notification.show("New version saved — quizzes use this latest text");
            } else {
                api.create("questions", payload);
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
        Long questionId = AdminUi.id(question);
        if (questionId == null) {
            return;
        }
        List<Map<String, Object>> revisions = api.listRevisions(questionId);
        Dialog revisionsDialog = new Dialog();
        revisionsDialog.setHeaderTitle("Versions");
        revisionsDialog.setMinWidth("720px");
        Grid<Map<String, Object>> grid = new Grid<>();
        grid.addColumn(item -> item.getOrDefault("version", "-")).setHeader("v").setWidth("70px");
        grid.addColumn(item -> item.getOrDefault("questionText", "-")).setHeader("Text");
        grid.addComponentColumn(item -> {
            Button load = new Button("Load");
            load.addClickListener(e -> {
                fillFrom(item);
                revisionsDialog.close();
                Notification.show("Loaded — save to publish as a new version");
            });
            return load;
        }).setHeader("");
        grid.setItems(revisions);
        grid.setHeight("280px");
        revisionsDialog.add(grid, new Button("Close", e -> revisionsDialog.close()));
        revisionsDialog.open();
    }

    private static int toInt(Object value) {
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (Exception ex) {
            return 0;
        }
    }
}
