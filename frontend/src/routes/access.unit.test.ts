import { describe, expect, it } from 'vitest';

import { ROLES, type Role } from '@/context/auth/permissions';

import { ROUTE_PAGES, canAccess, homeRouteFor, isIndustryPath } from './access';
import { ROUTES } from './routePaths';

import type { AuthUser } from '@/context/auth/types';

const userWith = (privileges: Role[]): AuthUser => ({
  username: 'u',
  email: '',
  roles: [],
  privileges,
  forestClientNumbers: privileges.includes('SUBMITTER') ? ['00000001'] : [],
});

const INDUSTRY_ROUTES = [ROUTES.UPLOAD_SUBMISSION, ROUTES.SUBMISSION_HISTORY];

describe('ROUTE_PAGES', () => {
  const protectedPaths = Object.keys(ROUTE_PAGES);

  it('names a page for every route except the public ones', () => {
    const publicRoutes: string[] = [ROUTES.LANDING, ROUTES.LOGOUT, ROUTES.NOT_FOUND];
    expect(protectedPaths.sort()).toEqual(
      Object.values(ROUTES)
        .filter((path) => !publicRoutes.includes(path))
        .sort(),
    );
  });

  it('lets a SUBMITTER open only Upload Submission and Submission History', () => {
    const submitter = userWith(['SUBMITTER']);
    const open = protectedPaths.filter((path) => canAccess(submitter, ROUTE_PAGES[path as keyof typeof ROUTE_PAGES]));
    expect(open.sort()).toEqual([...INDUSTRY_ROUTES].sort());
  });

  it.each(['ADMIN', 'APPROVE', 'VIEW'] as Role[])('lets %s open every route, as before', (role) => {
    const staff = userWith([role]);
    for (const path of protectedPaths) {
      expect(canAccess(staff, ROUTE_PAGES[path as keyof typeof ROUTE_PAGES]), path).toBe(true);
    }
  });

  it('lets nobody without a privilege open anything', () => {
    for (const page of Object.values(ROUTE_PAGES)) {
      expect(canAccess(userWith([]), page)).toBe(false);
      expect(canAccess(null, page)).toBe(false);
    }
  });

  it('covers every role', () => {
    expect([...ROLES]).toEqual(['ADMIN', 'APPROVE', 'VIEW', 'SUBMITTER']);
  });
});

describe('homeRouteFor', () => {
  it('sends a submitter to Upload Submission and everyone else to Search', () => {
    expect(homeRouteFor(userWith(['SUBMITTER']))).toBe(ROUTES.UPLOAD_SUBMISSION);
    expect(homeRouteFor(userWith(['VIEW']))).toBe(ROUTES.SEARCH);
    expect(homeRouteFor(userWith([]))).toBe(ROUTES.SEARCH);
    expect(homeRouteFor(null)).toBe(ROUTES.SEARCH);
  });
});

describe('isIndustryPath', () => {
  it.each([ROUTES.UPLOAD_SUBMISSION, ROUTES.SUBMISSION_HISTORY, `${ROUTES.SUBMISSION_HISTORY}/9001`])(
    'is true for %s',
    (path) => expect(isIndustryPath(path)).toBe(true),
  );

  it.each([
    ROUTES.SEARCH,
    ROUTES.INBOX,
    `${ROUTES.INVOICE}/1`,
    '/submission-historyx',
    '/',
    ROUTES.R06_INVOICE_PRINT_OUT,
  ])('is false for %s', (path) => expect(isIndustryPath(path)).toBe(false));
});
