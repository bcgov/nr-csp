import { describe, expect, it } from 'vitest';

import { userManualsForRoles } from './support';

describe('userManualsForRoles', () => {
  it('offers one manual, labelled plainly, for a single role', () => {
    expect(userManualsForRoles(['APPROVE'])).toEqual([
      { label: 'User manual', href: '/manuals/CSP-Approver-User-Manual.pdf' },
    ]);
    expect(userManualsForRoles(['SUBMITTER'])).toEqual([
      { label: 'User manual', href: '/manuals/CSP-Industry-User-Manual.pdf' },
    ]);
  });

  it('names each manual when the roles map to several', () => {
    expect(userManualsForRoles(['VIEW', 'ENTRY'])).toEqual([
      { label: 'User manual (Data Entry)', href: '/manuals/CSP-Data-Entry-User-Manual.pdf' },
      { label: 'User manual (Viewer)', href: '/manuals/CSP-Viewer-User-Manual.pdf' },
    ]);
  });

  it('offers nothing without a CSP role', () => {
    expect(userManualsForRoles([])).toEqual([]);
    expect(userManualsForRoles(undefined)).toEqual([]);
  });
});
