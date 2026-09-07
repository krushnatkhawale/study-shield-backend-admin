package com.studyshield.admin.ui;

import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.RouterLink;

public class MainLayout extends AppLayout {

    public MainLayout() {
        setPrimarySection(Section.NAVBAR);

        H1 title = new H1("StudyShield Admin");
        title.getStyle().set("margin", "0");

        Button logoutButton = new Button("Logout", event -> getUI().ifPresent(ui -> ui.getPage().setLocation("/logout")));
        logoutButton.getStyle().set("margin-left", "auto");

        HorizontalLayout topBar = new HorizontalLayout(new DrawerToggle(), title, logoutButton);
        topBar.setWidthFull();
        topBar.setAlignItems(FlexComponent.Alignment.CENTER);
        topBar.expand(title);
        addToNavbar(topBar);

        VerticalLayout drawer = new VerticalLayout(
                new RouterLink("Dashboard", DashboardView.class),
                new RouterLink("Data browser", SettingsView.class),
                new RouterLink("Class management", ClassGradeManagementView.class),
                new RouterLink("Subject order", SubjectOrderView.class),
                new RouterLink("Quiz management", QuizManagementView.class),
                new RouterLink("Question management", QuestionManagementView.class),
                new RouterLink("User management", UserManagementView.class)
        );
        drawer.setPadding(false);
        drawer.setSpacing(false);
        addToDrawer(drawer);
    }
}
