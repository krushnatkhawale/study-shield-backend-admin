package com.studyshield.admin.ui;

import com.studyshield.admin.config.BackendApiProperties;
import com.studyshield.admin.service.BackendDataService;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Route(value = "dashboard", layout = MainLayout.class)
@PageTitle("Dashboard")
@PermitAll
public class DashboardView extends VerticalLayout {

    private static final List<String> TABLES = List.of(
            "boards",
            "class-grades",
            "subjects",
            "content-packs",
            "quizzes",
            "questions",
            "quiz-bundles",
            "users",
            "admin-users",
            "quiz-attempts",
            "quiz-results",
            "tv-users",
            "wifi-networks",
            "connected-tvs"
    );

    private final BackendDataService backendDataService;
    private final HorizontalLayout summary = new HorizontalLayout();
    private final Span loading = new Span("Loading backend counts…");

    public DashboardView(BackendDataService backendDataService, BackendApiProperties backendApiProperties) {
        this.backendDataService = backendDataService;
        setPadding(true);
        setSpacing(true);

        add(new H2("StudyShield backend admin"));

        HorizontalLayout stats = new HorizontalLayout(
                statusCard("Backend", "StudyShield modulith"),
                statusCard("API", backendApiProperties.getBaseUrl()),
                statusCard("Collections", String.valueOf(TABLES.size())),
                statusCard("Mode", "CRUD admin")
        );
        stats.setWidthFull();
        stats.setSpacing(true);
        add(stats);

        Grid<String> grid = new Grid<>();
        grid.addColumn(item -> item).setHeader("Managed table");
        grid.setItems(TABLES);
        grid.setHeight("420px");
        add(grid);

        add(new H2("Backend data overview"));

        summary.setWidthFull();
        summary.getStyle().set("flex-wrap", "wrap");
        loading.getStyle().set("color", "var(--lumo-secondary-text-color)");
        summary.add(loading);
        add(summary);

        loadCountsAsync();
    }

    /**
     * Fetch row counts off the UI thread so the page renders immediately even when the backend is
     * slow or unreachable; each call is bounded by the configured connect/read timeouts.
     */
    private void loadCountsAsync() {
        UI ui = UI.getCurrent();
        CompletableFuture.supplyAsync(() -> {
            Map<String, Integer> counts = new LinkedHashMap<>();
            TABLES.parallelStream().forEach(table ->
                    counts.put(table, backendDataService.list(table).size()));
            return counts;
        }).thenAccept(counts -> {
            try {
                ui.access(() -> {
                    summary.remove(loading);
                    for (Map.Entry<String, Integer> entry : counts.entrySet()) {
                        summary.add(statusCard(entry.getKey(), String.valueOf(entry.getValue())));
                    }
                });
            } catch (Exception ex) {
                // UI already closed; nothing to update.
            }
        });
    }

    private Span statusCard(String label, String value) {
        Span card = new Span(label + "\n" + value);
        card.getStyle()
                .set("display", "inline-block")
                .set("padding", "1rem 1.25rem")
                .set("border-radius", "12px")
                .set("background", "#f4f5f7")
                .set("font-weight", "600")
                .set("white-space", "pre-line")
                .set("min-width", "180px");
        return card;
    }
}