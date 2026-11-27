---
name: update-manual
description: Update the CSP end-user manuals in docs/manual/ (industry edition for Business BCeID submitters, ministry edition for IDIR staff) after a change to user-visible behaviour (screens, menu items, fields, buttons, workflows, statuses, validation and XML submission messages, roles, reports). Use whenever frontend/src or backend/src/main changed in a way a user would notice, or when the check-manual Stop hook asks for it. Ends with a secrets and personal-data sweep of the changed text, screenshots and PDFs.
---

# Updating the CSP user manuals

The manuals in `docs/manual/` are for **end users**, not developers. There are
two editions, one per sign-in:

| Edition | Folder | Readers |
|---|---|---|
| Industry | `docs/manual/industry/` | Business BCeID users with the Submitter role, who send invoices for their forest clients |
| Ministry | `docs/manual/ministry/` | IDIR staff: Admin, Approver, Data Entry, Viewer |

The ministry edition is the source for four PDFs, one for each staff role.
CSP offers each user the PDF for their role under **Support** in the menu
(`userManualsForRoles` in `frontend/src/utils/support.ts`).

Keep both in step with the code in the same change.

## 1. Decide whether the manuals need to change

List what changed: `git diff --name-only HEAD` plus untracked files.

**Update the manuals** when a change alters anything a user sees or does:
- a screen added, removed, renamed or moved in the menu
- search criteria, columns, form fields, required fields, defaults, limits or
  validation messages
- a button or action added, removed or relabelled, or its availability
  (role, invoice or submission status)
- a workflow or status change (submission, invoice, line item)
- an XML submission rule or message a submitter sees after an upload
- a role or what a role can do
- a report added, removed or renamed, its options or its output formats
- signing in, the profile panel or session time-out

**Don't update the manuals** for refactors, tests, performance work, logging,
build/CI/deploy changes, backend changes with the same results, mock sign-in
for local development, or developer docs. In that case say in one sentence why
no user-visible behaviour changed, and stop.

A change usually lands in one edition only. A screen both audiences use
(Upload Submission, Submission History, a submission's details) is described
in both, each from its reader's side.

## 2. Find the chapter(s) to edit

| Source | Industry | Ministry |
|---|---|---|
| `pages/Welcome`, `pages/NotFound`, `components/Layout/**` (menu, header, profile panel, theme), `components/core/**`, `components/Form/**`, `context/auth/**` other than the role files below (sign-in, session time-out), `routes/ProtectedRoute.tsx`, `utils/support.ts` | `01-getting-started.md` | `01-getting-started.md` |
| `pages/Search`, `backend/.../service/SearchService.java` | none | `02-search.md` |
| `pages/Inbox`, `backend/.../service/InboxService.java` | none | `03-inbox.md` |
| `pages/Invoice`, `validations/invoice/**`, `backend/.../service/{InvoiceService,PriceConversionService}.java`, `backend/.../invoice/{manual,shared}/**` | none | `04-invoices.md` |
| `pages/FlatPriceConversion`, `pages/SortCode`, `backend/.../service/{FlatPriceConversion,SortCode}*.java` | none | `05-table-maintenance.md` |
| `pages/R*`, `validations/reports/**`, `services/reportValidation.ts`, `utils/report.ts`, `backend/.../service/R*Service.java`, `backend/.../service/reporting/**`, `backend/.../util/validation/reports/**`, `backend/src/main/resources/reports/**` | none | `06-reports.md` |
| `pages/UploadSubmission`, `pages/SubmissionHistory`, `pages/ViewSubmission`, `validations/submission.ts`, `backend/.../invoice/submission/**`, `backend/.../service/{CspSubmissionPersistenceService,SubmissionHistoryService}.java` | `02-submissions.md` | `07-submissions.md` |
| `backend/src/main/resources/messages.properties`, `validations/messages.ts`, `pages/*/messageKeyMap.ts` | the chapter of the screen that shows the message | same |
| Role files: `context/auth/{permissions,famRoles}.ts`, `routes/{access,navigation}.ts`, `ROLE_LABELS` in `components/Layout/HeaderPanelProfile`, `backend/.../util/constants/{PermissionConstants,Roles}.java`, `backend/.../security/**`, `backend/.../config/SecurityConfig.java` | `01-getting-started.md` | `08-who-can-do-what.md` (and the affected chapter) |
| A new area in the menu | New chapter: add it to that edition's `README.md` and this table |

Frontend paths are under `frontend/src/`; `backend/...` =
`backend/src/main/java/ca/bc/gov/nrs/csp/backend/`.

The page components are the source of truth for what users see: read the
labels, helper text, messages and the conditions around them (role, invoice
status, manual or electronic invoice) rather than guessing. What each role is
granted is `ROLE_PERMISSIONS` in `frontend/src/context/auth/permissions.ts`,
which mirrors `PermissionConstants.java`; the menu is `NAVIGATION_ITEMS` in
`routes/navigation.ts`, and the page each menu item needs is in
`routes/access.ts`; role display names are `ROLE_LABELS` in
`components/Layout/HeaderPanelProfile/index.tsx`.

## 3. Style

- Write for the user. Use the on-screen labels exactly, in **bold**:
  **Save**, **Upload Submission**, **Reports › R07 - Reconciliation**.
- Use role display names ("Approver", "Data Entry"), never `CSP_APPROVE`,
  `ENTRY` or an action string such as `invoiceDetails/Approve`.
- Never mention legacy screens, database tables, procedures, class names, API
  paths or file paths.
- Each chapter: a short intro (what the area is for, who uses it) → a
  **Screens** table (menu item → what it's for, plus a "Who" column where
  access differs) → **Common tasks** as numbered steps.
- Put only menu screens in the Screens table. Describe screens reached from a
  result or a button (an invoice, a submission's details, side panels,
  dialogs) inside the task that uses them.
- Notes and tips are blockquotes: `> **Tip:** …`.
- Short sentences, active voice, present tense. Canadian spelling.
- Don't describe what is planned or not yet built. Describe the app as it is
  in this change.
- Industry edition: only what a Submitter can see and do. Staff-only steps
  appear there as what staff do next ("Ministry staff review and approve each
  invoice"), never as instructions.
- Ministry edition: mark what each role gets with `roles` comments, as
  described in `docs/manual/README.md`. A role's manual holds only what that
  role can see and do, so mark a task, table row or sentence for the roles
  that have it (`ROLE_PERMISSIONS`), and write a short variant where a role
  sees the screen but can't change it. Write to "you", not "Approvers can".

## 4. Screenshots and the PDFs

Screenshots live in `docs/manual/<edition>/images/` and are referenced as
`![Caption](images/NN-name.png)`. The caption doubles as the figure caption
in the PDF. They are produced by `frontend/e2e/manual/manual.screenshots.spec.ts`;
don't hand-edit images.

- **Both editions** come from a synthetic session: the built app, signed in
  with a sample token (the industry edition as a Submitter, the ministry
  edition as an Admin), with every API call answered from
  `frontend/e2e/manual/manual-fixtures.ts`. The capture serves the app to the
  browser itself, so it needs no server, backend or credentials.
- If a change alters a screen that has a screenshot, or adds one worth
  showing, add or update its `SHOTS` entry and the fixture data for any API
  call the screen makes. A call with no fixture fails the capture and names
  the path.
- Keep every fixture value fictional ("Jordan Sample", "Pat Example",
  "Example Forestry Ltd.", `example.com`, client numbers such as `00000001`,
  made-up invoice numbers and timber marks). Never paste a record from TEST,
  PROD or the seeded e2e database into it: they hold real business data.
- Recapture: `cd frontend && npm run manual:screenshots`. It builds the app
  first. It needs the e2e packages (`npm --prefix e2e ci`) and Playwright's
  Chromium.
- The ministry images are captured as an Admin and shared by the four role
  manuals. Mark an image line for the roles that have that screen.
- Rebuild the PDFs after any manual change: `cd frontend && npm run manual:pdf`
  (writes the industry manual and the four role manuals to
  `frontend/public/manuals/`). Figures are at most 100 mm tall, so keep
  a capture to about one screen; frame a lower part of a page with `focus`.
  Every build rewrites all five PDFs, so if they conflict in a merge, take
  either side and rebuild them.
- `npm run manual:check` runs the build's role, link and table checks without
  a browser. Run it after every ministry edit.
- The capture and the PDF build launch Chromium. If it can't start where you
  are running (a sandboxed session), say which images and PDFs are now out of
  date and give the user the commands to run.

## 5. Secrets and personal-data sweep

The repository is public, so run this sweep on every manual change before you
report done: every new or changed chapter, image and the rebuilt PDFs. Fix
anything found; never just mention it.

1. **Text.** Grep the changed manual files (and the capture code if you
   touched it):
   `git diff --name-only HEAD -- docs/manual frontend/e2e/manual`
   plus untracked files, against
   `password|secret|token|api[_-]?key|bearer|BEGIN .*PRIVATE|eyJ[A-Za-z0-9_-]{10,}|AKIA|ghp_|sk-|jdbc:|@gov\.bc\.ca|IDIR\\[A-Z]|BCEID\\[A-Z0-9]`.
   Matches that only name a concept ("token", `IDIR\NAME`) are fine. Any real
   credential, person, personal mailbox, client or internal host is not.
   Never print a credential value you find, only the file and line.
2. **Screenshots: look at every new or changed image** (Read the PNG) and
   check it field by field. Reject it and fix the capture if it shows any of:
   - a real person's name, user id, email, phone number or address (only the
     sample identities `JSAMPLE` / `PEXAMPLE`, "Jordan Sample", "Pat Example"
     and `example.com` addresses are allowed);
   - a real client or company name, client number, invoice number or timber
     mark (sample values only), or free text copied from a real record;
   - a token, cookie, URL with query secrets, devtools, an error stack trace,
     the mock role selector or the browser address bar.
3. **Image metadata.** PNGs must carry no `tEXt`/`iTXt`/`zTXt`/`eXIf`
   chunks (Playwright writes none; anything else added them).
4. **PDFs.** After `npm run manual:pdf`, check each PDF in
   `frontend/public/manuals/`: its decompressed streams
   hold no `/Users/`, `file://` or local usernames, and that its Info has only
   Title, Creator, Producer and the build dates.

Report the sweep result in your summary (clean, or what you fixed).

## 6. Finish

- Edit only the chapters affected. Keep the tables in step: a removed screen
  leaves no row behind.
- If you added or renamed a chapter, update that edition's `README.md`.
- A new role or a renamed manual also changes `userManualsForRoles` and the
  `MANUALS` list in `frontend/e2e/manual/build-manual-pdf.mjs`.
- In your summary to the user, list which manual sections you changed, by
  edition, and for the ministry edition which roles get them.
