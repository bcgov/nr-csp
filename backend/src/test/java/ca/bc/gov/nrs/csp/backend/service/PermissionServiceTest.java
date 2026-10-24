package ca.bc.gov.nrs.csp.backend.service;

import ca.bc.gov.nrs.csp.backend.util.constants.Roles;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;

import static ca.bc.gov.nrs.csp.backend.util.constants.PermissionConstants.*;
import static org.junit.jupiter.api.Assertions.*;

class PermissionServiceTest {

    private PermissionService service;

    @BeforeEach
    void setUp() {
        service = new PermissionService();
    }

    private Authentication auth(String role) {
        return new UsernamePasswordAuthenticationToken(
                "user", null, List.of(new SimpleGrantedAuthority(role)));
    }

    // ── ADMIN ────────────────────────────────────────────────────────────────

    @Test
    void admin_has_tableMaintenance() {
        assertTrue(service.hasPermission(auth(Roles.ADMIN), TABLE_MAINTENANCE));
    }

    @Test
    void admin_has_esfSubmit() {
        assertTrue(service.hasPermission(auth(Roles.ADMIN), ESF_SUBMIT));
    }

    @Test
    void admin_has_invoiceDetails_approve() {
        assertTrue(service.hasPermission(auth(Roles.ADMIN), INVOICE_DETAILS_APPROVE));
    }

    @Test
    void admin_has_modelFlatPriceConv_edit() {
        assertTrue(service.hasPermission(auth(Roles.ADMIN), MODEL_FLAT_PRICE_CONV_EDIT));
    }

    @Test
    void admin_has_r14_search() {
        assertTrue(service.hasPermission(auth(Roles.ADMIN), R14_SEARCH));
    }

    // ── APPROVE ──────────────────────────────────────────────────────────────

    @Test
    void approve_has_invoiceDetails_approve() {
        assertTrue(service.hasPermission(auth(Roles.APPROVE), INVOICE_DETAILS_APPROVE));
    }

    @Test
    void approve_has_invoiceDetails_reject() {
        assertTrue(service.hasPermission(auth(Roles.APPROVE), INVOICE_DETAILS_REJECT));
    }

    @Test
    void approve_does_not_have_tableMaintenance() {
        assertFalse(service.hasPermission(auth(Roles.APPROVE), TABLE_MAINTENANCE));
    }

    @Test
    void approve_does_not_have_esfSubmit() {
        assertFalse(service.hasPermission(auth(Roles.APPROVE), ESF_SUBMIT));
    }

    @Test
    void approve_does_not_have_modelFlatPriceConv_edit() {
        assertFalse(service.hasPermission(auth(Roles.APPROVE), MODEL_FLAT_PRICE_CONV_EDIT));
    }

    @Test
    void approve_does_not_have_r14_search() {
        assertFalse(service.hasPermission(auth(Roles.APPROVE), R14_SEARCH));
    }

    @Test
    void approve_has_r13_clear() {
        assertTrue(service.hasPermission(auth(Roles.APPROVE), R13_CLEAR));
    }

    @Test
    void approve_does_not_have_r07_clear() {
        assertFalse(service.hasPermission(auth(Roles.APPROVE), R07_CLEAR));
    }

    // ── VIEW ─────────────────────────────────────────────────────────────────

    @Test
    void view_has_invoiceDetails_page() {
        assertTrue(service.hasPermission(auth(Roles.VIEW), INVOICE_DETAILS));
    }

    @Test
    void view_has_invoiceDetails_csv() {
        assertTrue(service.hasPermission(auth(Roles.VIEW), INVOICE_DETAILS_CSV));
    }

    @Test
    void view_does_not_have_invoiceDetails_approve() {
        assertFalse(service.hasPermission(auth(Roles.VIEW), INVOICE_DETAILS_APPROVE));
    }

    @Test
    void view_does_not_have_invoiceDetails_save() {
        assertFalse(service.hasPermission(auth(Roles.VIEW), INVOICE_DETAILS_SAVE));
    }

    @Test
    void view_does_not_have_tableMaintenance() {
        assertFalse(service.hasPermission(auth(Roles.VIEW), TABLE_MAINTENANCE));
    }

    @Test
    void view_has_r13_clear() {
        assertTrue(service.hasPermission(auth(Roles.VIEW), R13_CLEAR));
    }

    // ── SUBMITTER (Business BCeID industry) ──────────────────────────────────

    @Test
    void submitter_has_only_upload_and_history() {
        assertEquals(java.util.Set.of(SUBMISSION_UPLOAD, SUBMISSION_HISTORY), SUBMITTER_ACTIONS);
        assertTrue(service.hasPermission(auth(Roles.SUBMITTER), SUBMISSION_UPLOAD));
        assertTrue(service.hasPermission(auth(Roles.SUBMITTER), SUBMISSION_HISTORY));
    }

    @Test
    void submitter_has_no_staff_action() {
        for (String action : ADMIN_ACTIONS) {
            if (SUBMITTER_ACTIONS.contains(action)) continue;
            assertFalse(service.hasPermission(auth(Roles.SUBMITTER), action), action);
        }
    }

    @Test
    void every_staff_role_keeps_upload_and_history() {
        for (String role : List.of(Roles.ADMIN, Roles.APPROVE, Roles.VIEW)) {
            assertTrue(service.hasPermission(auth(role), SUBMISSION_UPLOAD), role);
            assertTrue(service.hasPermission(auth(role), SUBMISSION_HISTORY), role);
        }
    }

    @Test
    void forest_client_authority_grants_no_action() {
        assertFalse(service.hasPermission(auth(Roles.FOREST_CLIENT_PREFIX + "00000001"), SUBMISSION_UPLOAD));
    }

    // ── Edge cases ────────────────────────────────────────────────────────────

    @Test
    void null_auth_returns_false() {
        assertFalse(service.hasPermission(null, INVOICE_DETAILS_APPROVE));
    }

    @Test
    void unknown_action_returns_false_for_any_role() {
        assertFalse(service.hasPermission(auth(Roles.ADMIN), "nonExistentPage/FakeAction"));
    }

    @Test
    void unknown_role_returns_false() {
        assertFalse(service.hasPermission(auth("CSP_UNKNOWN"), INVOICE_DETAILS_APPROVE));
    }
}
