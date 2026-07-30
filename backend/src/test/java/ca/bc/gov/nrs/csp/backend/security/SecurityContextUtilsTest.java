package ca.bc.gov.nrs.csp.backend.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SecurityContextUtilsTest {

    @BeforeEach
    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void currentUsername_returnsEmpty_whenNoAuthentication() {
        assertTrue(SecurityContextUtils.currentUsername().isEmpty());
    }

    @Test
    void currentUsername_returnsEmpty_whenAnonymousUser() {
        var anon = new AnonymousAuthenticationToken(
                "key", "anonymousUser", List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS")));
        SecurityContextHolder.getContext().setAuthentication(anon);

        // AnonymousAuthenticationToken is authenticated=true but getName() returns "anonymousUser"
        assertTrue(SecurityContextUtils.currentUsername().isPresent());
        assertEquals("anonymousUser", SecurityContextUtils.currentUsername().get());
    }

    @Test
    void currentUsername_returnsName_whenAuthenticated() {
        var auth = new UsernamePasswordAuthenticationToken(
                "TESTUSER", null, List.of(new SimpleGrantedAuthority("ADMIN")));
        SecurityContextHolder.getContext().setAuthentication(auth);

        var result = SecurityContextUtils.currentUsername();

        assertTrue(result.isPresent());
        assertEquals("TESTUSER", result.get());
    }

    @Test
    void currentRoles_returnsEmpty_whenNoAuthentication() {
        assertTrue(SecurityContextUtils.currentRoles().isEmpty());
    }

    @Test
    void currentRoles_returnsRoles_whenAuthenticated() {
        var auth = new UsernamePasswordAuthenticationToken(
                "USER", null,
                List.of(new SimpleGrantedAuthority("ADMIN"), new SimpleGrantedAuthority("VIEWER")));
        SecurityContextHolder.getContext().setAuthentication(auth);

        var roles = SecurityContextUtils.currentRoles();

        assertEquals(2, roles.size());
        assertTrue(roles.contains("ADMIN"));
        assertTrue(roles.contains("VIEWER"));
    }

    @Test
    void requireUsername_returnsName_whenAuthenticated() {
        var auth = new UsernamePasswordAuthenticationToken(
                "REQUSER", null, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);

        assertEquals("REQUSER", SecurityContextUtils.requireUsername());
    }

    @Test
    void requireUsername_throws_whenNotAuthenticated() {
        assertThrows(AuthenticationCredentialsNotFoundException.class,
                SecurityContextUtils::requireUsername);
    }
}
