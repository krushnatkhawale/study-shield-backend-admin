package com.studyshield.admin.ui;

import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.login.LoginForm;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;

@Route("login")
@PageTitle("StudyShield Admin")
@AnonymousAllowed
public class LoginView extends VerticalLayout implements BeforeEnterObserver {

    private final LoginForm loginForm = new LoginForm();

    public LoginView() {
        addClassName("ss-login");
        setSizeFull();
        setJustifyContentMode(JustifyContentMode.CENTER);
        setDefaultHorizontalComponentAlignment(Alignment.CENTER);

        VerticalLayout card = AdminUi.card();
        card.setWidth("400px");
        H2 title = new H2("StudyShield Admin");
        title.getStyle().set("margin", "0");
        Paragraph subtitle = new Paragraph("Sign in with your admin account to manage curriculum, quizzes and packs.");
        subtitle.addClassName("ss-muted");

        loginForm.setAction("login");
        loginForm.setForgotPasswordButtonVisible(true);
        loginForm.addForgotPasswordListener(e ->
                getUI().ifPresent(ui -> ui.navigate("forgot-password")));
        loginForm.getElement().executeJs("this.$.vaadinLoginUsername.value = $0;", "admin@studyshield.local");

        card.add(title, subtitle, loginForm);
        add(card);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        if (event.getLocation().getQueryParameters().getParameters().containsKey("error")) {
            loginForm.setError(true);
        }
    }
}
