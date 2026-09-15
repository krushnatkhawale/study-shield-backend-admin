package com.studyshield.admin.ui;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import java.util.Map;

public final class AdminUi {

    private AdminUi() {}

    public static VerticalLayout page(String title, String subtitle, Component... toolbar) {
        VerticalLayout page = new VerticalLayout();
        page.addClassName("ss-page");
        page.setPadding(false);
        page.setSpacing(true);
        page.setSizeFull();

        H2 heading = new H2(title);
        Paragraph hint = new Paragraph(subtitle);
        hint.addClassName("ss-muted");

        VerticalLayout text = new VerticalLayout(heading, hint);
        text.setPadding(false);
        text.setSpacing(false);

        HorizontalLayout header = new HorizontalLayout();
        header.addClassName("ss-page-header");
        header.setWidthFull();
        header.setAlignItems(FlexComponent.Alignment.CENTER);
        header.add(text);
        if (toolbar.length > 0) {
            HorizontalLayout actions = new HorizontalLayout(toolbar);
            actions.setSpacing(true);
            header.add(actions);
            header.setFlexGrow(1, text);
        }
        page.add(header);
        return page;
    }

    public static VerticalLayout card(Component... children) {
        VerticalLayout card = new VerticalLayout(children);
        card.addClassName("ss-card");
        card.setPadding(true);
        card.setSpacing(true);
        card.setWidthFull();
        return card;
    }

    public static Div kpi(String label, String value) {
        Div box = new Div();
        box.addClassName("ss-kpi");
        Div l = new Div();
        l.setText(label);
        l.addClassName("label");
        Div v = new Div();
        v.setText(value);
        v.addClassName("value");
        box.add(l, v);
        return box;
    }

    public static void confirmDelete(String title, String message, Runnable onConfirm) {
        ConfirmDialog dialog = new ConfirmDialog();
        dialog.setHeader(title);
        dialog.setText(message);
        dialog.setCancelable(true);
        dialog.setConfirmText("Delete");
        dialog.setConfirmButtonTheme("error primary");
        dialog.addConfirmListener(e -> onConfirm.run());
        dialog.open();
    }

    public static String str(Map<String, Object> row, String key) {
        if (row == null || row.get(key) == null) {
            return "";
        }
        return String.valueOf(row.get(key));
    }

    public static Long id(Map<String, Object> row) {
        if (row == null || row.get("id") == null) {
            return null;
        }
        try {
            return Long.valueOf(String.valueOf(row.get("id")));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    public static String label(Map<String, Object> row, String... keys) {
        if (row == null) {
            return "?";
        }
        for (String key : keys) {
            String value = str(row, key);
            if (!value.isBlank() && !"null".equals(value)) {
                return value;
            }
        }
        return "?";
    }

    public static Button primary(String text) {
        Button button = new Button(text);
        button.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        return button;
    }

    public static Button danger(String text) {
        Button button = new Button(text);
        button.addThemeVariants(ButtonVariant.LUMO_ERROR);
        return button;
    }

    /** Canonical entity dialog: modal, 560px / 95vw max, with the form as content. */
    public static Dialog entityDialog(String title, Component form) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle(title);
        dialog.setModal(true);
        dialog.setDraggable(false);
        dialog.setWidth("560px");
        dialog.setMaxWidth("95vw");
        dialog.add(form);
        return dialog;
    }

    /** Canonical form: full-width, 1 column under 480px, 2 columns above. */
    public static FormLayout entityForm(Component... fields) {
        FormLayout form = new FormLayout(fields);
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1, FormLayout.ResponsiveStep.LabelsPosition.TOP),
                new FormLayout.ResponsiveStep("480px", 2, FormLayout.ResponsiveStep.LabelsPosition.TOP));
        form.setWidthFull();
        return form;
    }

    /** Canonical footer: Delete + Save + Cancel, right-aligned, ESC cancels. */
    public static HorizontalLayout dialogFooter(Dialog dialog, Button delete, Button save) {
        Button cancel = new Button("Cancel", e -> dialog.close());
        cancel.addClickShortcut(Key.ESCAPE);
        HorizontalLayout footer = new HorizontalLayout(delete, save, cancel);
        footer.setWidthFull();
        footer.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
        footer.setAlignItems(FlexComponent.Alignment.CENTER);
        dialog.getFooter().add(footer);
        return footer;
    }
}
