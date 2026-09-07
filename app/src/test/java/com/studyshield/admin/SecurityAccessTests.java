package com.studyshield.admin;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class SecurityAccessTests {

    @Autowired
    private UserDetailsService userDetailsService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private SecurityFilterChain securityFilterChain;

    @Test
    void adminUserExists() {
        UserDetails admin = userDetailsService.loadUserByUsername("admin");

        assertThat(admin.getUsername()).isEqualTo("admin");
        assertThat(admin.getAuthorities()).isNotEmpty();
    }

    @Test
    void passwordIsEncoded() {
        UserDetails admin = userDetailsService.loadUserByUsername("admin");

        assertThat(passwordEncoder.matches("admin123", admin.getPassword())).isTrue();
    }

    @Test
    void securityFilterChainIsConfigured() {
        assertThat(securityFilterChain).isNotNull();
    }
}
