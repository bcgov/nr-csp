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

    private static final String CLIENT_AUTHORITY_PREFIX = "CLIENT_";

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
     * Client numbers this caller is scoped to via FAM's contextual Cognito
     * groups (see JwtService#extractAuthorities), or empty when unrestricted
     * (every IDIR user today, and any BCeID user FAM hasn't scoped).
     */
    public static List<String> currentClientNumbers() {
        return currentRoles().stream()
                .filter(a -> a.startsWith(CLIENT_AUTHORITY_PREFIX))
                .map(a -> a.substring(CLIENT_AUTHORITY_PREFIX.length()))
                .toList();
    }

    /** Whether this caller is scoped to specific client number(s) rather than unrestricted. */
    public static boolean isClientRestricted() {
        return !currentClientNumbers().isEmpty();
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
