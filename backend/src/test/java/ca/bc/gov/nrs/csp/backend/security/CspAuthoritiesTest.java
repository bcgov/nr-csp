package ca.bc.gov.nrs.csp.backend.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.security.core.GrantedAuthority;

class CspAuthoritiesTest {

    private static List<String> grant(String identityProvider, String... famRoles) {
        return CspAuthorities.fromFamRoles(identityProvider, Arrays.asList(famRoles)).stream()
                .map(GrantedAuthority::getAuthority).toList();
    }

    // ── IDIR staff ──────────────────────────────────────────────────────────

    @ParameterizedTest
    @ValueSource(strings = {"idir", "azureidir"})
    void staffGetExactUnscopedStaffCodes(String idp) {
        assertThat(grant(idp, "CSP_VIEW", "CSP_APPROVE", "CSP_ADMIN")).containsExactly("ADMIN", "APPROVE", "VIEW");
    }

    @Test
    void caseAndPaddingDoNotChangeWhatACodeGrants() {
        assertThat(grant("idir", " csp_admin ", "Csp_View")).containsExactly("ADMIN", "VIEW");
        assertThat(grant("bceidbusiness", " csp_submitter_forest_client-00012345 "))
                .containsExactly("SUBMITTER", "FOREST_CLIENT-00012345");
    }

    @Test
    void staffCodesMatchWholeCodesOnly() {
        assertThat(grant("idir", "ADMIN", "NRS_CSP_ADMIN", "CSP_ADMINISTRATOR", "", "  ")).isEmpty();
    }

    @Test
    void staffNeverGetTheIndustryRole() {
        assertThat(grant("azureidir", "CSP_SUBMITTER_FOREST_CLIENT-00012345", "CSP_SUBMITTER")).isEmpty();
        assertThat(grant("azureidir", "CSP_VIEW", "CSP_SUBMITTER_FOREST_CLIENT-00012345")).containsExactly("VIEW");
    }

    @Test
    void scopedStaffCodesAreNotStaffCodes() {
        // FAM never scopes CSP's staff roles; a scoped one is a different role and grants nothing.
        assertThat(grant("idir", "CSP_ADMIN_FOREST_CLIENT-00012345", "CSP_VIEW_DISTRICT-DCC")).isEmpty();
    }

    // ── Business BCeID industry ─────────────────────────────────────────────

    @Test
    void submitterGetsOneAuthorityPerForestClient_sortedAndDistinct() {
        assertThat(grant("bceidbusiness",
                "CSP_SUBMITTER_FOREST_CLIENT-00000002", "CSP_SUBMITTER_FOREST_CLIENT-00000001",
                "CSP_SUBMITTER_FOREST_CLIENT-00000002"))
                .containsExactly("SUBMITTER", "FOREST_CLIENT-00000001", "FOREST_CLIENT-00000002");
    }

    @Test
    void businessIdentityNeverGetsStaffRoles() {
        assertThat(grant("bceidbusiness", "CSP_ADMIN", "CSP_APPROVE", "CSP_VIEW")).isEmpty();
        assertThat(grant("bceidbusiness", "CSP_ADMIN", "CSP_SUBMITTER_FOREST_CLIENT-00012345"))
                .containsExactly("SUBMITTER", "FOREST_CLIENT-00012345");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "CSP_SUBMITTER",                                      // the bare code is never in a token
            "CSP_SUBMITTER_DISTRICT-DCC",                         // wrong scope type
            "CSP_SUBMITTER_DISTRICT-DCC_FOREST_CLIENT-00012345",  // compound scope
            "CSP_SUBMITTER_FOREST_CLIENT-1234567",                // seven digits
            "CSP_SUBMITTER_FOREST_CLIENT-000123456",              // nine digits
            "CSP_SUBMITTER_FOREST_CLIENT-0001234A",               // not digits
            "CSP_SUBMITTER_FOREST_CLIENT-",                       // no client
            "CSP_SUBMITTER_FOREST_CLIENT-00012345X",              // trailing junk
            "ILCR_SUBMITTER_FOREST_CLIENT-00012345",              // another app's role
            "XCSP_SUBMITTER_FOREST_CLIENT-00012345",
            "FAM:EXPIRES:2026-09-30:CSP_SUBMITTER_FOREST_CLIENT-00012345",
    })
    void anythingButAnExactClientScopedSubmitterGrantGivesNothing(String role) {
        assertThat(grant("bceidbusiness", role)).isEmpty();
    }

    @Test
    void submitterWithNoRolesGetsNothing() {
        assertThat(grant("bceidbusiness")).isEmpty();
    }

    // ── Other identity providers ────────────────────────────────────────────

    @ParameterizedTest
    @ValueSource(strings = {"bceidboth", "bceidbasic", "github", "IDIR", "BCEIDBUSINESS", ""})
    void otherIdentityProvidersGetNothing(String idp) {
        assertThat(grant(idp, "CSP_ADMIN", "CSP_SUBMITTER_FOREST_CLIENT-00012345")).isEmpty();
    }

    @Test
    void missingIdentityProviderGetsNothing() {
        assertThat(grant(null, "CSP_ADMIN", "CSP_SUBMITTER_FOREST_CLIENT-00012345")).isEmpty();
    }

    @Test
    void nullRoleEntriesAreIgnored() {
        assertThat(CspAuthorities.fromFamRoles("idir", Arrays.asList(null, "CSP_VIEW")))
                .extracting(GrantedAuthority::getAuthority).containsExactly("VIEW");
    }
}
