package ca.bc.gov.nrs.csp.backend.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class SpringDocConfig {

    private static final String BEARER_SCHEME = "bearerAuth";

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                // Relative server so Swagger UI issues same-origin requests to
                // whatever host the page was loaded from (direct :8080 or the
                // Vite/Caddy /api proxy). Avoids baking in a proxy-internal
                // host like backend:8080, which the browser can't resolve.
                .servers(List.of(new Server().url("/")))
                .info(new Info()
                        .title("CSP Backend API")
                        .description("CSP Backend — code-first SpringDoc API")
                        .version("0.0.1-SNAPSHOT"))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME))
                .components(new Components()
                        .addSecuritySchemes(BEARER_SCHEME, new SecurityScheme()
                                .name(BEARER_SCHEME)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }
}
