package ca.bc.gov.nrs.csp.backend.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

/**
 * Static accessors for the current authenticated user.
 * Used by services for audit fields and current-user-aware queries.
 */
public final class SecurityContextUtils {

    private SecurityContextUtils() {}

    public static Optional<String> currentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return Optional.empty();
        }
        return Optional.ofNullable(auth.getName());
    }

    public static List<String> currentRoles() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            return List.of();
        }
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();
    }

    /**
     * The forest clients the current user may act for: all of them for staff, the held clients for
     * a Business BCeID submitter. Throws a 403 for anyone else; see {@link ClientScope#from}.
     */
    public static ClientScope currentClientScope() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new org.springframework.security.authentication.AuthenticationCredentialsNotFoundException(
                    "No authenticated user in the current security context.");
        }
        return ClientScope.from(auth.getAuthorities());
    }

    /**
     * Returns the current username or throws if no authenticated user is present.
     * Use in service methods where an authenticated user is required by contract.
     */
    public static String requireUsername() {
        return currentUsername().orElseThrow(() ->
                new org.springframework.security.authentication.AuthenticationCredentialsNotFoundException(
                        "No authenticated user in the current security context."));
    }
}
