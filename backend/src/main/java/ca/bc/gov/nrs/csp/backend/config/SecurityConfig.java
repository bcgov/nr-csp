package ca.bc.gov.nrs.csp.backend.config;

import ca.bc.gov.nrs.csp.backend.filter.JwtRequestFilter;
import ca.bc.gov.nrs.csp.backend.filter.MockRequestFilter;
import ca.bc.gov.nrs.csp.backend.service.JwtService;
import ca.bc.gov.nrs.csp.backend.util.constants.Roles;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import jakarta.servlet.http.HttpServletResponse;

import java.util.Optional;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@EnableConfigurationProperties(JwtProperties.class)
public class SecurityConfig {

    private static final String API_PATH = "/api/**";

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtService jwtService,
            Optional<MockRequestFilter> mockRequestFilter) throws Exception {

        // Not a @Bean — keeps it out of the servlet container's filter registry so it
        // only runs inside the Spring Security filter chain (where SecurityContextHolder
        // is properly managed for stateless sessions).
        JwtRequestFilter jwtRequestFilter = new JwtRequestFilter(jwtService);

        mockRequestFilter.ifPresent(f ->
                http.addFilterBefore(f, UsernamePasswordAuthenticationFilter.class));
        http.addFilterBefore(jwtRequestFilter, UsernamePasswordAuthenticationFilter.class);

        http
                // Safe to disable: stateless sessions (no session cookie) + JWT via Authorization
                // header means there is no browser-automatable credential for CSRF to exploit.
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((req, res, e) ->
                                res.sendError(HttpServletResponse.SC_UNAUTHORIZED)))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/health").permitAll()
                        // API docs are an authenticated, admin-only surface — never anonymous.
                        // In prod they are additionally disabled entirely (application.yml prod
                        // profile sets springdoc.api-docs.enabled / swagger-ui.enabled=false),
                        // so these matchers only ever apply in non-prod environments.
                        .requestMatchers(
                                "/api/swagger-ui/**", "/api/swagger-ui.html",
                                "/api/v3/api-docs/**"
                        ).hasAuthority(Roles.ADMIN)
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
