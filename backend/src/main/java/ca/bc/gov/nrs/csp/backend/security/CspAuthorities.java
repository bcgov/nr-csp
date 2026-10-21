package ca.bc.gov.nrs.csp.backend.security;

import ca.bc.gov.nrs.csp.backend.util.constants.Roles;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

/**
 * Maps an identity provider and its FAM role codes to CSP authorities, for real tokens and the
 * mock user alike. IDIR gets ADMIN, APPROVE or VIEW from CSP_ADMIN, CSP_APPROVE or CSP_VIEW.
 * Business BCeID gets SUBMITTER plus one forest-client authority per 8-digit
 * CSP_SUBMITTER_FOREST_CLIENT-00012345 role. Anything else gets nothing. Codes are trimmed and
 * upper-cased first, as in LEXIS.
 */
public final class CspAuthorities {

    public static final Set<String> STAFF_PROVIDERS = Set.of("idir", "azureidir");
    public static final String BCEID_BUSINESS = "bceidbusiness";

    /** The industry role, scoped per forest client in FAM. */
    public static final String FAM_SUBMITTER = "CSP_SUBMITTER";

    private static final List<String> STAFF_ROLES = List.of(Roles.ADMIN, Roles.APPROVE, Roles.VIEW);
    private static final Pattern CLIENT_NUMBER = Pattern.compile("[0-9]{8}");

    private CspAuthorities() {}

    public static List<GrantedAuthority> fromFamRoles(String identityProvider, Collection<String> famRoles) {
        List<String> roles = famRoles.stream()
                .filter(Objects::nonNull)
                .map(role -> role.trim().toUpperCase(Locale.ROOT))
                .filter(role -> !role.isEmpty() && !CssRoleNaming.isSidecarRole(role))
                .toList();
        if (identityProvider != null && STAFF_PROVIDERS.contains(identityProvider)) {
            return STAFF_ROLES.stream()
                    .filter(role -> roles.contains("CSP_" + role))
                    .map(role -> (GrantedAuthority) new SimpleGrantedAuthority(role))
                    .toList();
        }
        if (!BCEID_BUSINESS.equals(identityProvider)) {
            return List.of();
        }
        List<String> clients = roles.stream()
                .map(CspAuthorities::submitterClient)
                .flatMap(Optional::stream)
                .distinct()
                .sorted()
                .toList();
        if (clients.isEmpty()) {
            return List.of();
        }
        List<GrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority(Roles.SUBMITTER));
        clients.forEach(client -> authorities.add(new SimpleGrantedAuthority(Roles.FOREST_CLIENT_PREFIX + client)));
        return List.copyOf(authorities);
    }

    /** The 8-digit client of a CSP_SUBMITTER_FOREST_CLIENT-00012345 role; empty for anything else. */
    static Optional<String> submitterClient(String role) {
        CssRoleNaming.ScopedRoleName parsed = CssRoleNaming.parse(role);
        if (!FAM_SUBMITTER.equals(parsed.baseRoleName()) || parsed.scopes().size() != 1) {
            return Optional.empty();
        }
        CssRoleNaming.Scope scope = parsed.scopes().get(0);
        return CssRoleNaming.SCOPE_FOREST_CLIENT.equals(scope.type()) && CLIENT_NUMBER.matcher(scope.value()).matches()
                ? Optional.of(scope.value())
                : Optional.empty();
    }
}
