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
 * Maps an identity provider and its FAM role codes to CSP authorities.
 *
 * <p>{@code JwtService} uses this for CSS tokens and {@code MockRequestFilter} for the local mock
 * user, so both grant the same way. It fails closed:
 * <ul>
 *   <li>IDIR staff get {@code ADMIN}/{@code APPROVE}/{@code VIEW} from the exact unscoped codes
 *       {@code CSP_ADMIN}/{@code CSP_APPROVE}/{@code CSP_VIEW}, and never an industry role.</li>
 *   <li>Business BCeID users get {@code SUBMITTER} plus one forest-client authority per exact
 *       {@code CSP_SUBMITTER_FOREST_CLIENT-<8 digits>} grant, and never a staff role. Without such
 *       a grant they get nothing.</li>
 *   <li>Any other identity provider gets nothing.</li>
 * </ul>
 *
 * <p>Codes are trimmed and upper-cased before matching, as in LEXIS, so a role's spelling cannot
 * change what it grants. {@code FAM:} bookkeeping roles are ignored.
 */
public final class CspAuthorities {

    public static final Set<String> STAFF_PROVIDERS = Set.of("idir", "azureidir");
    public static final String BCEID_BUSINESS = "bceidbusiness";

    /** The FAM code FAM scopes per forest client for industry submitters. */
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

    /**
     * The forest client of a {@code CSP_SUBMITTER_FOREST_CLIENT-<8 digits>} grant. Any other base,
     * an unscoped or compound grant, or a malformed client number is not a submitter grant.
     */
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
