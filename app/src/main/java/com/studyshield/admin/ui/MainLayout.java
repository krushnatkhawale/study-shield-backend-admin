package com.studyshield.admin.ui;

import com.studyshield.admin.service.BackendDataService;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;
import com.vaadin.flow.theme.lumo.Lumo;

public class MainLayout extends AppLayout {

    private final BackendDataService backendDataService;

    public MainLayout(BackendDataService backendDataService) {
        this.backendDataService = backendDataService;
        setPrimarySection(Section.DRAWER);
        createHeader();
        createDrawer();
    }

    private void createHeader() {
        H1 title = new H1("StudyShield");
        title.addClassName("ss-shell-title");

        Select<String> environment = new Select<>();
        environment.setLabel("Environment");
        environment.setItems(backendDataService.getEnvironments());
        environment.setValue(backendDataService.getEnvironment());
        environment.addValueChangeListener(e -> {
            String selected = e.getValue();
            if (selected != null && !selected.equals(backendDataService.getEnvironment())) {
                backendDataService.setEnvironment(selected);
                Notification.show("Now operating against the " + selected.toUpperCase() + " environment");
            }
        });
        environment.setWidth("9em");

        Button theme = new Button(VaadinIcon.MOON_O.create());
        theme.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        theme.getElement().setAttribute("aria-label", "Toggle theme");
        theme.addClickListener(e -> getUI().ifPresent(ui -> {
            var themes = ui.getElement().getThemeList();
            if (themes.contains(Lumo.DARK)) {
                themes.remove(Lumo.DARK);
                theme.setIcon(VaadinIcon.MOON_O.create());
            } else {
                themes.add(Lumo.DARK);
                theme.setIcon(VaadinIcon.SUN_O.create());
            }
        }));

        Button logout = new Button("Sign out", VaadinIcon.SIGN_OUT.create());
        logout.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        logout.addClickListener(e -> getUI().ifPresent(ui -> ui.getPage().setLocation("/logout")));

        HorizontalLayout header = new HorizontalLayout(new DrawerToggle(), title, environment, theme, logout);
        header.setWidthFull();
        header.setAlignItems(FlexComponent.Alignment.CENTER);
        header.expand(title);
        header.getStyle().set("padding-right", "var(--lumo-space-m)");
        addToNavbar(header);
    }

    private void createDrawer() {
        SideNav catalog = new SideNav();
        catalog.setLabel("Catalog");
        catalog.addItem(
                new SideNavItem("Boards", BoardManagementView.class, VaadinIcon.INSTITUTION.create()),
                new SideNavItem("Board classes", BoardClassManagementView.class, VaadinIcon.ACADEMY_CAP.create()),
                new SideNavItem("Subjects", SubjectManagementView.class, VaadinIcon.BOOK.create()),
                new SideNavItem("Offerings", OfferingManagementView.class, VaadinIcon.LINK.create())
        );

        SideNav content = new SideNav();
        content.setLabel("Content");
        content.addItem(
                new SideNavItem("Packs", PackManagementView.class, VaadinIcon.PACKAGE.create()),
                new SideNavItem("Quizzes", QuizManagementView.class, VaadinIcon.LIST.create()),
                new SideNavItem("Question bank", QuestionLibraryView.class, VaadinIcon.QUESTION.create())
        );

        SideNav ops = new SideNav();
        ops.setLabel("Operations");
        ops.addItem(
                new SideNavItem("Home", DashboardView.class, VaadinIcon.DASHBOARD.create()),
                new SideNavItem("Users", UserManagementView.class, VaadinIcon.USERS.create()),
                new SideNavItem("Advanced data", SettingsView.class, VaadinIcon.DATABASE.create())
        );

        VerticalLayout drawer = new VerticalLayout(brand(), catalog, content, ops);
        drawer.setPadding(true);
        drawer.setSpacing(true);
        drawer.setSizeFull();
        addToDrawer(drawer);
    }

    private VerticalLayout brand() {
        Span name = new Span("Admin console");
        name.getStyle().set("font-weight", "700");
        Span hint = new Span("Curriculum & packs");
        hint.addClassName("ss-muted");
        VerticalLayout brand = new VerticalLayout(name, hint);
        brand.setPadding(false);
        brand.setSpacing(false);
        return brand;
    }
}
