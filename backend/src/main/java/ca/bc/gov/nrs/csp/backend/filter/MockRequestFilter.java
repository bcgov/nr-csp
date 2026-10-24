package ca.bc.gov.nrs.csp.backend.filter;

import ca.bc.gov.nrs.csp.backend.security.CspAuthorities;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * Local-dev-only filter that injects a mock authenticated user into the SecurityContext.
 * Active only when {@code auth.mock.enabled=true} (set automatically in the local profile).
 *
 * <p>The mock user's FAM roles go through {@link CspAuthorities}, the same mapping real tokens
 * use, so a mock Business BCeID user gets exactly what a real one would. The defaults come from
 * {@code auth.mock.identity-provider} and {@code auth.mock.roles}; the frontend's mock role
 * selector overrides them per request with {@link #IDENTITY_PROVIDER_HEADER} and
 * {@link #ROLES_HEADER}. That widens nothing: with this filter active every request is already
 * authenticated, as ADMIN by default.
 */
@Component
@ConditionalOnProperty(name = "auth.mock.enabled", havingValue = "true")
public class MockRequestFilter extends OncePerRequestFilter {

    private static final Logger log = LogManager.getLogger(MockRequestFilter.class);

    public static final String IDENTITY_PROVIDER_HEADER = "X-Mock-Identity-Provider";
    public static final String ROLES_HEADER = "X-Mock-Roles";

    /** Bare staff roles, accepted in {@code auth.mock.roles} as shorthand for their FAM codes. */
    private static final Set<String> STAFF_SHORTHAND = Set.of("ADMIN", "APPROVE", "VIEW");

    private final String mockUsername;
    private final String mockIdentityProvider;
    private final String mockRoles;

    public MockRequestFilter(
            @org.springframework.beans.factory.annotation.Value("${auth.mock.username:local-dev-user}") String mockUsername,
            @org.springframework.beans.factory.annotation.Value("${auth.mock.identity-provider:idir}") String mockIdentityProvider,
            @org.springframework.beans.factory.annotation.Value("${auth.mock.roles:ADMIN}") String mockRoles) {
        this.mockUsername = mockUsername;
        this.mockIdentityProvider = mockIdentityProvider.trim();
        this.mockRoles = mockRoles;
        log.warn("MockRequestFilter is active — all requests will be authenticated as '{}' ({}) with roles {} " +
                "unless the request names others. Do NOT enable this in production.",
                mockUsername, this.mockIdentityProvider, authorities(this.mockIdentityProvider, mockRoles));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            String idpHeader = request.getHeader(IDENTITY_PROVIDER_HEADER);
            String rolesHeader = request.getHeader(ROLES_HEADER);
            String identityProvider = idpHeader == null ? mockIdentityProvider : idpHeader.trim();
            String roles = rolesHeader == null ? mockRoles : rolesHeader;
            UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(mockUsername, null, authorities(identityProvider, roles));
            SecurityContextHolder.getContext().setAuthentication(auth);
        }
        chain.doFilter(request, response);
    }

    static List<GrantedAuthority> authorities(String identityProvider, String roles) {
        List<String> famRoles = Arrays.stream(roles.split(","))
                .map(String::trim)
                .filter(role -> !role.isEmpty())
                .map(role -> STAFF_SHORTHAND.contains(role) ? "CSP_" + role : role)
                .toList();
        return CspAuthorities.fromFamRoles(identityProvider, famRoles);
    }
}
