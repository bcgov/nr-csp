package ca.bc.gov.nrs.csp.backend.security;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Reads the scope FAM encodes into a CSS role name.
 *
 * <p>A CSS role is a bare name, so when FAM grants a role for a forest client it creates and
 * assigns {@code <CODE>_FOREST_CLIENT-<number>}; the bare code never reaches the token. Ported
 * from {@code CssRoleNaming.parse} in nr-fam, which is the reference implementation.
 */
public final class CssRoleNaming {

    /** Prefix of FAM's bookkeeping roles (e.g. {@code FAM:EXPIRES:...}), which grant nothing. */
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
     * Splits a role name into its base role and every scope suffix, in written order.
     *
     * <p>Suffixes are stripped from the right, one at a time: the value is everything after the
     * last {@code -}, and it is a scope only if what precedes it ends in a known scope type.
     * Splitting on the last underscore would read {@code FOREST_CLIENT} as {@code CLIENT}.
     *
     * <pre>
     * CSP_SUBMITTER_FOREST_CLIENT-00001018  -> (CSP_SUBMITTER, [FOREST_CLIENT=00001018])
     * CSP_ADMIN                             -> (CSP_ADMIN, [])
     * SOME-ROLE                             -> (SOME-ROLE, [])
     * </pre>
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

    /** One dimension a role is scoped by, e.g. {@code FOREST_CLIENT} of {@code 00001018}. */
    public record Scope(String type, String value) {}

    /** The base role and every scope its name carries; {@code scopes} is empty when unscoped. */
    public record ScopedRoleName(String baseRoleName, List<Scope> scopes) {}
}
