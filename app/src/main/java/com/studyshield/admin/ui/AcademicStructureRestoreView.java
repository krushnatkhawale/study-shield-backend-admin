package com.studyshield.admin.ui;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.studyshield.admin.service.BackendDataService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MemoryBuffer;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.StreamResource;
import jakarta.annotation.security.RolesAllowed;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * Guided restore flow for the academic structure (GitHub JSON is the single source
 * of truth): backup/download → human commits the JSON and re-pins the ref → purge
 * (confirm required) → load from the pinned ref or an uploaded file → count report.
 */
@Route(value = "academic-restore", layout = MainLayout.class)
@PageTitle("Academic restore")
@RolesAllowed("ADMIN")
public class AcademicStructureRestoreView extends VerticalLayout {

    private static final List<String> SCOPES = List.of(
            "all", "class-levels", "countries", "boards", "board-classes", "subjects", "offerings");

    private final BackendDataService backendDataService;
    private final ObjectMapper objectMapper;

    private final Select<String> scope = new Select<>();
    private final Select<String> loadSource = new Select<>();
    private final Checkbox dryRun = new Checkbox("Dry run (plan only, no rows written)");
    private final TextArea resultLog = new TextArea();
    private final Grid<Map<String, Object>> statusGrid = new Grid<>();
    private final Paragraph sourceLine = new Paragraph();

    private final Anchor downloadAnchor = new Anchor();
    private final Upload upload;
    private byte[] uploadedJson;

    public AcademicStructureRestoreView(BackendDataService backendDataService, ObjectMapper objectMapper) {
        this.backendDataService = backendDataService;
        this.objectMapper = objectMapper;
        setSizeFull();
        setPadding(false);

        scope.setItems(SCOPES);
        scope.setValue("all");
        scope.setLabel("Scope");
        scope.setWidth("200px");
        scope.addValueChangeListener(e -> prepareDownload());

        loadSource.setItems("GitHub (pinned ref)", "Upload JSON file");
        loadSource.setValue("GitHub (pinned ref)");
        loadSource.setLabel("Load from");
        loadSource.setWidth("220px");

        MemoryBuffer buffer = new MemoryBuffer();
        upload = new Upload(buffer);
        upload.setAcceptedFileTypes("application/json", ".json");
        upload.setMaxFiles(1);
        upload.addFinishedListener(event -> {
            try {
                uploadedJson = buffer.getInputStream().readAllBytes();
            } catch (Exception ex) {
                uploadedJson = null;
                Notification.show("Could not read the uploaded file: " + ex.getMessage());
            }
        });

        downloadAnchor.getElement().setAttribute("download", true);
        downloadAnchor.setVisible(false);

        VerticalLayout page = AdminUi.page("Academic structure restore",
                "GitHub JSON is the source of truth. Backup first, purge second, load last.",
                new Button("Refresh status", e -> refreshStatus()));

        VerticalLayout statusCard = AdminUi.card(sourceLine, statusGrid);
        VerticalLayout backupCard = AdminUi.card(
                new H3("1 · Backup"),
                new Paragraph("Download the current structure as load-schema JSON. Commit the files "
                        + "to academic-structure/ in the backend repo and push (respect the no-push "
                        + "window). If you changed the branch/tag, re-pin ACADEMIC_DATA_REF before loading."),
                downloadAnchor);
        VerticalLayout purgeCard = AdminUi.card(
                new H3("2 · Purge"),
                new Paragraph("Rows only — never tables, never the database. Child references refuse "
                        + "the purge (409); purge 'all' walks the content chain first, then the "
                        + "structure in FK order."),
                buildPurgeButton());
        VerticalLayout loadCard = AdminUi.card(
                new H3("3 · Load"),
                new Paragraph("Initialize-only: an already-populated table refuses the load (409). "
                        + "File ids are preserved; identity sequences are bumped after the load."),
                loadSource, upload, dryRun, buildLoadButton());

        resultLog.setLabel("Result");
        resultLog.setReadOnly(true);
        resultLog.setWidthFull();
        resultLog.setHeight("180px");

        statusGrid.addColumn(row -> AdminUi.str(row, "entity")).setHeader("Entity").setAutoWidth(true);
        statusGrid.addColumn(row -> AdminUi.str(row, "rowCount")).setHeader("Rows").setAutoWidth(true);
        statusGrid.setSizeFull();
        statusGrid.setMinHeight("180px");

        page.add(statusCard, backupCard, purgeCard, loadCard, resultLog);
        page.setFlexGrow(1, loadCard);
        add(page);
        setFlexGrow(1, page);

        refreshStatus();
        prepareDownload();
    }

    /* ----------------------------------------------------------------- status */

    private void refreshStatus() {
        try {
            Map<String, Object> status = backendDataService.academicStructureStatus();
            sourceLine.setText("Source: " + status.getOrDefault("source", "?")
                    + "  ·  Repo: " + status.getOrDefault("repo", "?")
                    + "  ·  Ref: " + status.getOrDefault("ref", "(unset)"));
            List<Map<String, Object>> entities = objectMapper.convertValue(
                    status.get("entities"), new TypeReference<List<Map<String, Object>>>() {});
            statusGrid.setItems(entities == null ? List.of() : entities);
        } catch (Exception ex) {
            sourceLine.setText("Status unavailable: " + ex.getMessage());
            statusGrid.setItems();
        }
    }

    /* ----------------------------------------------------------------- backup */

    /**
     * Fetches the export JSON for the current scope and points the download anchor at it.
     * The anchor is inert until this runs, so no stale bytes can leak across scopes.
     */
    private void prepareDownload() {
        try {
            String json = backendDataService.academicStructureExport(scope.getValue());
            String fileName = "all".equals(scope.getValue())
                    ? "academic-structure-all.json" : scope.getValue() + ".json";
            StreamResource resource = new StreamResource(fileName,
                    () -> new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)));
            resource.setContentType("application/json");
            downloadAnchor.setHref(resource);
            downloadAnchor.setText("Download " + fileName + " (" + json.length() + " chars)");
            downloadAnchor.setVisible(true);
            log("Export fetched: " + fileName + ". Commit it to the repo, then purge + load.");
        } catch (Exception ex) {
            downloadAnchor.setVisible(false);
            Notification.show("Export failed: " + ex.getMessage());
            log("Export failed: " + ex.getMessage());
        }
    }

    /* ------------------------------------------------------------------ purge */

    private Button buildPurgeButton() {
        Button purge = AdminUi.danger("Purge rows");
        purge.addClickListener(e -> {
            String target = scope.getValue();
            if ("all".equals(target)) {
                AdminUi.confirmDelete("Purge everything?",
                        "This deletes the whole academic structure AND the content chain "
                                + "(questions, quizzes, packs, bundles). Content reseeds at the "
                                + "next startup. Download a backup first.",
                        () -> runPurge(target));
            } else {
                AdminUi.confirmDelete("Purge " + target + "?",
                        "Deletes all " + target + " rows. The purge is refused while child rows "
                                + "still reference them — download a backup first.",
                        () -> runPurge(target));
            }
        });
        return purge;
    }

    private void runPurge(String target) {
        try {
            Map<String, Object> report = backendDataService.academicStructurePurge(target);
            log("Purge " + target + ": " + pretty(report));
            refreshStatus();
        } catch (Exception ex) {
            log("Purge failed (" + target + "): " + ex.getMessage());
            Notification.show("Purge failed: " + ex.getMessage());
        }
    }

    /* ------------------------------------------------------------------- load */

    private Button buildLoadButton() {
        Button load = AdminUi.primary("Load");
        load.addClickListener(e -> {
            String target = scope.getValue();
            String bodyJson = null;
            if ("Upload JSON file".equals(loadSource.getValue())) {
                if ("all".equals(target)) {
                    Notification.show("Uploaded files load one entity at a time — pick a specific scope");
                    return;
                }
                if (uploadedJson == null) {
                    Notification.show("Choose a JSON file first");
                    return;
                }
                bodyJson = new String(uploadedJson, StandardCharsets.UTF_8);
            }
            runLoad(target, dryRun.getValue(), bodyJson);
        });
        return load;
    }

    private void runLoad(String target, boolean dryRunFlag, String bodyJson) {
        try {
            Map<String, Object> report =
                    backendDataService.academicStructureLoad(target, dryRunFlag, bodyJson);
            log((dryRunFlag ? "Load (dry run) " : "Load ") + target + ": " + pretty(report));
            refreshStatus();
        } catch (Exception ex) {
            log("Load failed (" + target + "): " + ex.getMessage());
            Notification.show("Load failed: " + ex.getMessage());
        }
    }

    /* ------------------------------------------------------------------ utils */

    private void log(String line) {
        resultLog.setValue(line + "\n" + resultLog.getValue());
    }

    private String pretty(Object value) {
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(value);
        } catch (Exception ex) {
            return String.valueOf(value);
        }
    }
}
