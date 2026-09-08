package com.studyshield.admin;

import com.studyshield.admin.security.BackendAuthenticationProvider;
import com.studyshield.admin.service.BackendDataService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@SpringBootTest
class SecurityAccessTests {

    @Autowired
    private SecurityFilterChain securityFilterChain;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private BackendAuthenticationProvider backendAuthenticationProvider;

    @MockBean
    private BackendDataService backendDataService;

    @Test
    void securityFilterChainIsConfigured() {
        assertThat(securityFilterChain).isNotNull();
    }

    @Test
    void passwordEncoderIsBCrypt() {
        String encoded = passwordEncoder.encode("some-pass");
        assertThat(encoded).startsWith("$2");
        assertThat(passwordEncoder.matches("some-pass", encoded)).isTrue();
    }

    @Test
    void loginResolvesValidAdminCredentialsAgainstBackend() {
        BackendDataService.AuthenticatedAdmin admin =
                new BackendDataService.AuthenticatedAdmin("7", "admin@studyshield.local", "jwt-token");
        when(backendDataService.authenticateAdmin(anyString(), anyString())).thenReturn(admin);

        Authentication result = backendAuthenticationProvider.authenticate(
                new UsernamePasswordAuthenticationToken("admin@studyshield.local", "secret"));

        assertThat(result.isAuthenticated()).isTrue();
        assertThat(result.getPrincipal()).isEqualTo(admin);
        assertThat(result.getAuthorities()).extracting(auth -> auth.getAuthority())
                .contains("ROLE_ADMIN");
    }

    @Test
    void loginRejectsInvalidAdminCredentials() {
        when(backendDataService.authenticateAdmin(anyString(), anyString()))
                .thenThrow(new BadCredentialsException("Invalid login ID or password"));

        assertThatThrownBy(() -> backendAuthenticationProvider.authenticate(
                new UsernamePasswordAuthenticationToken("admin@studyshield.local", "wrong")))
                .isInstanceOf(BadCredentialsException.class);
    }
}
