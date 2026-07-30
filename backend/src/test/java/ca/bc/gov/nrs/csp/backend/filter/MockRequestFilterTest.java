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
        MockRequestFilter filter = new MockRequestFilter("LOCALDEV", "ADMIN");

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
        MockRequestFilter filter = new MockRequestFilter("LOCALDEV", "ADMIN, VIEW ,APPROVE");

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
    void existingAuthentication_isNotOverwritten() throws Exception {
        MockRequestFilter filter = new MockRequestFilter("LOCALDEV", "ADMIN");

        var existingAuth = new UsernamePasswordAuthenticationToken("EXISTING", null, List.of());
        SecurityContextHolder.getContext().setAuthentication(existingAuth);

        filter.doFilter(request, response, filterChain);

        assertEquals("EXISTING", SecurityContextHolder.getContext().getAuthentication().getName());
        verify(filterChain).doFilter(request, response);
    }
}
