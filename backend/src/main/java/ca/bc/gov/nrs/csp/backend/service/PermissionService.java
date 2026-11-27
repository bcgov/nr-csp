package ca.bc.gov.nrs.csp.backend.service;

import ca.bc.gov.nrs.csp.backend.util.constants.ConstantsCode;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Set;

import static ca.bc.gov.nrs.csp.backend.util.constants.PermissionConstants.INVOICE_DETAILS_APPROVE;
import static ca.bc.gov.nrs.csp.backend.util.constants.PermissionConstants.INVOICE_DETAILS_CANCEL;
import static ca.bc.gov.nrs.csp.backend.util.constants.PermissionConstants.INVOICE_DETAILS_REJECT;
import static ca.bc.gov.nrs.csp.backend.util.constants.PermissionConstants.INVOICE_DETAILS_UNAPPROVE;
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

    private static final Map<String, String> STATUS_CHANGE_PERMISSIONS = Map.of(
            ConstantsCode.INVENTRYSTATUS_APPROVED, INVOICE_DETAILS_APPROVE,
            ConstantsCode.INVENTRYSTATUS_REJECTED, INVOICE_DETAILS_REJECT,
            ConstantsCode.INVENTRYSTATUS_CANCELLED, INVOICE_DETAILS_CANCEL,
            ConstantsCode.INVENTRYSTATUS_UNAPPROVED, INVOICE_DETAILS_UNAPPROVE);

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
                .anyMatch(role -> roleGrants(role, action));
    }

    // Each target status needs the permission for that one decision.
    public boolean canChangeInvoiceStatusTo(Authentication auth, String targetStatus) {
        String action = targetStatus == null ? null : STATUS_CHANGE_PERMISSIONS.get(targetStatus);
        return action != null && hasPermission(auth, action);
    }

    static boolean roleGrants(String role, String action) {
        return ROLE_PERMISSIONS.getOrDefault(role, Set.of()).contains(action);
    }
}
