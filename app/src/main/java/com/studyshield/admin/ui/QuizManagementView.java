package com.studyshield.admin.ui;

import com.studyshield.admin.service.BackendDataService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H4;
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
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Route(value = "quizzes", layout = MainLayout.class)
@PageTitle("Quizzes")
@RolesAllowed("ADMIN")
public class QuizManagementView extends VerticalLayout {

    private static final List<String> QUIZ_TYPES = List.of("STANDARD", "SINGLE");
    private static final List<String> CONTENT_TIERS = List.of(
            "FREEMIUM", "PREMIUM", "LIBRARY", "PROMOTIONAL", "SEASONAL", "COMPLEMENTARY");

    private final BackendDataService api;
    private final ComboBox<Map<String, Object>> subjectSelect = new ComboBox<>("Subject");
    private final ComboBox<Map<String, Object>> packSelect = new ComboBox<>("Pack");
    private final Grid<Map<String, Object>> quizGrid = new Grid<>();
    private final Grid<Map<String, Object>> questionGrid = new Grid<>();

    private final TextField title = new TextField("Title");
    private final TextArea description = new TextArea("Description");
    private final ComboBox<String> quizType = new ComboBox<>("Quiz type");
    private final IntegerField questionCount = new IntegerField("Question count");
    private final ComboBox<String> contentTier = new ComboBox<>("Tier");
    private final IntegerField freemiumIndex = new IntegerField("Freemium index");
    private final TextField language = new TextField("Language");
    private final Checkbox active = new Checkbox("Active");

    private Map<String, Object> selectedQuiz;

    public QuizManagementView(BackendDataService api) {
        this.api = api;
        setSizeFull();
        setPadding(false);

        Button create = AdminUi.primary("New quiz");
        create.addClickListener(e -> clearQuiz());
        VerticalLayout page = AdminUi.page("Quizzes",
                "A quiz always plays the latest version of each question. Add or remove questions without deleting the bank copy.",
                create);

        subjectSelect.setItemLabelGenerator(item -> AdminUi.label(item, "name"));
        subjectSelect.setItems(api.list("subjects"));
        subjectSelect.setWidth("260px");
        subjectSelect.addValueChangeListener(e -> loadPacks());

        packSelect.setItemLabelGenerator(item -> AdminUi.str(item, "name") + " · " + AdminUi.str(item, "packType"));
        packSelect.setWidth("320px");
        packSelect.addValueChangeListener(e -> loadQuizzes());

        quizGrid.addColumn(r -> AdminUi.str(r, "id")).setHeader("ID").setAutoWidth(true);
        quizGrid.addColumn(r -> AdminUi.str(r, "title")).setHeader("Title").setFlexGrow(1);
        quizGrid.addColumn(r -> AdminUi.str(r, "contentTier")).setHeader("Tier");
        quizGrid.addColumn(r -> AdminUi.str(r, "active")).setHeader("Active");
        quizGrid.setHeight("220px");
        quizGrid.asSingleSelect().addValueChangeListener(e -> loadQuiz(e.getValue()));

        quizType.setItems(QUIZ_TYPES);
        contentTier.setItems(CONTENT_TIERS);
        contentTier.setValue("FREEMIUM");
        language.setValue("English");
        questionCount.setValue(10);
        active.setValue(true);

        FormLayout form = new FormLayout(title, description, quizType, questionCount, contentTier,
                freemiumIndex, language, active);
        Button save = AdminUi.primary("Save quiz");
        save.addClickListener(e -> saveQuiz());
        Button delete = AdminUi.danger("Delete quiz");
        delete.addClickListener(e -> {
            if (AdminUi.id(selectedQuiz) == null) {
                Notification.show("Select a quiz");
                return;
            }
            AdminUi.confirmDelete("Delete quiz?", "Questions stay in the bank if you remove them first.", () -> {
                api.delete("quizzes", AdminUi.id(selectedQuiz));
                clearQuiz();
                loadQuizzes();
            });
        });

        questionGrid.addColumn(r -> AdminUi.str(r, "id")).setHeader("ID").setAutoWidth(true);
        questionGrid.addColumn(r -> AdminUi.str(r, "questionText")).setHeader("Question").setFlexGrow(1);
        questionGrid.addColumn(r -> "v" + AdminUi.str(r, "version")).setHeader("Latest").setWidth("90px");
        questionGrid.addComponentColumn(this::questionActions).setHeader("").setAutoWidth(true);
        questionGrid.setHeight("280px");

        Button addNew = AdminUi.primary("Write question");
        addNew.addClickListener(e -> openEditor(null));
        Button addExisting = new Button("Add from bank");
        addExisting.addClickListener(e -> openBankPicker());

        HorizontalLayout qHeader = new HorizontalLayout(new H4("Questions in this quiz"), addExisting, addNew);
        qHeader.setAlignItems(Alignment.CENTER);

        page.add(
                new HorizontalLayout(subjectSelect, packSelect),
                AdminUi.card(quizGrid),
                AdminUi.card(form, new HorizontalLayout(save, delete)),
                AdminUi.card(qHeader, questionGrid));
        add(page);
    }

    private HorizontalLayout questionActions(Map<String, Object> question) {
        Button edit = new Button("Edit", e -> openEditor(question));
        Button remove = new Button("Remove from quiz");
        remove.addClickListener(e -> removeFromQuiz(question));
        return new HorizontalLayout(edit, remove);
    }

    private void loadPacks() {
        Long subjectId = AdminUi.id(subjectSelect.getValue());
        packSelect.setItems(subjectId == null ? List.of() : api.listBy("content-packs", "subject", subjectId));
        quizGrid.setItems(List.of());
        questionGrid.setItems(List.of());
    }

    private void loadQuizzes() {
        Long packId = AdminUi.id(packSelect.getValue());
        quizGrid.setItems(packId == null ? List.of() : api.listByPath("quizzes", "content-pack", packId));
        questionGrid.setItems(List.of());
    }

    private void loadQuiz(Map<String, Object> quiz) {
        selectedQuiz = quiz;
        if (quiz == null) {
            return;
        }
        title.setValue(AdminUi.str(quiz, "title"));
        description.setValue(AdminUi.str(quiz, "description"));
        String type = AdminUi.str(quiz, "quizType");
        quizType.setValue(QUIZ_TYPES.contains(type) ? type : "STANDARD");
        questionCount.setValue(parseInt(quiz.get("questionCount"), 10));
        String tier = AdminUi.str(quiz, "contentTier");
        contentTier.setValue(CONTENT_TIERS.contains(tier) ? tier : "FREEMIUM");
        freemiumIndex.setValue(parseInt(quiz.get("freemiumIndex"), 1));
        language.setValue(AdminUi.str(quiz, "language"));
        active.setValue(Boolean.parseBoolean(AdminUi.str(quiz, "active")));
        loadQuestions();
    }

    private void loadQuestions() {
        Long quizId = AdminUi.id(selectedQuiz);
        questionGrid.setItems(quizId == null ? List.of() : api.listByPath("questions", "quiz", quizId));
    }

    private void clearQuiz() {
        selectedQuiz = null;
        title.clear();
        description.clear();
        quizType.setValue("STANDARD");
        questionCount.setValue(10);
        contentTier.setValue("FREEMIUM");
        freemiumIndex.setValue(1);
        language.setValue("English");
        active.setValue(true);
        quizGrid.deselectAll();
        questionGrid.setItems(List.of());
    }

    private void saveQuiz() {
        Long packId = AdminUi.id(packSelect.getValue());
        if (packId == null) {
            Notification.show("Choose a pack");
            return;
        }
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("title", title.getValue());
        payload.put("description", description.getValue());
        payload.put("contentPackId", packId);
        payload.put("quizType", quizType.getValue());
        payload.put("questionCount", questionCount.getValue());
        payload.put("contentTier", contentTier.getValue());
        payload.put("freemiumIndex", freemiumIndex.getValue());
        payload.put("language", language.getValue());
        payload.put("active", active.getValue());
        try {
            if (AdminUi.id(selectedQuiz) != null) {
                api.update("quizzes", AdminUi.id(selectedQuiz), payload);
                Notification.show("Quiz updated");
            } else {
                api.create("quizzes", payload);
                Notification.show("Quiz created");
            }
            loadQuizzes();
        } catch (Exception ex) {
            Notification.show(ex.getMessage());
        }
    }

    private void openEditor(Map<String, Object> question) {
        Long quizId = AdminUi.id(selectedQuiz);
        if (quizId == null) {
            Notification.show("Select or save a quiz first");
            return;
        }
        new QuestionEditorDialog(api, quizId, question, this::loadQuestions).open();
    }

    private void removeFromQuiz(Map<String, Object> question) {
        Long subjectId = AdminUi.id(subjectSelect.getValue());
        if (subjectId == null || AdminUi.id(question) == null) {
            return;
        }
        try {
            Map<String, Object> library = api.postPath("quizzes/library/" + subjectId, Map.of());
            Long libraryQuizId = AdminUi.id(library);
            api.postPath("questions/" + AdminUi.id(question) + "/assign-quiz",
                    Map.of("quizId", libraryQuizId));
            Notification.show("Moved to the subject question bank — not deleted");
            loadQuestions();
        } catch (Exception ex) {
            Notification.show(ex.getMessage());
        }
    }

    private void openBankPicker() {
        Long subjectId = AdminUi.id(subjectSelect.getValue());
        Long quizId = AdminUi.id(selectedQuiz);
        if (subjectId == null || quizId == null) {
            Notification.show("Select a subject and a quiz");
            return;
        }
        Set<String> already = questionGrid.getListDataView().getItems()
                .map(q -> String.valueOf(q.get("id")))
                .collect(Collectors.toSet());
        List<Map<String, Object>> bank = new ArrayList<>(api.listByPath("questions", "subject", subjectId));
        bank.removeIf(q -> already.contains(String.valueOf(q.get("id")))
                || Objects.equals(String.valueOf(q.get("quizId")), String.valueOf(quizId)));

        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Add from question bank");
        dialog.setMinWidth("720px");
        Grid<Map<String, Object>> bankGrid = new Grid<>();
        bankGrid.setItems(bank);
        bankGrid.addColumn(r -> AdminUi.str(r, "questionText")).setHeader("Question").setFlexGrow(1);
        bankGrid.addColumn(r -> AdminUi.str(r, "quizId")).setHeader("Current quiz");
        bankGrid.setSelectionMode(Grid.SelectionMode.MULTI);
        bankGrid.setHeight("320px");
        Button add = AdminUi.primary("Add selected");
        add.addClickListener(e -> {
            for (Map<String, Object> q : bankGrid.getSelectedItems()) {
                api.postPath("questions/" + AdminUi.id(q) + "/assign-quiz", Map.of("quizId", quizId));
            }
            dialog.close();
            loadQuestions();
            Notification.show("Questions added — latest versions will play");
        });
        dialog.add(bankGrid, new HorizontalLayout(add, new Button("Cancel", ev -> dialog.close())));
        dialog.open();
    }

    private static int parseInt(Object value, int fallback) {
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (Exception ex) {
            return fallback;
        }
    }
}
