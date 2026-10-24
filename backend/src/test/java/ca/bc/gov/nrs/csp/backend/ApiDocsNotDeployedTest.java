package ca.bc.gov.nrs.csp.backend;

import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.springdoc.webmvc.ui.SwaggerWelcomeWebMvc;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
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
 * The application as deployed: real (not mock) authentication, and API_DOCS_ENABLED unset,
 * because the OpenShift templates never pass it. Swagger UI and the OpenAPI document must not
 * exist, and nothing but /api/health may be reached without signing in.
 */
@SpringBootTest(properties = {
        "auth.mock.enabled=false",
        "security.jwt.jwks-uri=https://localhost/protocol/openid-connect/certs",
        "security.jwt.issuer=https://localhost/auth/realms/standard",
        "security.jwt.client-id=csp-smoke"
})
class ApiDocsNotDeployedTest {

    private static final String[] API_DOC_PATHS = {
            "/api/v3/api-docs", "/api/swagger-ui.html", "/api/swagger-ui/index.html"};

    @MockitoBean(answers = Answers.RETURNS_MOCKS)
    private HikariDataSource hikariDataSource;

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @Test
    void springdocIsNotLoaded() {
        assertThat(context.getBeanNamesForType(SwaggerWelcomeWebMvc.class)).isEmpty();
    }

    @Test
    void onlyHealthIsPublic() throws Exception {
        mockMvc.perform(get("/api/health")).andExpect(status().isOk());
        for (String path : API_DOC_PATHS) {
            mockMvc.perform(get(path)).andExpect(status().isUnauthorized());
        }
    }

    @Test
    void staffFindNoApiDocs() throws Exception {
        for (String path : API_DOC_PATHS) {
            mockMvc.perform(get(path).with(user("JSMITH").authorities(() -> "ADMIN")))
                    .andExpect(status().isNotFound());
        }
    }
}
