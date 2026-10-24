package ca.bc.gov.nrs.csp.backend.util.constants;

/**
 * FAM role constants for CSP.
 * FAM client codes map exactly to these existing internal permissions
 * (e.g. code "CSP_ADMIN" → role "ADMIN"). SUBMITTER comes from the Business BCeID code
 * CSP_SUBMITTER_FOREST_CLIENT-00012345, with one FOREST_CLIENT- authority per client.
 */
public final class Roles {

    private Roles() {}

    public static final String ADMIN   = "ADMIN";
    public static final String APPROVE = "APPROVE";
    public static final String VIEW    = "VIEW";
    public static final String SUBMITTER = "SUBMITTER";

    /** Prefix of a submitter's per-client authority. */
    public static final String FOREST_CLIENT_PREFIX = "FOREST_CLIENT-";
}
