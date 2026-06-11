package ca.bc.gov.nrs.csp.backend.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SpringDocConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("CSP Backend API")
                        .description("CSP Backend — code-first SpringDoc API")
                        .version("0.0.1-SNAPSHOT"));
    }
}
