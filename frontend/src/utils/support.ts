import type { Role } from '@/context/auth/permissions';

// Built from docs/manual by `npm run manual:pdf`.
const USER_MANUALS: { name: string; href: string; role: Role }[] = [
  { name: 'Admin', href: '/manuals/CSP-Admin-User-Manual.pdf', role: 'ADMIN' },
  { name: 'Approver', href: '/manuals/CSP-Approver-User-Manual.pdf', role: 'APPROVE' },
  { name: 'Data Entry', href: '/manuals/CSP-Data-Entry-User-Manual.pdf', role: 'ENTRY' },
  { name: 'Viewer', href: '/manuals/CSP-Viewer-User-Manual.pdf', role: 'VIEW' },
  { name: 'Industry', href: '/manuals/CSP-Industry-User-Manual.pdf', role: 'SUBMITTER' },
];

/** One download link per user manual the roles map to; labelled by manual when there are several. */
export const userManualsForRoles = (privileges: readonly Role[] | undefined): { label: string; href: string }[] => {
  const manuals = USER_MANUALS.filter((manual) => privileges?.includes(manual.role));
  return manuals.map((manual) => ({
    label: manuals.length === 1 ? 'User manual' : `User manual (${manual.name})`,
    href: manual.href,
  }));
};
