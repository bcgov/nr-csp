package ca.bc.gov.nrs.csp.backend.service;

import ca.bc.gov.nrs.csp.backend.config.JwtProperties;
import com.auth0.jwk.Jwk;
import com.auth0.jwk.JwkProvider;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Date;

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
}
