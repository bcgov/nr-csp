package ca.bc.gov.nrs.csp.backend.filter;

import ca.bc.gov.nrs.csp.backend.exception.JwtSigningKeyException;
import ca.bc.gov.nrs.csp.backend.service.JwtService;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtRequestFilterTest {

    @Mock JwtService jwtService;
    @Mock FilterChain filterChain;

    JwtRequestFilter filter;
    MockHttpServletRequest request;
    MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        filter = new JwtRequestFilter(jwtService);
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void noAuthorizationHeader_passesRequestThrough() throws Exception {
        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void nonBearerAuthorizationHeader_passesRequestThrough() throws Exception {
        request.addHeader("Authorization", "Basic dXNlcjpwYXNz");

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void validBearerToken_setsAuthenticationAndPassesThrough() throws Exception {
        request.addHeader("Authorization", "Bearer valid.jwt.token");
        when(jwtService.extractUsername("valid.jwt.token")).thenReturn("TESTUSER");
        when(jwtService.extractAuthorities("valid.jwt.token"))
                .thenReturn(List.of(new SimpleGrantedAuthority("ADMIN")));

        filter.doFilter(request, response, filterChain);

        var auth = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(auth);
        assertEquals("TESTUSER", auth.getName());
        assertTrue(auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ADMIN")));
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void expiredToken_returns401_andHaltsChain() throws Exception {
        request.addHeader("Authorization", "Bearer expired.token");
        when(jwtService.extractUsername("expired.token"))
                .thenThrow(new ExpiredJwtException(null, null, "Token expired"));

        filter.doFilter(request, response, filterChain);

        assertEquals(HttpServletResponse.SC_UNAUTHORIZED, response.getStatus());
        verify(filterChain, never()).doFilter(any(), any());
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void invalidToken_returns401_andHaltsChain() throws Exception {
        request.addHeader("Authorization", "Bearer bad.token");
        when(jwtService.extractUsername("bad.token"))
                .thenThrow(new JwtException("Malformed token"));

        filter.doFilter(request, response, filterChain);

        assertEquals(HttpServletResponse.SC_UNAUTHORIZED, response.getStatus());
        verify(filterChain, never()).doFilter(any(), any());
    }

    @Test
    void signingKeyException_returns401_andHaltsChain() throws Exception {
        request.addHeader("Authorization", "Bearer key.error.token");
        when(jwtService.extractUsername("key.error.token"))
                .thenThrow(new JwtSigningKeyException("Key error", new RuntimeException("cause")));

        filter.doFilter(request, response, filterChain);

        assertEquals(HttpServletResponse.SC_UNAUTHORIZED, response.getStatus());
        verify(filterChain, never()).doFilter(any(), any());
    }

    @Test
    void existingAuthentication_isNotOverwritten() throws Exception {
        request.addHeader("Authorization", "Bearer another.token");
        when(jwtService.extractUsername("another.token")).thenReturn("OTHER");
        // extractAuthorities is NOT called when auth is already set in the context

        var existingAuth = new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                "EXISTING", null, List.of());
        SecurityContextHolder.getContext().setAuthentication(existingAuth);

        filter.doFilter(request, response, filterChain);

        assertEquals("EXISTING", SecurityContextHolder.getContext().getAuthentication().getName());
        verify(filterChain).doFilter(request, response);
    }
}
