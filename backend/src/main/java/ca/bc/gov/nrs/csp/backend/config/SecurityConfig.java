package ca.bc.gov.nrs.csp.backend.config;

import ca.bc.gov.nrs.csp.backend.filter.JwtRequestFilter;
import ca.bc.gov.nrs.csp.backend.filter.MockRequestFilter;
import ca.bc.gov.nrs.csp.backend.service.JwtService;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

import java.util.Optional;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@EnableConfigurationProperties(JwtProperties.class)
public class SecurityConfig {

    private static final String API_PATH = "/api/**";

    @Bean
    public JwtRequestFilter jwtRequestFilter(JwtService jwtService) {
        return new JwtRequestFilter(jwtService);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtRequestFilter jwtRequestFilter,
            Optional<MockRequestFilter> mockRequestFilter) throws Exception {

        mockRequestFilter.ifPresent(f ->
                http.addFilterBefore(f, UsernamePasswordAuthenticationFilter.class));
        http.addFilterBefore(jwtRequestFilter, UsernamePasswordAuthenticationFilter.class);

        http
                // CSRF protection is intentionally disabled: this API is stateless (no session
                // cookie) and authenticates solely via the Authorization: Bearer header, which
                // browsers do not auto-attach — so CSRF is not exploitable here.
                // codeql[java/spring-disabled-csrf-protection]
                .csrf(csrf -> csrf.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/api/health",
                                "/swagger-ui/**", "/swagger-ui.html",
                                "/v3/api-docs/**"
                        ).permitAll()
                        .requestMatchers(API_PATH).authenticated()
                        .anyRequest().authenticated()
                );

        http.headers(headers -> headers
                .contentSecurityPolicy(csp -> csp.policyDirectives(
                        "default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline'; img-src 'self' data:"))
                .frameOptions(frame -> frame.deny())
                .httpStrictTransportSecurity(hsts -> hsts.maxAgeInSeconds(31536000))
        );

        return http.build();
    }
}
