import { createContext } from 'react';

import type { AuthContextValue } from './types';
//blah blahb;ah delete me

export type AuthContextType = AuthContextValue;

export const AuthContext = createContext<AuthContextType | null>(null);
