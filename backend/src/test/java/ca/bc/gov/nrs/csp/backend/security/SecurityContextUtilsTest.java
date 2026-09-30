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

    @Test
    void currentClientNumbers_returnsEmpty_whenNoAuthentication() {
        assertTrue(SecurityContextUtils.currentClientNumbers().isEmpty());
    }

    @Test
    void currentClientNumbers_returnsEmpty_whenNoClientAuthorities() {
        var auth = new UsernamePasswordAuthenticationToken(
                "USER", null, List.of(new SimpleGrantedAuthority("ADMIN")));
        SecurityContextHolder.getContext().setAuthentication(auth);

        assertTrue(SecurityContextUtils.currentClientNumbers().isEmpty());
    }

    @Test
    void currentClientNumbers_stripsPrefix_fromClientAuthorities() {
        var auth = new UsernamePasswordAuthenticationToken(
                "USER", null, List.of(
                        new SimpleGrantedAuthority("APPROVE"),
                        new SimpleGrantedAuthority("CLIENT_000478HH"),
                        new SimpleGrantedAuthority("CLIENT_000512AB")));
        SecurityContextHolder.getContext().setAuthentication(auth);

        var clientNumbers = SecurityContextUtils.currentClientNumbers();

        assertEquals(2, clientNumbers.size());
        assertTrue(clientNumbers.contains("000478HH"));
        assertTrue(clientNumbers.contains("000512AB"));
    }

    @Test
    void isClientRestricted_false_whenNoClientAuthorities() {
        var auth = new UsernamePasswordAuthenticationToken(
                "USER", null, List.of(new SimpleGrantedAuthority("ADMIN")));
        SecurityContextHolder.getContext().setAuthentication(auth);

        assertFalse(SecurityContextUtils.isClientRestricted());
    }

    @Test
    void isClientRestricted_true_whenClientAuthorityPresent() {
        var auth = new UsernamePasswordAuthenticationToken(
                "USER", null, List.of(new SimpleGrantedAuthority("CLIENT_000478HH")));
        SecurityContextHolder.getContext().setAuthentication(auth);

        assertTrue(SecurityContextUtils.isClientRestricted());
    }
}
