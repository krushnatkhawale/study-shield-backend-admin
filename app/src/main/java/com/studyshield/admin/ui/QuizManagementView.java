package com.studyshield.admin.ui;

import com.studyshield.admin.service.BackendDataService;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Route(value = "quizzes", layout = MainLayout.class)
@PageTitle("Quizzes")
@RolesAllowed("ADMIN")
public class QuizManagementView extends VerticalLayout {

    private static final List<String> QUIZ_TYPES = List.of("STANDARD", "SINGLE");
    private static final List<String> CONTENT_TIERS = List.of(
            "FREEMIUM", "PREMIUM", "LIBRARY", "PROMOTIONAL", "SEASONAL", "COMPLEMENTARY");

    private final BackendDataService api;
    private final ComboBox<Map<String, Object>> boardSelect = new ComboBox<>("Board");
    private final ComboBox<Map<String, Object>> boardClassSelect = new ComboBox<>("Board class");
    private final ComboBox<Map<String, Object>> offeringSelect = new ComboBox<>("Offering");
    private final ComboBox<Map<String, Object>> packSelect = new ComboBox<>("Pack");
    private final Grid<Map<String, Object>> quizGrid = new Grid<>();
    private final Grid<Map<String, Object>> questionGrid = new Grid<>();

    private Map<String, Object> selectedQuiz;

    public QuizManagementView(BackendDataService api) {
        this.api = api;
        setSizeFull();
        setPadding(false);

        Button create = AdminUi.primary("New quiz");
        create.addClickListener(e -> openDialog(null));
        VerticalLayout page = AdminUi.page("Quizzes",
                "A quiz always plays the latest version of each question. Double-click a row to edit. Select a quiz to manage its questions below.",
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
        offeringSelect.addValueChangeListener(e -> loadPacks());

        packSelect.setItemLabelGenerator(item -> AdminUi.str(item, "name") + " · " + AdminUi.str(item, "packType"));
        packSelect.setWidth("320px");
        packSelect.addValueChangeListener(e -> loadQuizzes());

        quizGrid.addColumn(r -> AdminUi.str(r, "id")).setHeader("ID").setAutoWidth(true);
        quizGrid.addColumn(r -> AdminUi.str(r, "title")).setHeader("Title").setFlexGrow(1);
        quizGrid.addColumn(r -> AdminUi.str(r, "contentTier")).setHeader("Tier");
        quizGrid.addColumn(r -> AdminUi.str(r, "active")).setHeader("Active");
        quizGrid.setHeight("220px");
        quizGrid.setWidthFull();
        quizGrid.addItemDoubleClickListener(e -> openDialog(e.getItem()));
        quizGrid.asSingleSelect().addValueChangeListener(e -> loadQuiz(e.getValue()));

        questionGrid.addColumn(r -> AdminUi.str(r, "id")).setHeader("ID").setAutoWidth(true);
        questionGrid.addColumn(r -> AdminUi.str(r, "questionText")).setHeader("Question").setFlexGrow(1);
        questionGrid.addColumn(r -> "v" + AdminUi.str(r, "version")).setHeader("Latest").setWidth("90px");
        questionGrid.addComponentColumn(this::questionActions).setHeader("").setAutoWidth(true);
        questionGrid.setHeight("280px");
        questionGrid.setWidthFull();

        Button addNew = AdminUi.primary("Write question");
        addNew.addClickListener(e -> openEditor(null));
        Button addExisting = new Button("Add from bank");
        addExisting.addClickListener(e -> openBankPicker());

        HorizontalLayout qHeader = new HorizontalLayout(
                new com.vaadin.flow.component.html.H4("Questions in selected quiz"), addExisting, addNew);
        qHeader.setAlignItems(Alignment.CENTER);

        HorizontalLayout selectors = new HorizontalLayout(boardSelect, boardClassSelect, offeringSelect, packSelect);
        selectors.setAlignItems(Alignment.END);
        page.add(
                selectors,
                AdminUi.card(quizGrid),
                AdminUi.card(qHeader, questionGrid));
        add(page);
        loadQuizzes();
    }

    private void openDialog(Map<String, Object> row) {
        Long id = AdminUi.id(row);
        boolean isNew = id == null;

        TextField title = new TextField("Title");
        title.setRequired(true);
        title.setWidthFull();
        TextArea description = new TextArea("Description");
        description.setWidthFull();
        ComboBox<String> quizType = new ComboBox<>("Quiz type");
        quizType.setItems(QUIZ_TYPES);
        quizType.setWidthFull();
        IntegerField questionCount = new IntegerField("Question count");
        questionCount.setWidthFull();
        ComboBox<String> contentTier = new ComboBox<>("Tier");
        contentTier.setItems(CONTENT_TIERS);
        contentTier.setWidthFull();
        IntegerField freemiumIndex = new IntegerField("Freemium index");
        freemiumIndex.setWidthFull();
        TextField language = new TextField("Language");
        language.setWidthFull();
        Checkbox active = new Checkbox("Active");
        if (row != null) {
            title.setValue(AdminUi.str(row, "title"));
            description.setValue(AdminUi.str(row, "description"));
            String type = AdminUi.str(row, "quizType");
            quizType.setValue(QUIZ_TYPES.contains(type) ? type : "STANDARD");
            questionCount.setValue(parseInt(row.get("questionCount"), 10));
            String tier = AdminUi.str(row, "contentTier");
            contentTier.setValue(CONTENT_TIERS.contains(tier) ? tier : "FREEMIUM");
            freemiumIndex.setValue(parseInt(row.get("freemiumIndex"), 1));
            language.setValue(AdminUi.str(row, "language"));
            active.setValue(Boolean.parseBoolean(AdminUi.str(row, "active")));
        } else {
            quizType.setValue("STANDARD");
            questionCount.setValue(10);
            contentTier.setValue("FREEMIUM");
            freemiumIndex.setValue(1);
            language.setValue("English");
            active.setValue(true);
        }

        FormLayout form = AdminUi.entityForm(title, description, quizType, questionCount,
                contentTier, freemiumIndex, language, active);
        form.setColspan(description, 2);

        Dialog dialog = AdminUi.entityDialog(isNew ? "New quiz" : "Edit quiz", form);

        Button save = AdminUi.primary("Save");
        save.addClickListener(e -> {
            Long packId = AdminUi.id(packSelect.getValue());
            if (packId == null) {
                Notification.show("Choose a pack");
                return;
            }
            if (title.getValue().isBlank()) {
                Notification.show("Name is required");
                title.focus();
                return;
            }
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("title", title.getValue().trim());
            payload.put("description", description.getValue());
            payload.put("contentPackId", packId);
            payload.put("quizType", quizType.getValue());
            payload.put("questionCount", questionCount.getValue());
            payload.put("contentTier", contentTier.getValue());
            payload.put("freemiumIndex", freemiumIndex.getValue());
            payload.put("language", language.getValue());
            payload.put("active", active.getValue());
            try {
                if (!isNew) {
                    api.update("quizzes", id, payload);
                    Notification.show("Quiz updated");
                } else {
                    api.create("quizzes", payload);
                    Notification.show("Quiz created");
                }
                dialog.close();
                loadQuizzes();
            } catch (Exception ex) {
                Notification.show(ex.getMessage());
            }
        });
        Button delete = AdminUi.danger("Delete");
        delete.setVisible(!isNew);
        delete.addClickListener(e -> AdminUi.confirmDelete("Delete quiz?", "Questions stay in the bank if you remove them first.", () -> {
            api.delete("quizzes", id);
            selectedQuiz = null;
            dialog.close();
            loadQuizzes();
        }));
        AdminUi.dialogFooter(dialog, delete, save);

        dialog.open();
        title.focus();
    }

    private HorizontalLayout questionActions(Map<String, Object> question) {
        Button edit = new Button("Edit", e -> openEditor(question));
        Button remove = new Button("Remove from quiz");
        remove.addClickListener(e -> removeFromQuiz(question));
        return new HorizontalLayout(edit, remove);
    }

    private void loadBoardClasses() {
        Long boardId = AdminUi.id(boardSelect.getValue());
        boardClassSelect.setItems(boardId == null ? List.of() : api.listBy("board-classes", "board", boardId));
        offeringSelect.setItems(List.of());
        packSelect.setItems(List.of());
        loadQuizzes();
    }

    private void loadOfferings() {
        Long boardClassId = AdminUi.id(boardClassSelect.getValue());
        offeringSelect.setItems(boardClassId == null ? List.of() : api.listBy("offerings", "board-class", boardClassId));
        packSelect.setItems(List.of());
        loadQuizzes();
    }

    private void loadPacks() {
        Long offeringId = AdminUi.id(offeringSelect.getValue());
        packSelect.setItems(offeringId == null ? List.of() : api.listBy("content-packs", "offering", offeringId));
        loadQuizzes();
    }

    private void loadQuizzes() {
        Long packId = AdminUi.id(packSelect.getValue());
        quizGrid.setItems(packId == null ? api.list("quizzes") : api.listByPath("quizzes", "content-pack", packId));
        questionGrid.setItems(List.of());
        questionGrid.setItems(List.of());
    }

    private void loadQuiz(Map<String, Object> quiz) {
        selectedQuiz = quiz;
        if (quiz == null) {
            questionGrid.setItems(List.of());
            return;
        }
        loadQuestions();
    }

    private void loadQuestions() {
        Long quizId = AdminUi.id(selectedQuiz);
        questionGrid.setItems(quizId == null ? List.of() : api.listByPath("questions", "quiz", quizId));
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
        Long offeringId = AdminUi.id(offeringSelect.getValue());
        if (offeringId == null || AdminUi.id(question) == null) {
            return;
        }
        try {
            Map<String, Object> library = api.postPath("quizzes/library/" + offeringId, Map.of());
            Long libraryQuizId = AdminUi.id(library);
            api.postPath("questions/" + AdminUi.id(question) + "/assign-quiz",
                    Map.of("quizId", libraryQuizId));
            Notification.show("Moved to the offering library quiz — not deleted");
            loadQuestions();
        } catch (Exception ex) {
            Notification.show(ex.getMessage());
        }
    }

    private void openBankPicker() {
        Long offeringId = AdminUi.id(offeringSelect.getValue());
        Long quizId = AdminUi.id(selectedQuiz);
        if (offeringId == null || quizId == null) {
            Notification.show("Select an offering and a quiz");
            return;
        }
        Long subjectId = getOfferingSubjectId();
        if (subjectId == null) {
            Notification.show("Could not determine subject for the selected offering");
            return;
        }
        java.util.Set<String> already = questionGrid.getListDataView().getItems()
                .map(q -> String.valueOf(q.get("id")))
                .collect(java.util.stream.Collectors.toSet());
        List<Map<String, Object>> bank = new java.util.ArrayList<>(api.listByPath("questions", "subject", subjectId));
        bank.removeIf(q -> already.contains(String.valueOf(q.get("id")))
                || java.util.Objects.equals(String.valueOf(q.get("quizId")), String.valueOf(quizId)));

        Dialog dialog = AdminUi.entityDialog("Add from question bank", new VerticalLayout());
        dialog.setWidth("720px");
        Grid<Map<String, Object>> bankGrid = new Grid<>();
        bankGrid.setItems(bank);
        bankGrid.addColumn(r -> AdminUi.str(r, "questionText")).setHeader("Question").setFlexGrow(1);
        bankGrid.addColumn(r -> AdminUi.str(r, "quizId")).setHeader("Current quiz");
        bankGrid.setSelectionMode(Grid.SelectionMode.MULTI);
        bankGrid.setHeight("320px");
        bankGrid.setWidthFull();
        dialog.add(bankGrid);
        Button add = AdminUi.primary("Save");
        add.addClickListener(e -> {
            for (Map<String, Object> q : bankGrid.getSelectedItems()) {
                api.postPath("questions/" + AdminUi.id(q) + "/assign-quiz", Map.of("quizId", quizId));
            }
            dialog.close();
            loadQuestions();
            Notification.show("Questions added — latest versions will play");
        });
        Button delete = AdminUi.danger("Delete");
        delete.setVisible(false);
        AdminUi.dialogFooter(dialog, delete, add);

        dialog.open();
    }

    /** Read the subjectId from the currently selected offering row. */
    @SuppressWarnings("unchecked")
    private Long getOfferingSubjectId() {
        Map<String, Object> offering = offeringSelect.getValue();
        if (offering == null) return null;
        Object val = offering.get("subjectId");
        if (val instanceof Number n) return n.longValue();
        try { return Long.parseLong(String.valueOf(val)); } catch (Exception e) { return null; }
    }

    private static int parseInt(Object value, int fallback) {
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (Exception ex) {
            return fallback;
        }
    }
}
