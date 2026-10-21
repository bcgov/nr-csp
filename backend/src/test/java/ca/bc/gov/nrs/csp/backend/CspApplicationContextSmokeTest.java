package ca.bc.gov.nrs.csp.backend;

import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.springdoc.webmvc.ui.SwaggerWelcomeWebMvc;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Boots the full application context with the Oracle datasource stubbed out.
 *
 * Unit tests never load the Spring context, so a dependency upgrade can pass CI
 * yet crash on deploy while Spring introspects auto-configuration classes
 * (e.g. springdoc 2.8.17 compiled against Spring Boot 3 threw
 * NoClassDefFoundError for WebMvcProperties on Boot 4 at startup). This test
 * fails on that whole class of breakage before it reaches OpenShift.
 *
 * RETURNS_MOCKS lets startup-time DB access (ValidatingDataSource's
 * pre-flight query, ReferenceDataWarmupService) run against empty results
 * instead of a real Oracle connection. security.jwt.jwks-uri needs a
 * well-formed URL because JwtService parses it in @PostConstruct (the
 * provider fetches keys lazily, so nothing is contacted).
 */
@SpringBootTest(properties = {
        "security.jwt.jwks-uri=https://localhost/protocol/openid-connect/certs",
        "security.jwt.issuer=https://localhost/auth/realms/standard",
        "security.jwt.client-id=csp-smoke",
        // As local development runs it, with the API docs on.
        "API_DOCS_ENABLED=true"
})
class CspApplicationContextSmokeTest {

    @MockitoBean(answers = Answers.RETURNS_MOCKS)
    private HikariDataSource hikariDataSource;

    @Autowired
    private ApplicationContext context;

    @Test
    void contextLoadsAndSwaggerConfigResolves() {
        assertThat(context.getBean(SwaggerWelcomeWebMvc.class)).isNotNull();
    }

    @Test
    void enabledApiDocsAreServedToStaffOnly() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.webAppContextSetup((WebApplicationContext) context)
                .apply(springSecurity()).build();

        mockMvc.perform(get("/api/v3/api-docs")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v3/api-docs").with(user("JSMITH").authorities(() -> "SUBMITTER")))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v3/api-docs").with(user("JSMITH").authorities(() -> "VIEW")))
                .andExpect(status().isOk());
    }
}
