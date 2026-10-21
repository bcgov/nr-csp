package ca.bc.gov.nrs.csp.backend.security;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Reads the scope FAM encodes into a CSS role name, e.g. CSP_SUBMITTER_FOREST_CLIENT-00001018.
 * Ported from CssRoleNaming.parse in nr-fam.
 */
public final class CssRoleNaming {

    /** Prefix of FAM's bookkeeping roles, e.g. FAM:EXPIRES:..., which grant nothing. */
    public static final String FAM_SIDECAR_PREFIX = "FAM:";

    public static final String SCOPE_DISTRICT = "DISTRICT";
    public static final String SCOPE_REGION = "REGION";
    public static final String SCOPE_FOREST_CLIENT = "FOREST_CLIENT";

    // Longest first, so FOREST_CLIENT is never read as a CLIENT scope of a *_FOREST base.
    private static final List<String> SCOPE_TYPES =
            List.of(SCOPE_FOREST_CLIENT, SCOPE_DISTRICT, SCOPE_REGION);

    private CssRoleNaming() {}

    public static boolean isSidecarRole(String roleName) {
        return roleName != null && roleName.startsWith(FAM_SIDECAR_PREFIX);
    }

    /**
     * Splits a role name into its base role and scopes, in written order. Scopes are stripped from
     * the right; splitting on the last underscore would misread FOREST_CLIENT as CLIENT.
     */
    public static ScopedRoleName parse(String roleName) {
        List<Scope> scopes = new ArrayList<>();
        String remaining = roleName;
        while (true) {
            Optional<ScopeSplit> split = stripOneScope(remaining);
            if (split.isEmpty()) {
                break;
            }
            scopes.add(split.get().scope());
            remaining = split.get().head();
        }
        Collections.reverse(scopes);
        return new ScopedRoleName(remaining, List.copyOf(scopes));
    }

    private static Optional<ScopeSplit> stripOneScope(String roleName) {
        int hyphen = roleName.lastIndexOf('-');
        if (hyphen < 0) {
            return Optional.empty();
        }
        String head = roleName.substring(0, hyphen);
        String value = roleName.substring(hyphen + 1);
        for (String scopeType : SCOPE_TYPES) {
            String suffix = "_" + scopeType;
            if (head.endsWith(suffix) && head.length() > suffix.length()) {
                return Optional.of(new ScopeSplit(
                        head.substring(0, head.length() - suffix.length()), new Scope(scopeType, value)));
            }
        }
        // A hyphen that is not a scope separator: the name is its own base role.
        return Optional.empty();
    }

    private record ScopeSplit(String head, Scope scope) {}

    /** One scope, e.g. FOREST_CLIENT 00001018. */
    public record Scope(String type, String value) {}

    /** A base role and its scopes; scopes is empty for an unscoped role. */
    public record ScopedRoleName(String baseRoleName, List<Scope> scopes) {}
}
