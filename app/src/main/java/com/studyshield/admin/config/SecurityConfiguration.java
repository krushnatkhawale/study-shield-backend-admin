package com.studyshield.admin.config;

import com.studyshield.admin.security.BackendAuthenticationProvider;
import com.vaadin.flow.spring.security.VaadinWebSecurity;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@Configuration
public class SecurityConfiguration extends VaadinWebSecurity {

    private final BackendAuthenticationProvider backendAuthenticationProvider;

    public SecurityConfiguration(BackendAuthenticationProvider backendAuthenticationProvider) {
        this.backendAuthenticationProvider = backendAuthenticationProvider;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public UserDetailsService userDetailsService(PasswordEncoder passwordEncoder) {
        // Placeholder so Spring Boot auto-config does not supply an in-memory
        // default. Real authentication is handled entirely by
        // BackendAuthenticationProvider; form-login credentials go straight
        // to that provider.
        return new InMemoryUserDetailsManager();
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(auth -> auth
                .requestMatchers(
                        new AntPathRequestMatcher("/login"),
                        new AntPathRequestMatcher("/VAADIN/**"),
                        new AntPathRequestMatcher("/favicon.ico"),
                        new AntPathRequestMatcher("/h2-console/**"),
                        new AntPathRequestMatcher("/error")
                ).permitAll()
        );

        http.formLogin(form -> form
                .loginPage("/login")
                .permitAll()
                .defaultSuccessUrl("/dashboard", true)
        );

        http.logout(logout -> logout
                .logoutSuccessUrl("/login?logout")
                .permitAll());

        http.headers(headers -> headers.frameOptions(frameOptions -> frameOptions.sameOrigin()));
        http.csrf(csrf -> csrf.ignoringRequestMatchers(new AntPathRequestMatcher("/h2-console/**")));

        super.configure(http);
    }
}
