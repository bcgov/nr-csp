package ca.bc.gov.nrs.csp.backend.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SpringDocConfigTest {

    private OpenAPI openAPI;

    @BeforeEach
    void setUp() {
        openAPI = new SpringDocConfig().openAPI();
    }

    @Test
    void openAPI_hasRelativeServerUrl() {
        assertThat(openAPI.getServers()).hasSize(1);
        assertThat(openAPI.getServers().getFirst().getUrl()).isEqualTo("/");
    }

    @Test
    void openAPI_hasApiInfo() {
        assertThat(openAPI.getInfo()).isNotNull();
        assertThat(openAPI.getInfo().getTitle()).isEqualTo("CSP Backend API");
        assertThat(openAPI.getInfo().getDescription())
                .isEqualTo("CSP Backend — code-first SpringDoc API");
        assertThat(openAPI.getInfo().getVersion()).isEqualTo("0.0.1-SNAPSHOT");
    }

    @Test
    void openAPI_hasGlobalBearerSecurityRequirement() {
        assertThat(openAPI.getSecurity()).hasSize(1);
        assertThat(openAPI.getSecurity().getFirst()).containsKey("bearerAuth");
    }

    @Test
    void openAPI_definesBearerJwtSecurityScheme() {
        assertThat(openAPI.getComponents()).isNotNull();
        assertThat(openAPI.getComponents().getSecuritySchemes()).containsKey("bearerAuth");

        SecurityScheme scheme = openAPI.getComponents().getSecuritySchemes().get("bearerAuth");
        assertThat(scheme.getName()).isEqualTo("bearerAuth");
        assertThat(scheme.getType()).isEqualTo(SecurityScheme.Type.HTTP);
        assertThat(scheme.getScheme()).isEqualTo("bearer");
        assertThat(scheme.getBearerFormat()).isEqualTo("JWT");
    }
}
