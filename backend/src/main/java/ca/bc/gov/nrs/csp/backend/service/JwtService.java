package ca.bc.gov.nrs.csp.backend.service;

import ca.bc.gov.nrs.csp.backend.config.JwtProperties;
import ca.bc.gov.nrs.csp.backend.exception.JwtSigningKeyException;
import ca.bc.gov.nrs.csp.backend.security.CspAuthorities;
import com.auth0.jwk.JwkProvider;
import com.auth0.jwk.JwkProviderBuilder;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import jakarta.annotation.PostConstruct;
import java.net.MalformedURLException;
import java.net.URI;
import java.security.Key;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;

/** Validates CSS access tokens and maps their FAM roles to CSP authorities. */
@Service
public class JwtService {
    private static final String IDENTITY_PROVIDER = "identity_provider";
    private final JwtProperties jwtProperties;
    private JwkProvider provider;
    @Value("${auth.mock.enabled:false}")
    private boolean mockEnabled;

    public JwtService(JwtProperties jwtProperties) { this.jwtProperties = jwtProperties; }

    @PostConstruct
    public void init() throws MalformedURLException {
        if (mockEnabled) return;
        if (jwtProperties.issuer() == null || jwtProperties.issuer().isBlank()
                || jwtProperties.clientId() == null || jwtProperties.clientId().isBlank()) {
            throw new IllegalStateException("CSP_OIDC_ISSUER_URI and CSP_OIDC_CLIENT_ID are required");
        }
        String jwks = jwtProperties.jwksUri();
        if (jwks == null || jwks.isBlank()) {
            String issuer = jwtProperties.issuer();
            int end = issuer.length();
            while (end > 0 && issuer.charAt(end - 1) == '/') {
                end--;
            }
            jwks = issuer.substring(0, end) + "/protocol/openid-connect/certs";
        }
        this.provider = new JwkProviderBuilder(URI.create(jwks).toURL()).build();
    }

    public Claims extractAllClaims(String token) {
        if (provider == null) throw new JwtException("SSO authentication is not configured");
        var parsed = Jwts.parser()
                .keyLocator(header -> {
                    try { return (Key) provider.get((String) header.get("kid")).getPublicKey(); }
                    catch (Exception e) { throw new JwtSigningKeyException("Failed to resolve signing key", e); }
                })
                .requireIssuer(jwtProperties.issuer())
                .require("azp", jwtProperties.clientId())
                .require("typ", "Bearer")
                .build().parseSignedClaims(token);
        Claims claims = parsed.getPayload();
        String idp = claims.get(IDENTITY_PROVIDER, String.class);
        if (!"RS256".equals(parsed.getHeader().getAlgorithm()) || claims.getExpiration() == null
                || !(CspAuthorities.STAFF_PROVIDERS.contains(idp == null ? "" : idp)
                        || CspAuthorities.BCEID_BUSINESS.equals(idp))
                || auditUsername(claims).isBlank()) {
            throw new JwtException("A CSP user access token with an audit identity is required");
        }
        return claims;
    }

    public String extractUsername(String token) { return auditUsername(extractAllClaims(token)); }

    private String auditUsername(Claims claims) {
        String key = CspAuthorities.BCEID_BUSINESS.equals(claims.get(IDENTITY_PROVIDER)) ? "bceid_username" : "idir_username";
        Object value = claims.get(key);
        return value instanceof String username ? username.trim() : "";
    }

    /** Maps the token's CSP roles to authorities (see CspAuthorities). */
    public List<GrantedAuthority> extractAuthorities(String token) {
        Claims claims = extractAllClaims(token);
        List<String> roles = stringRoles(claims.get("client_roles"));
        if (roles.isEmpty() && claims.get("resource_access") instanceof Map<?, ?> byClient
                && byClient.get(jwtProperties.clientId()) instanceof Map<?, ?> client) {
            roles = stringRoles(client.get("roles"));
        }
        return CspAuthorities.fromFamRoles(claims.get(IDENTITY_PROVIDER, String.class), roles);
    }

    private static List<String> stringRoles(Object claim) {
        return claim instanceof List<?> values
                ? values.stream().filter(String.class::isInstance).map(String.class::cast).toList()
                : List.of();
    }
}
