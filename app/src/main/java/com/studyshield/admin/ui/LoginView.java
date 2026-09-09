package com.studyshield.admin.ui;

import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.login.LoginForm;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;

@Route("login")
@PageTitle("Login")
@AnonymousAllowed
public class LoginView extends VerticalLayout implements BeforeEnterObserver {

    private final LoginForm loginForm = new LoginForm();

    public LoginView() {
        setSizeFull();
        setPadding(true);
        setSpacing(false);
        setJustifyContentMode(JustifyContentMode.CENTER);
        setDefaultHorizontalComponentAlignment(Alignment.CENTER);

        VerticalLayout card = new VerticalLayout();
        card.setWidth("360px");
        card.setPadding(true);
        card.setSpacing(false);
        card.setAlignItems(Alignment.STRETCH);
        card.getStyle().set("background", "var(--lumo-base-color)");
        card.getStyle().set("border-radius", "0.75rem");
        card.getStyle().set("box-shadow", "0 2px 8px rgba(0, 0, 0, 0.15)");
        card.getStyle().set("padding", "2rem");

        H2 title = new H2("StudyShield Admin");
        title.getStyle().set("margin", "0 0 1rem 0");
        title.getStyle().set("text-align", "center");

        loginForm.setAction("login");
        loginForm.setForgotPasswordButtonVisible(true);
        loginForm.addForgotPasswordListener(e ->
                getUI().ifPresent(ui -> ui.navigate("forgot-password")));

        card.add(title, loginForm);
        add(card);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        if (event.getLocation().getQueryParameters().getParameters().containsKey("error")) {
            loginForm.setError(true);
        }
    }
}
