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
        return extractAllClaims(token).getSubject();
    }

    public Claims extractAllClaims(String token) {
        return Jwts.parser()
                .keyLocator(header -> {
                    try {
                        String kid = (String) header.get("kid");
                        Jwk jwk = provider.get(kid);
                        return (Key) jwk.getPublicKey();
                    } catch (Exception e) {
                        throw new JwtSigningKeyException("Failed to resolve signing key", e);
                    }
                })
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    @SuppressWarnings("unchecked")
    public List<GrantedAuthority> extractAuthorities(String token) {
        Claims claims = extractAllClaims(token);
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
        return groups.stream().anyMatch(g -> {
            String upper = g.toUpperCase();
            return upper.equals(role.toUpperCase()) || upper.endsWith(roleSuffix);
        });
    }
}
