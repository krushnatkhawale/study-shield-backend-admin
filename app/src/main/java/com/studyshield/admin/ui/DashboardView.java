package com.studyshield.admin.ui;

import com.studyshield.admin.service.BackendDataService;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;

import java.util.List;

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
            "quiz-attempts",
            "quiz-results",
            "tv-users",
            "wifi-networks",
            "connected-tvs"
    );

    private final BackendDataService backendDataService;

    public DashboardView(BackendDataService backendDataService) {
        this.backendDataService = backendDataService;
        setPadding(true);
        setSpacing(true);

        add(new H2("StudyShield backend admin"));

        HorizontalLayout stats = new HorizontalLayout(
                statusCard("Backend", "StudyShield modulith"),
                statusCard("API", "http://localhost:8080"),
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

        HorizontalLayout summary = new HorizontalLayout();
        for (String table : TABLES) {
            int count = backendDataService.list(table).size();
            summary.add(statusCard(table, String.valueOf(count)));
        }
        summary.setWidthFull();
        summary.getStyle().set("flex-wrap", "wrap");
        add(summary);
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
