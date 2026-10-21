package ca.bc.gov.nrs.csp.backend.filter;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MockRequestFilterTest {

    @Mock FilterChain filterChain;

    MockHttpServletRequest request;
    MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void noExistingAuthentication_injectsMockUserAndPassesThrough() throws Exception {
        MockRequestFilter filter = new MockRequestFilter("LOCALDEV", "idir", "ADMIN");

        filter.doFilter(request, response, filterChain);

        var auth = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(auth);
        assertEquals("LOCALDEV", auth.getName());
        assertNull(auth.getCredentials());
        assertTrue(auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ADMIN")));
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void multipleRoles_areSplitOnCommaAndTrimmed() throws Exception {
        MockRequestFilter filter = new MockRequestFilter("LOCALDEV", "idir", "ADMIN, VIEW ,APPROVE");

        filter.doFilter(request, response, filterChain);

        var auth = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(auth);
        assertEquals(3, auth.getAuthorities().size());
        assertTrue(auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ADMIN")));
        assertTrue(auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("VIEW")));
        assertTrue(auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("APPROVE")));
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void famCodes_mapLikeARealToken() throws Exception {
        MockRequestFilter filter = new MockRequestFilter("LOCALDEV", "idir", "CSP_VIEW,CSP_ADMIN_FOREST_CLIENT-00000001");

        filter.doFilter(request, response, filterChain);

        assertEquals(Set.of("VIEW"), authorities());
    }

    @Test
    void bceidDefault_grantsSubmitterForItsForestClients() throws Exception {
        MockRequestFilter filter = new MockRequestFilter("LOCALDEV", "bceidbusiness",
                "CSP_SUBMITTER_FOREST_CLIENT-00000002,CSP_SUBMITTER_FOREST_CLIENT-00000001,ADMIN");

        filter.doFilter(request, response, filterChain);

        assertEquals(Set.of("SUBMITTER", "FOREST_CLIENT-00000001", "FOREST_CLIENT-00000002"), authorities());
    }

    @Test
    void bceidWithoutAClientRole_getsNothing() throws Exception {
        MockRequestFilter filter = new MockRequestFilter("LOCALDEV", "bceidbusiness", "CSP_SUBMITTER");

        filter.doFilter(request, response, filterChain);

        assertEquals(Set.of(), authorities());
    }

    @Test
    void requestHeaders_overrideTheConfiguredUser() throws Exception {
        MockRequestFilter filter = new MockRequestFilter("LOCALDEV", "idir", "ADMIN");
        request.addHeader(MockRequestFilter.IDENTITY_PROVIDER_HEADER, "bceidbusiness");
        request.addHeader(MockRequestFilter.ROLES_HEADER, "CSP_SUBMITTER_FOREST_CLIENT-00000001");

        filter.doFilter(request, response, filterChain);

        assertEquals(Set.of("SUBMITTER", "FOREST_CLIENT-00000001"), authorities());
    }

    @Test
    void unknownIdentityProviderHeader_getsNothing() throws Exception {
        MockRequestFilter filter = new MockRequestFilter("LOCALDEV", "idir", "ADMIN");
        request.addHeader(MockRequestFilter.IDENTITY_PROVIDER_HEADER, "github");

        filter.doFilter(request, response, filterChain);

        assertEquals(Set.of(), authorities());
    }

    private static Set<String> authorities() {
        return SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .map(a -> a.getAuthority()).collect(Collectors.toSet());
    }

    @Test
    void existingAuthentication_isNotOverwritten() throws Exception {
        MockRequestFilter filter = new MockRequestFilter("LOCALDEV", "idir", "ADMIN");

        var existingAuth = new UsernamePasswordAuthenticationToken("EXISTING", null, List.of());
        SecurityContextHolder.getContext().setAuthentication(existingAuth);

        filter.doFilter(request, response, filterChain);

        assertEquals("EXISTING", SecurityContextHolder.getContext().getAuthentication().getName());
        verify(filterChain).doFilter(request, response);
    }
}
