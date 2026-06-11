package ca.bc.gov.nrs.csp.backend.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * Local-dev-only filter that injects a mock authenticated user into the SecurityContext.
 * Active only when {@code auth.mock.enabled=true} (set automatically in the local profile).
 */
@Component
@ConditionalOnProperty(name = "auth.mock.enabled", havingValue = "true")
public class MockRequestFilter extends OncePerRequestFilter {

    private static final Logger log = LogManager.getLogger(MockRequestFilter.class);

    private final String mockUsername;
    private final List<SimpleGrantedAuthority> mockAuthorities;

    public MockRequestFilter(
            @org.springframework.beans.factory.annotation.Value("${auth.mock.username:local-dev-user}") String mockUsername,
            @org.springframework.beans.factory.annotation.Value("${auth.mock.roles:ADMIN}") String mockRoles) {
        this.mockUsername = mockUsername;
        this.mockAuthorities = Arrays.stream(mockRoles.split(","))
                .map(String::trim)
                .map(SimpleGrantedAuthority::new)
                .toList();
        log.warn("MockRequestFilter is active — all requests will be authenticated as '{}' with roles {}. " +
                "Do NOT enable this in production.", mockUsername, mockRoles);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(mockUsername, null, mockAuthorities);
            SecurityContextHolder.getContext().setAuthentication(auth);
        }
        chain.doFilter(request, response);
    }
}
