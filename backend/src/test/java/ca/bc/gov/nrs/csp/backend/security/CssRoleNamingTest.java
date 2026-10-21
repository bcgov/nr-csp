package ca.bc.gov.nrs.csp.backend.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/** Ported from nr-fam's CssRoleNamingTest (the parse cases). */
@DisplayName("CssRoleNaming.parse (scope encoded into CSS role names)")
class CssRoleNamingTest {

    @ParameterizedTest(name = "{0} -> ({1}, {2}, {3})")
    @CsvSource({
            "CHR_FREP_EDITOR_DISTRICT-DCC,         CHR_FREP_EDITOR, DISTRICT,      DCC",
            "FOM_SUBMITTER_FOREST_CLIENT-00001018, FOM_SUBMITTER,   FOREST_CLIENT, 00001018",
            "CSP_SUBMITTER_FOREST_CLIENT-00012345, CSP_SUBMITTER,   FOREST_CLIENT, 00012345",
            "FREP_EDITOR_REGION-KOOTENAY_BOUNDARY, FREP_EDITOR,     REGION,        KOOTENAY_BOUNDARY",
            "A_B_C_DISTRICT-DQU,                   A_B_C,           DISTRICT,      DQU",
    })
    @DisplayName("parses a scope-specific role name back into its parts")
    void parsesScopedName(String roleName, String base, String scopeType, String value) {
        CssRoleNaming.ScopedRoleName parsed = CssRoleNaming.parse(roleName);

        assertThat(parsed.baseRoleName()).isEqualTo(base);
        assertThat(parsed.scopes()).containsExactly(new CssRoleNaming.Scope(scopeType, value));
    }

    @Test
    @DisplayName("keeps a multi-word scope type intact")
    void keepsMultiWordScopeTypeIntact() {
        // Splitting on the last underscore reads this as scope type CLIENT of role
        // FOM_SUBMITTER_FOREST: the value survives but the role and scope type do not.
        CssRoleNaming.ScopedRoleName parsed = CssRoleNaming.parse("FOM_SUBMITTER_FOREST_CLIENT-00001018");

        assertThat(parsed.baseRoleName()).isNotEqualTo("FOM_SUBMITTER_FOREST");
        assertThat(parsed.scopes()).extracting(CssRoleNaming.Scope::type).doesNotContain("CLIENT");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "FREP_EDITOR",          // no scope at all
            "SOME-ROLE",            // a hyphen that is not a scope separator
            "PLAIN",
            "_DISTRICT-DCC",        // nothing before the scope type
            "CSP_ADMIN",
    })
    @DisplayName("leaves a name that is not scope-encoded alone")
    void leavesUnscopedNamesAlone(String roleName) {
        CssRoleNaming.ScopedRoleName parsed = CssRoleNaming.parse(roleName);

        assertThat(parsed.baseRoleName()).isEqualTo(roleName);
        assertThat(parsed.scopes()).isEmpty();
    }

    @Test
    @DisplayName("reads both scopes back out of a compound name, in written order")
    void parsesCompoundName() {
        CssRoleNaming.ScopedRoleName parsed =
                CssRoleNaming.parse("FOM_SUBMITTER_DISTRICT-DCC_FOREST_CLIENT-00001012");

        assertThat(parsed.baseRoleName()).isEqualTo("FOM_SUBMITTER");
        assertThat(parsed.scopes()).containsExactly(
                new CssRoleNaming.Scope("DISTRICT", "DCC"),
                new CssRoleNaming.Scope("FOREST_CLIENT", "00001012"));
    }

    @Test
    @DisplayName("parses all three scopes")
    void parsesAllThreeScopes() {
        CssRoleNaming.ScopedRoleName parsed = CssRoleNaming.parse(
                "FOM_SUBMITTER_DISTRICT-DCC_REGION-KOOTENAY_BOUNDARY_FOREST_CLIENT-00001012");

        assertThat(parsed.baseRoleName()).isEqualTo("FOM_SUBMITTER");
        assertThat(parsed.scopes()).containsExactly(
                new CssRoleNaming.Scope("DISTRICT", "DCC"),
                new CssRoleNaming.Scope("REGION", "KOOTENAY_BOUNDARY"),
                new CssRoleNaming.Scope("FOREST_CLIENT", "00001012"));
    }

    @Test
    @DisplayName("a base role containing a hyphen is still not mistaken for a scope")
    void hyphenInBaseRoleSurvivesCompoundParsing() {
        CssRoleNaming.ScopedRoleName parsed = CssRoleNaming.parse("SOME-ROLE_DISTRICT-DCC");

        assertThat(parsed.baseRoleName()).isEqualTo("SOME-ROLE");
        assertThat(parsed.scopes()).containsExactly(new CssRoleNaming.Scope("DISTRICT", "DCC"));
    }

    @Test
    @DisplayName("a FAM sidecar never parses as the role it describes")
    void sidecarIsNotMistakenForAScopedRole() {
        String label = "FAM:LABEL:FOM_SUBMITTER:Submitter - district";
        String expiry = "FAM:EXPIRES:2026-09-30:CSP_SUBMITTER_FOREST_CLIENT-00012345";

        assertThat(CssRoleNaming.parse(label).scopes()).isEmpty();
        assertThat(CssRoleNaming.parse(expiry).baseRoleName()).isNotEqualTo("CSP_SUBMITTER");
        assertThat(List.of(label, expiry)).allMatch(CssRoleNaming::isSidecarRole);
        assertThat(CssRoleNaming.isSidecarRole("CSP_SUBMITTER_FOREST_CLIENT-00012345")).isFalse();
        assertThat(CssRoleNaming.isSidecarRole(null)).isFalse();
    }
}
