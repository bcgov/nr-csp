import type { Role } from './permissions';

export interface AuthUser {
  username: string;
  /**
   * IDIR username from the `custom:idp_username` id-token claim (e.g. "JSMITH").
   * This is the value the backend resolves as the principal and writes to audit
   * fields like `entryUserID`, so prefer it over {@link username} (a Cognito id)
   * anywhere the UI shows who entered or submitted a record.
   */
  idirUsername?: string;
  displayName?: string;
  email: string;
  /** Raw Cognito group names from the id-token (e.g. "CSP_SUBMITTER"). */
  roles: string[];
  /**
   * Derived app-level permissions extracted from {@link roles}.
   * Values are uppercase role constants: "ADMIN" | "APPROVE" | "VIEW".
   */
  privileges: Role[];
}

export interface AuthContextValue {
  user: AuthUser | null;
  isAuthenticated: boolean;
  isLoading: boolean;
  isSigningOut: boolean;
  signIn: () => Promise<void>;
  signOut: () => Promise<void>;
}
