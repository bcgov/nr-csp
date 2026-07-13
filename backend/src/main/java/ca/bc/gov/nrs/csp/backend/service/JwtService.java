package ca.bc.gov.nrs.csp.backend.service;

import ca.bc.gov.nrs.csp.backend.config.JwtProperties;
import ca.bc.gov.nrs.csp.backend.exception.JwtSigningKeyException;
import ca.bc.gov.nrs.csp.backend.util.constants.Roles;
import com.auth0.jwk.Jwk;
import com.auth0.jwk.JwkProvider;
import com.auth0.jwk.JwkProviderBuilder;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import jakarta.annotation.PostConstruct;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;

import java.net.MalformedURLException;
import java.net.URL;
import java.security.Key;
import java.util.ArrayList;
import java.util.List;

@Service
public class JwtService {

    private final JwtProperties jwtProperties;
    private JwkProvider provider;

    public JwtService(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
    }

    @PostConstruct
    public void init() throws MalformedURLException {
        this.provider = new JwkProviderBuilder(new URL(jwtProperties.jwksUri())).build();
    }

    public String extractUsername(String token) {
        return extractUsername(extractAllClaims(token));
    }

    public String extractUsername(Claims claims) {
        String idpUsername = claims.get("custom:idp_username", String.class);
        return (idpUsername != null && !idpUsername.isBlank()) ? idpUsername : claims.getSubject();
    }

    public Claims extractAllClaims(String token) {
        var parser = Jwts.parser()
                .keyLocator(header -> {
                    try {
                        String kid = (String) header.get("kid");
                        Jwk jwk = provider.get(kid);
                        return (Key) jwk.getPublicKey();
                    } catch (Exception e) {
                        throw new JwtSigningKeyException("Failed to resolve signing key", e);
                    }
                });

        String issuer = jwtProperties.issuer();
        if (issuer != null && !issuer.isBlank()) {
            parser.requireIssuer(issuer);
        }

        String audience = jwtProperties.audience();
        if (audience != null && !audience.isBlank()) {
            parser.requireAudience(audience);
        }

        return parser.build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public List<GrantedAuthority> extractAuthorities(String token) {
        return extractAuthorities(extractAllClaims(token));
    }

    @SuppressWarnings("unchecked")
    public List<GrantedAuthority> extractAuthorities(Claims claims) {
        List<GrantedAuthority> authorities = new ArrayList<>();

        List<String> groups = claims.get("cognito:groups", List.class);
        if (groups != null) {
            if (matchesRole(groups, Roles.VIEW))    authorities.add(new SimpleGrantedAuthority(Roles.VIEW));
            if (matchesRole(groups, Roles.APPROVE)) authorities.add(new SimpleGrantedAuthority(Roles.APPROVE));
            if (matchesRole(groups, Roles.ADMIN))   authorities.add(new SimpleGrantedAuthority(Roles.ADMIN));
        }
        return authorities;
    }

    /**
     * Matches a Cognito group name against a role constant.
     * Supports plain names ("ADMIN") and FAM-prefixed names ("CSP_ADMIN", "NRS_CSP_ADMIN").
     */
    private boolean matchesRole(List<String> groups, String role) {
        String roleSuffix = "_" + role.toUpperCase();
        return groups.stream()
                .filter(java.util.Objects::nonNull)
                .anyMatch(g -> {
                    String upper = g.toUpperCase();
                    return upper.equals(role.toUpperCase()) || upper.endsWith(roleSuffix);
                });
    }
}
