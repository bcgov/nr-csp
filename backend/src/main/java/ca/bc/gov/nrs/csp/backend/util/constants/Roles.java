package ca.bc.gov.nrs.csp.backend.util.constants;

/**
 * Internal CSP roles, granted from FAM role codes by {@code CspAuthorities}.
 *
 * <p>IDIR staff hold unscoped codes that map exactly to these permissions
 * (e.g. code "CSP_ADMIN" → role "ADMIN"). Business BCeID industry users hold
 * {@link #SUBMITTER} only, through the forest-client-scoped FAM code
 * "CSP_SUBMITTER_FOREST_CLIENT-&lt;number&gt;", one per client, and carry one
 * {@link #FOREST_CLIENT_PREFIX} authority per client.
 */
public final class Roles {

    private Roles() {}

    public static final String ADMIN   = "ADMIN";
    public static final String APPROVE = "APPROVE";
    public static final String VIEW    = "VIEW";
    public static final String SUBMITTER = "SUBMITTER";

    /** Prefix of the authority naming one forest client a submitter may act for. */
    public static final String FOREST_CLIENT_PREFIX = "FOREST_CLIENT-";
}
