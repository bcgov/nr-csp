import type { Role } from './permissions';

export interface AuthUser {
  username: string;
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
  signIn: () => Promise<void>;
  signOut: () => Promise<void>;
}
