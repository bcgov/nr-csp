package ca.bc.gov.nrs.csp.backend.util.constants;

/**
 * FAM role constants for CSP.
 * Values must match the suffix of the Cognito group names assigned by FAM
 * (e.g. group "CSP_ADMIN" → role "ADMIN").
 */
public final class Roles {

    private Roles() {}

    public static final String ADMIN   = "ADMIN";
    public static final String APPROVE = "APPROVE";
    public static final String VIEW    = "VIEW";
}
