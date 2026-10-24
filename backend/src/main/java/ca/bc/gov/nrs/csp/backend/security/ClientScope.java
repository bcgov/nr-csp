package ca.bc.gov.nrs.csp.backend.security;

import ca.bc.gov.nrs.csp.backend.util.constants.Roles;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.security.core.GrantedAuthority;

/**
 * The forest clients the current user may see and submit for: every client for IDIR staff, or
 * the clients a Business BCeID submitter holds.
 *
 * <p>There is no empty scope. Unrestricted is its own value, never an empty list, so a submitter
 * whose clients went missing is denied rather than shown everything.
 */
public final class ClientScope {

    private static final ClientScope ALL_CLIENTS = new ClientScope(null);
    private static final Set<String> STAFF_ROLES = Set.of(Roles.ADMIN, Roles.APPROVE, Roles.VIEW);

    private final List<String> clientNumbers;

    private ClientScope(List<String> clientNumbers) {
        this.clientNumbers = clientNumbers;
    }

    public static ClientScope allClients() {
        return ALL_CLIENTS;
    }

    public static ClientScope of(Collection<String> clientNumbers) {
        if (clientNumbers == null || clientNumbers.isEmpty()) {
            throw new IllegalArgumentException("A restricted client scope needs at least one client.");
        }
        return new ClientScope(List.copyOf(new TreeSet<>(clientNumbers)));
    }

    /**
     * Reads the scope from a user's authorities. Staff (and no submitter role) see every client;
     * a submitter (and no staff role) with at least one forest-client authority sees those clients.
     * Anything else is denied.
     */
    public static ClientScope from(Collection<? extends GrantedAuthority> authorities) {
        Set<String> names = authorities.stream().map(GrantedAuthority::getAuthority).collect(Collectors.toSet());
        boolean staff = names.stream().anyMatch(STAFF_ROLES::contains);
        boolean submitter = names.contains(Roles.SUBMITTER);
        List<String> clients = names.stream()
                .filter(name -> name.startsWith(Roles.FOREST_CLIENT_PREFIX))
                .map(name -> name.substring(Roles.FOREST_CLIENT_PREFIX.length()))
                .filter(client -> !client.isEmpty())
                .toList();
        if (staff && !submitter && clients.isEmpty()) {
            return ALL_CLIENTS;
        }
        if (submitter && !staff && !clients.isEmpty()) {
            return of(clients);
        }
        throw new AuthorizationDeniedException("The current user has no forest-client scope.");
    }

    public boolean isUnrestricted() {
        return clientNumbers == null;
    }

    /** The clients of a restricted scope, sorted and never empty. */
    public List<String> clientNumbers() {
        if (clientNumbers == null) {
            throw new IllegalStateException("An unrestricted scope has no client list.");
        }
        return clientNumbers;
    }

    public boolean permits(String clientNumber) {
        return clientNumbers == null || (clientNumber != null && clientNumbers.contains(clientNumber));
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof ClientScope other && Objects.equals(clientNumbers, other.clientNumbers);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(clientNumbers);
    }

    @Override
    public String toString() {
        return clientNumbers == null ? "ClientScope[all clients]" : "ClientScope" + clientNumbers;
    }
}
