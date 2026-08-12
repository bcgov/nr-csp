import { createContext } from 'react';

// temp comment DELETE ME

import type { AuthContextValue } from './types';

export type AuthContextType = AuthContextValue;

export const AuthContext = createContext<AuthContextType | null>(null);
