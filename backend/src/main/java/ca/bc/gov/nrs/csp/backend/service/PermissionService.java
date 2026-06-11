package ca.bc.gov.nrs.csp.backend.service;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;

import java.util.Set;

import static ca.bc.gov.nrs.csp.backend.util.constants.PermissionConstants.ROLE_PERMISSIONS;

/**
 * Evaluates action-level permissions against {@link ca.bc.gov.nrs.csp.backend.util.constants.PermissionConstants#ROLE_PERMISSIONS}.
 *
 * <p>Used in {@code @PreAuthorize} SpEL expressions on controller methods:
 * <pre>
 *   {@literal @}PreAuthorize("@permissionService.hasPermission(authentication, 'invoiceDetails/Approve')")
 * </pre>
 */
@Service
public class PermissionService {

    /**
     * Returns {@code true} if the authenticated user's role grants the given action.
     *
     * @param auth   the current Spring Security authentication (may be {@code null} for safety)
     * @param action an action string from {@link ca.bc.gov.nrs.csp.backend.util.constants.PermissionConstants}
     */
    public boolean hasPermission(Authentication auth, String action) {
        if (auth == null) return false;
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(role -> {
                    Set<String> actions = ROLE_PERMISSIONS.getOrDefault(role, Set.of());
                    return actions.contains(action);
                });
    }
}
