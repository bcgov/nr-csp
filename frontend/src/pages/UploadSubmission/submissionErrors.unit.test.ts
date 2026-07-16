import { describe, expect, it } from 'vitest';

import { type ValidationMessageResponse } from '@/services/invoice.service';

import {
  collectIssueBanners,
  mapSubmissionIssues,
  structuralIssuesToCsv,
  toStructuralIssueRows,
  type StructuralIssueRow,
} from './submissionErrors';

const msg = (messageKey: string, message: string, type: 'ERROR' | 'WARNING' = 'ERROR'): ValidationMessageResponse => ({
  messageKey,
  message,
  type,
  args: [],
});

describe('toStructuralIssueRows', () => {
  it('splits a "line N, col N: cvc-...: detail" message and strips the cvc code', () => {
    const rows = toStructuralIssueRows([
      msg('XSD', "line 4, col 33: cvc-complex-type.2.4.a: Invalid content was found starting with element 'x'."),
    ]);
    expect(rows[0]).toEqual({
      id: 'structural-0',
      issue: 'XML content error',
      location: 'line 4, col 33',
      detail: "Invalid content was found starting with element 'x'.",
    });
  });

  it('leaves location empty and keeps the whole message as detail when there is no location prefix', () => {
    const rows = toStructuralIssueRows([
      msg('FORMAT_UNRECOGNIZED', "could not detect format — expected XML (starts with '<')"),
    ]);
    expect(rows[0]).toEqual({
      id: 'structural-0',
      issue: 'Unrecognized format',
      location: '',
      detail: "could not detect format — expected XML (starts with '<')",
    });
  });

  it('maps known message keys to their human category', () => {
    const rows = toStructuralIssueRows([msg('XSD', 'bad content'), msg('ENVELOPE_NO_BODY', 'no body')]);
    expect(rows[0].issue).toBe('XML content error');
    expect(rows[1].issue).toBe('Envelope error');
  });

  it('defaults an unknown message key to "XML content error"', () => {
    expect(toStructuralIssueRows([msg('MYSTERY', 'line 1, col 1: boom')])[0].issue).toBe('XML content error');
  });

  it('assigns sequential ids across multiple errors', () => {
    const rows = toStructuralIssueRows([msg('XSD', 'a'), msg('JAXB', 'b')]);
    expect(rows.map((r) => r.id)).toEqual(['structural-0', 'structural-1']);
  });
});

describe('structuralIssuesToCsv', () => {
  it('emits a header row plus data rows joined by CRLF, quoting cells that need it', () => {
    const rows: StructuralIssueRow[] = [
      // Plain cells (no quoting) alongside a cell with a comma, a quote and a newline.
      { id: 'structural-0', issue: 'XML content error', location: 'line 4, col 33', detail: 'a "quote", and\nnewline' },
      { id: 'structural-1', issue: 'Envelope error', location: '', detail: 'plain detail' },
    ];
    const csv = structuralIssuesToCsv(rows);
    // Records are joined with CRLF; the embedded LF stays inside the quoted cell,
    // so splitting on CRLF yields header, row 0 (with its literal newline), row 1.
    const parts = csv.split('\r\n');

    expect(parts[0]).toBe('Issue,File location,Detail');
    // 'XML content error' is plain (no quoting); location has a comma -> quoted;
    // detail has a comma/quote/newline -> quoted with the quote doubled.
    expect(parts[1]).toBe('XML content error,"line 4, col 33","a ""quote"", and\nnewline"');
    // Row 1: all plain cells, empty location cell rendered as an empty field.
    expect(parts[2]).toBe('Envelope error,,plain detail');
  });
});

describe('mapSubmissionIssues', () => {
  it('attributes a line-level message to lineItems and the mapped field (LINE_KEY_TO_FIELD)', () => {
    const result = mapSubmissionIssues([msg('invoice.grade.z.warning', 'invoice #2 (INV-1), line 3: grade Z.', 'WARNING')]);
    expect(result.lineItems['2:3']).toEqual({ fields: { grade: [{ message: 'grade Z.', type: 'WARNING' }] }, row: [] });
    expect(result.hasErrors).toBe(false);
  });

  it('attaches an unmapped line-level key at the row level', () => {
    const result = mapSubmissionIssues([msg('invoice.unknown.line.error', 'invoice #2 (INV-1), line 3: mystery.')]);
    expect(result.lineItems['2:3']).toEqual({ fields: {}, row: [{ message: 'mystery.', type: 'ERROR' }] });
  });

  it('attributes an invoice-level message to invoices and the mapped field (INVOICE_KEY_TO_FIELD)', () => {
    const result = mapSubmissionIssues([msg('invoice.fob.required.error', 'invoice #2 (INV-1): FOB required.')]);
    expect(result.invoices[2]).toEqual({ fields: { locationFOB: [{ message: 'FOB required.', type: 'ERROR' }] }, row: [] });
  });

  it('attaches an unmapped invoice-level key at the row level', () => {
    const result = mapSubmissionIssues([msg('invoice.unknown.error', 'invoice #2: mystery invoice.')]);
    expect(result.invoices[2]).toEqual({ fields: {}, row: [{ message: 'mystery invoice.', type: 'ERROR' }] });
  });

  it('maps a submission-level single-field key to that metadata field (SUBMISSION_KEY_TO_FIELD)', () => {
    const result = mapSubmissionIssues([
      msg('invoice.month.completed.warning', 'submission: Month not complete.', 'WARNING'),
    ]);
    expect(result.submissionFields).toEqual({ monthComplete: [{ message: 'Month not complete.', type: 'WARNING' }] });
    expect(result.formIssues).toEqual([]);
  });

  it('maps a submission-level array key to every listed metadata field', () => {
    const clean = 'The combination of the submitter Client Number and Client Location cannot be found in CSP.';
    const result = mapSubmissionIssues([
      msg('invoice.submitter.client.location.invalid.error', `submission: ${clean}`),
    ]);
    expect(result.submissionFields.submissionClientNumber?.[0]).toEqual({ message: clean, type: 'ERROR' });
    expect(result.submissionFields.submissionClientLocnCode?.[0]).toEqual({ message: clean, type: 'ERROR' });
    expect(result.formIssues).toEqual([]);
    expect(result.hasErrors).toBe(true);
  });

  it('routes an unmapped submission-level key to formIssues', () => {
    const result = mapSubmissionIssues([msg('some.other.submission.error', 'submission: Something went wrong.')]);
    expect(result.formIssues).toEqual([{ message: 'Something went wrong.', type: 'ERROR' }]);
    expect(result.submissionFields).toEqual({});
  });

  it('treats a message with no ": " separator as an unlocated form issue (whole body)', () => {
    const result = mapSubmissionIssues([msg('some.error', 'A bare message with no locator')]);
    expect(result.formIssues).toEqual([{ message: 'A bare message with no locator', type: 'ERROR' }]);
  });

  it('treats a non-locator, non-submission prefix as the whole message (form issue)', () => {
    const result = mapSubmissionIssues([msg('some.error', 'Heads up: this is not a locator prefix')]);
    expect(result.formIssues).toEqual([{ message: 'Heads up: this is not a locator prefix', type: 'ERROR' }]);
  });

  it('strips a bare "submission" locator prefix from the body', () => {
    const result = mapSubmissionIssues([msg('some.other.submission.error', 'submission: Cleaned body.')]);
    expect(result.formIssues).toEqual([{ message: 'Cleaned body.', type: 'ERROR' }]);
  });

  it('sets hasErrors true when any message is an ERROR', () => {
    const warn = mapSubmissionIssues([msg('invoice.grade.z.warning', 'invoice #1, line 1: warn.', 'WARNING')]);
    expect(warn.hasErrors).toBe(false);
    const err = mapSubmissionIssues([msg('invoice.fob.required.error', 'invoice #1: FOB required.')]);
    expect(err.hasErrors).toBe(true);
  });
});

describe('collectIssueBanners', () => {
  it('labels invoice and line issues, with and without an invoice number, sorted numerically', () => {
    const issues = mapSubmissionIssues([
      // Deliberately out of numeric order to exercise the numeric sort.
      msg('invoice.totalpieces.dismatch.warning', 'invoice #10 (INV-10): pieces mismatch.', 'WARNING'),
      msg('invoice.fob.required.error', 'invoice #2 (INV-1): FOB required.'),
      msg('invoice.price.negative.value.error', 'invoice #2 (INV-1), line 3: price negative.'),
      // No invoice number provided -> number-less label.
      msg('invoice.fob.required.error', 'invoice #5: FOB required.'),
    ]);

    const banners = collectIssueBanners(issues, { 2: 'INV-1', 10: 'INV-10' });

    // Invoices iterated in numeric-sorted order: 2, 5, 10 (then line items).
    expect(banners.map((b) => b.label)).toEqual([
      'Invoice #2 (INV-1)',
      'Invoice #5',
      'Invoice #10 (INV-10)',
      'Invoice #2 (INV-1), line 3',
    ]);
    expect(banners[0]).toEqual({ key: 'inv-2-0', label: 'Invoice #2 (INV-1)', message: 'FOB required.', type: 'ERROR' });
    expect(banners[3]).toEqual({
      key: 'li-2:3-0',
      label: 'Invoice #2 (INV-1), line 3',
      message: 'price negative.',
      type: 'ERROR',
    });
  });

  it('returns nothing when there are no invoice or line issues', () => {
    const issues = mapSubmissionIssues([msg('some.other.submission.error', 'submission: Something went wrong.')]);
    expect(collectIssueBanners(issues, {})).toEqual([]);
  });
});
