import type { ReactNode } from 'react';

import { env } from '@/env';

import { MockAuthProvider } from './MockAuthProvider';
import { RealAuthProvider } from './RealAuthProvider';

export function AuthProvider({ children }: { children: ReactNode }) {
  return env.mockUser ? (
    <MockAuthProvider>{children}</MockAuthProvider>
  ) : (
    <RealAuthProvider>{children}</RealAuthProvider>
  );
}
