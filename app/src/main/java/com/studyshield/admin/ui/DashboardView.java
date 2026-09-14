package com.studyshield.admin.ui;

import com.studyshield.admin.config.BackendApiProperties;
import com.studyshield.admin.service.BackendDataService;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;
import jakarta.annotation.security.RolesAllowed;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Route(value = "", layout = MainLayout.class)
@RouteAlias(value = "dashboard", layout = MainLayout.class)
@PageTitle("Home")
@RolesAllowed("ADMIN")
public class DashboardView extends VerticalLayout {

    private static final Map<String, String> KPI = Map.of(
            "boards", "Boards",
            "board-classes", "Board classes",
            "class-levels", "Class levels",
            "subjects", "Subjects",
            "offerings", "Offerings",
            "content-packs", "Packs",
            "quizzes", "Quizzes",
            "questions", "Questions"
    );

    private final BackendDataService backendDataService;
    private final FlexLayout kpis = new FlexLayout();

    public DashboardView(BackendDataService backendDataService, BackendApiProperties backendApiProperties) {
        this.backendDataService = backendDataService;
        setSizeFull();
        setPadding(false);
        setSpacing(false);

        Button rebuild = AdminUi.primary("Rebuild freemium catalog");
        rebuild.addClickListener(e -> rebuildCatalog());

        VerticalLayout page = AdminUi.page(
                "Curriculum home",
                "API: " + backendApiProperties.getBaseUrl() + " — counts refresh in the background.",
                rebuild);

        kpis.setFlexWrap(FlexLayout.FlexWrap.WRAP);
        kpis.getStyle().set("gap", "1rem");
        KPI.values().forEach(label -> kpis.add(AdminUi.kpi(label, "…")));

        Paragraph hint = new Paragraph(
                "Work top-down: Board → Board class (ordinal) → Offering (subject) → Pack → Quiz → Questions. "
                        + "A quiz always plays the latest version of each question. "
                        + "Rebuild catalog drops issued kid bundles so every offering with questions is offered.");
        hint.addClassName("ss-muted");

        page.add(AdminUi.card(kpis), AdminUi.card(hint));
        add(page);
        loadCounts();
    }

    private void rebuildCatalog() {
        try {
            Map<String, Object> result = backendDataService.postPath("quiz-bundles/rebuild-catalog", Map.of());
            Notification.show("Rebuilt catalog — deleted " + result.getOrDefault("issuedBundlesDeleted", "?")
                    + " issued bundles, seeded " + result.getOrDefault("classesSeeded", "?") + " classes");
            loadCounts();
        } catch (Exception ex) {
            Notification.show("Rebuild failed: " + ex.getMessage());
        }
    }

    private void loadCounts() {
        UI ui = UI.getCurrent();
        CompletableFuture.supplyAsync(() -> {
            Map<String, Integer> counts = new LinkedHashMap<>();
            KPI.keySet().forEach(key -> counts.put(key, backendDataService.list(key).size()));
            return counts;
        }).thenAccept(counts -> ui.access(() -> {
            kpis.removeAll();
            KPI.forEach((key, label) ->
                    kpis.add(AdminUi.kpi(label, String.valueOf(counts.getOrDefault(key, 0)))));
        }));
    }
}
