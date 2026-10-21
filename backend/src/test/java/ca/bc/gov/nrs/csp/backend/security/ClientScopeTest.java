package ca.bc.gov.nrs.csp.backend.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

class ClientScopeTest {

    private static List<GrantedAuthority> authorities(String... names) {
        return Arrays.stream(names).map(name -> (GrantedAuthority) new SimpleGrantedAuthority(name)).toList();
    }

    @Test
    void staffSeeEveryClient() {
        for (String role : List.of("ADMIN", "APPROVE", "VIEW")) {
            ClientScope scope = ClientScope.from(authorities(role));
            assertThat(scope.isUnrestricted()).isTrue();
            assertThat(scope.permits("00000001")).isTrue();
        }
    }

    @Test
    void submitterSeesOnlyTheirClients() {
        ClientScope scope = ClientScope.from(authorities("SUBMITTER", "FOREST_CLIENT-00000002", "FOREST_CLIENT-00000001"));

        assertThat(scope.isUnrestricted()).isFalse();
        assertThat(scope.clientNumbers()).containsExactly("00000001", "00000002");
        assertThat(scope.permits("00000001")).isTrue();
        assertThat(scope.permits("00000003")).isFalse();
        assertThat(scope.permits(" 00000001")).isFalse();
        assertThat(scope.permits(null)).isFalse();
    }

    @Test
    void submitterWithoutAClientIsDenied_neverUnrestricted() {
        assertThatThrownBy(() -> ClientScope.from(authorities("SUBMITTER")))
                .isInstanceOf(AuthorizationDeniedException.class);
        assertThatThrownBy(() -> ClientScope.from(authorities("SUBMITTER", "FOREST_CLIENT-")))
                .isInstanceOf(AuthorizationDeniedException.class);
    }

    @Test
    void clientsWithoutTheSubmitterRoleAreDenied() {
        assertThatThrownBy(() -> ClientScope.from(authorities("FOREST_CLIENT-00000001")))
                .isInstanceOf(AuthorizationDeniedException.class);
    }

    @Test
    void staffAndSubmitterTogetherAreDenied() {
        // CspAuthorities never grants both; a mix means something upstream is wrong.
        assertThatThrownBy(() -> ClientScope.from(authorities("ADMIN", "SUBMITTER", "FOREST_CLIENT-00000001")))
                .isInstanceOf(AuthorizationDeniedException.class);
        assertThatThrownBy(() -> ClientScope.from(authorities("VIEW", "FOREST_CLIENT-00000001")))
                .isInstanceOf(AuthorizationDeniedException.class);
    }

    @Test
    void noRoleIsDenied() {
        assertThatThrownBy(() -> ClientScope.from(authorities())).isInstanceOf(AuthorizationDeniedException.class);
        assertThatThrownBy(() -> ClientScope.from(authorities("OTHER_APP_ADMIN")))
                .isInstanceOf(AuthorizationDeniedException.class);
    }

    @Test
    void aRestrictedScopeCannotBeEmpty() {
        assertThatThrownBy(() -> ClientScope.of(List.of())).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ClientScope.of(null)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void theUnrestrictedScopeHasNoClientList() {
        assertThatThrownBy(() -> ClientScope.allClients().clientNumbers()).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void scopesCompareByTheirClients() {
        assertThat(ClientScope.of(List.of("2", "1"))).isEqualTo(ClientScope.of(List.of("1", "2")));
        assertThat(ClientScope.of(List.of("1"))).isNotEqualTo(ClientScope.allClients());
    }
}
