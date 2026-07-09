package ca.bc.gov.nrs.csp.backend.service;

import ca.bc.gov.nrs.csp.backend.config.JwtProperties;
import ca.bc.gov.nrs.csp.backend.exception.JwtSigningKeyException;
import com.auth0.jwk.Jwk;
import com.auth0.jwk.JwkException;
import com.auth0.jwk.JwkProvider;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.test.util.ReflectionTestUtils;

import java.net.MalformedURLException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class JwtServiceTest {

    private static final String KID = "test-kid";

    private KeyPair keyPair;
    private JwtService service;

    @BeforeEach
    void setUp() throws Exception {
        KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
        gen.initialize(2048);
        keyPair = gen.generateKeyPair();
        service = serviceWith("", "");
    }

    private JwtService serviceWith(String issuer, String audience) throws Exception {
        Jwk jwk = mock(Jwk.class);
        when(jwk.getPublicKey()).thenReturn(keyPair.getPublic());
        JwkProvider provider = mock(JwkProvider.class);
        when(provider.get(KID)).thenReturn(jwk);

        JwtService s = new JwtService(new JwtProperties(
                "https://dummy.example.com/.well-known/jwks.json", issuer, audience));
        ReflectionTestUtils.setField(s, "provider", provider);
        return s;
    }

    private String token(String sub, String issuer, String audience, String idpUsername) {
        var builder = Jwts.builder()
                .header().keyId(KID).and()
                .subject(sub)
                .expiration(new Date(System.currentTimeMillis() + 3_600_000));

        if (issuer != null)      builder.issuer(issuer);
        if (audience != null)    builder.audience().add(audience).and();
        if (idpUsername != null) builder.claim("custom:idp_username", idpUsername);

        return builder.signWith(keyPair.getPrivate()).compact();
    }

    private String tokenWithGroups(String sub, List<String> groups) {
        return Jwts.builder()
                .header().keyId(KID).and()
                .subject(sub)
                .expiration(new Date(System.currentTimeMillis() + 3_600_000))
                .claim("cognito:groups", groups)
                .signWith(keyPair.getPrivate())
                .compact();
    }

    // ── extractUsername ───────────────────────────────────────────────────────

    @Test
    void extractUsername_returnsIdpUsername_whenPresent() {
        String jwt = token("sub-uuid", null, null, "SOCOOPER");
        assertEquals("SOCOOPER", service.extractUsername(jwt));
    }

    @Test
    void extractUsername_fallsBackToSub_whenIdpUsernameAbsent() {
        String jwt = token("sub-uuid", null, null, null);
        assertEquals("sub-uuid", service.extractUsername(jwt));
    }

    @Test
    void extractUsername_fallsBackToSub_whenIdpUsernameBlank() {
        String jwt = token("sub-uuid", null, null, "   ");
        assertEquals("sub-uuid", service.extractUsername(jwt));
    }

    // ── issuer validation ─────────────────────────────────────────────────────

    @Test
    void extractAllClaims_rejectsToken_whenIssuerMismatch() throws Exception {
        JwtService strict = serviceWith("https://expected.example.com", "");
        String jwt = token("sub", "https://wrong.example.com", null, null);
        assertThrows(JwtException.class, () -> strict.extractAllClaims(jwt));
    }

    @Test
    void extractAllClaims_skipsIssuerValidation_whenConfiguredIssuerBlank() {
        String jwt = token("sub", "https://any-issuer.example.com", null, null);
        assertDoesNotThrow(() -> service.extractAllClaims(jwt));
    }

    // ── audience validation ───────────────────────────────────────────────────

    @Test
    void extractAllClaims_rejectsToken_whenAudienceMismatch() throws Exception {
        JwtService strict = serviceWith("", "expected-client-id");
        String jwt = token("sub", null, "wrong-client-id", null);
        assertThrows(JwtException.class, () -> strict.extractAllClaims(jwt));
    }

    @Test
    void extractAllClaims_skipsAudienceValidation_whenConfiguredAudienceBlank() {
        String jwt = token("sub", null, "any-client-id", null);
        assertDoesNotThrow(() -> service.extractAllClaims(jwt));
    }

    @Test
    void extractAllClaims_skipsValidation_whenConfiguredIssuerAndAudienceNull() throws Exception {
        JwtService lenient = serviceWith(null, null);
        String jwt = token("sub", "https://any-issuer.example.com", "any-client-id", null);
        assertDoesNotThrow(() -> lenient.extractAllClaims(jwt));
    }

    // ── signing key resolution ────────────────────────────────────────────────

    @Test
    void extractAllClaims_wrapsProviderFailure_inJwtSigningKeyException() throws Exception {
        JwkProvider failingProvider = mock(JwkProvider.class);
        when(failingProvider.get(KID)).thenThrow(new JwkException("JWKS endpoint unavailable"));

        JwtService failing = new JwtService(new JwtProperties(
                "https://dummy.example.com/.well-known/jwks.json", "", ""));
        ReflectionTestUtils.setField(failing, "provider", failingProvider);

        String jwt = token("sub", null, null, null);
        JwtSigningKeyException ex =
                assertThrows(JwtSigningKeyException.class, () -> failing.extractAllClaims(jwt));
        assertEquals("Failed to resolve signing key", ex.getMessage());
        assertInstanceOf(JwkException.class, ex.getCause());
    }

    // ── init ──────────────────────────────────────────────────────────────────

    @Test
    void init_buildsJwkProvider_fromConfiguredJwksUri() throws Exception {
        JwtService fresh = new JwtService(new JwtProperties(
                "https://cognito-idp.ca-central-1.amazonaws.com/pool/.well-known/jwks.json", "", ""));
        fresh.init();
        assertNotNull(ReflectionTestUtils.getField(fresh, "provider"));
    }

    @Test
    void init_throwsMalformedURLException_whenJwksUriInvalid() {
        JwtService broken = new JwtService(new JwtProperties("not-a-valid-url", "", ""));
        assertThrows(MalformedURLException.class, broken::init);
    }

    // ── extractAuthorities ────────────────────────────────────────────────────

    @Test
    void extractAuthorities_mapsAllMatchingFamPrefixedGroups() {
        String jwt = tokenWithGroups("sub", List.of("CSP_VIEW", "CSP_APPROVE", "CSP_ADMIN"));

        List<GrantedAuthority> authorities = service.extractAuthorities(jwt);

        assertEquals(3, authorities.size());
        assertTrue(authorities.stream().anyMatch(a -> a.getAuthority().equals("VIEW")));
        assertTrue(authorities.stream().anyMatch(a -> a.getAuthority().equals("APPROVE")));
        assertTrue(authorities.stream().anyMatch(a -> a.getAuthority().equals("ADMIN")));
    }

    @Test
    void extractAuthorities_matchesPlainRoleNames_caseInsensitively() {
        String jwt = tokenWithGroups("sub", List.of("admin"));

        List<GrantedAuthority> authorities = service.extractAuthorities(jwt);

        assertEquals(1, authorities.size());
        assertEquals("ADMIN", authorities.get(0).getAuthority());
    }

    @Test
    void extractAuthorities_matchesLongerFamPrefixedNames() {
        String jwt = tokenWithGroups("sub", List.of("NRS_CSP_VIEW"));

        List<GrantedAuthority> authorities = service.extractAuthorities(jwt);

        assertEquals(1, authorities.size());
        assertEquals("VIEW", authorities.get(0).getAuthority());
    }

    @Test
    void extractAuthorities_returnsEmpty_whenGroupsClaimAbsent() {
        String jwt = token("sub", null, null, null);
        assertTrue(service.extractAuthorities(jwt).isEmpty());
    }

    @Test
    void extractAuthorities_returnsEmpty_whenNoGroupMatchesAnyRole() {
        String jwt = tokenWithGroups("sub", List.of("SOME_OTHER_GROUP", "VIEWER"));
        assertTrue(service.extractAuthorities(jwt).isEmpty());
    }
}
