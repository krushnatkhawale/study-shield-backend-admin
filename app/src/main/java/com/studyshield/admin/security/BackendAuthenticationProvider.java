package com.studyshield.admin.security;

import com.studyshield.admin.service.BackendDataService;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Resolves admin logins against the backend via
 * {@code POST /api/auth/admin-signin}. Returns a simple
 * {@link UsernamePasswordAuthenticationToken} with the backend admin principal and a single
 * ROLE_ADMIN authority.
 */
@Component
public class BackendAuthenticationProvider implements AuthenticationProvider {

    private final BackendDataService backendDataService;

    public BackendAuthenticationProvider(BackendDataService backendDataService) {
        this.backendDataService = backendDataService;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String loginId = String.valueOf(authentication.getPrincipal());
        String password = String.valueOf(authentication.getCredentials());

        BackendDataService.AuthenticatedAdmin admin =
                backendDataService.authenticateAdmin(loginId, password);

        List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_ADMIN"));
        return new UsernamePasswordAuthenticationToken(admin, null, authorities);
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }
}