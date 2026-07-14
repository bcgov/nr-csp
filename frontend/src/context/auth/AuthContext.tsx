import { createContext } from 'react';

import type { AuthContextValue } from './types';

export type AuthContextType = AuthContextValue;

export const AuthContext = createContext<AuthContextType | null>(null);
