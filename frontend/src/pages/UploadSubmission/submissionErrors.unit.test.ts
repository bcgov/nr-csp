import { describe, expect, it } from 'vitest';

import { type ValidationMessageResponse } from '@/services/invoice.service';

import {
  collectIssueBanners,
  mapSubmissionIssues,
  structuralIssuesToCsv,
  toStructuralIssueRows,
} from './submissionErrors';

const msg = (messageKey: string, message: string, type: 'ERROR' | 'WARNING' = 'ERROR'): ValidationMessageResponse => ({
  messageKey,
  message,
  type,
  args: [],
});

describe('mapSubmissionIssues', () => {
  it('routes a submission-level submitter combo error to both metadata fields, prefix stripped', () => {
    const result = mapSubmissionIssues([
      msg(
        'invoice.submitter.client.location.invalid.error',
        'submission: The combination of the submitter Client Number 00126920 and Client Location 01 cannot be found in CSP.',
      ),
    ]);

    const clean =
      'The combination of the submitter Client Number 00126920 and Client Location 01 cannot be found in CSP.';
    expect(result.submissionFields.submissionClientNumber?.[0]).toEqual({ message: clean, type: 'ERROR' });
    expect(result.submissionFields.submissionClientLocnCode?.[0]).toEqual({ message: clean, type: 'ERROR' });
    // It highlights the fields inline rather than sitting in the banner.
    expect(result.formIssues).toEqual([]);
    expect(result.hasErrors).toBe(true);
  });

  it('keeps an unmapped submission-level message in the banner', () => {
    const result = mapSubmissionIssues([msg('some.other.submission.error', 'submission: Something went wrong.')]);
    expect(result.formIssues).toEqual([{ message: 'Something went wrong.', type: 'ERROR' }]);
    expect(result.submissionFields).toEqual({});
  });

  it('attaches an invoice-located message to the invoice row/field', () => {
    const result = mapSubmissionIssues([
      msg('invoice.fob.required.error', 'invoice #2 (INV-2): FOB location is required.'),
    ]);
    expect(result.invoices[2]?.fields.locationFOB?.[0]).toEqual({
      message: 'FOB location is required.',
      type: 'ERROR',
    });
  });
});

describe('collectIssueBanners', () => {
  it('labels invoice and line-item issues with their row context', () => {
    const issues = mapSubmissionIssues([
      msg('invoice.totalpieces.dismatch.warning', 'invoice #1 (INV-1): total pieces mismatch.', 'WARNING'),
      msg('invoice.price.negative.value.error', 'invoice #2 (INV-2), line 3: price is negative.'),
    ]);

    const banners = collectIssueBanners(issues, { 1: 'INV-1', 2: 'INV-2' });

    expect(banners).toEqual([
      { key: 'inv-1-0', label: 'Invoice #1 (INV-1)', message: 'total pieces mismatch.', type: 'WARNING' },
      { key: 'li-2:3-0', label: 'Invoice #2 (INV-2), line 3', message: 'price is negative.', type: 'ERROR' },
    ]);
  });

  it('falls back to a number-less label when the invoice number is unknown', () => {
    const issues = mapSubmissionIssues([msg('invoice.fob.required.error', 'invoice #5: FOB is required.')]);
    expect(collectIssueBanners(issues, {})[0].label).toBe('Invoice #5');
  });

  it('returns nothing when there are no invoice or line-item issues', () => {
    const issues = mapSubmissionIssues([msg('some.other.submission.error', 'submission: Something went wrong.')]);
    expect(collectIssueBanners(issues, {})).toEqual([]);
  });
});

describe('toStructuralIssueRows', () => {
  it('splits a schema error into issue / location / detail and drops the cvc code', () => {
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

  it('strips a variety of cvc-* prefixes from the detail', () => {
    const rows = toStructuralIssueRows([
      msg('XSD', "line 12, col 5: cvc-fractionDigits-valid: Value '1.00088786' has 8 fraction digits, limited to 6."),
      msg('XSD', "line 13, col 9: cvc-type.3.1.3: The value '1.0' of element 'csp:volume' is not valid."),
    ]);
    expect(rows[0].detail).toBe("Value '1.00088786' has 8 fraction digits, limited to 6.");
    expect(rows[1].detail).toBe("The value '1.0' of element 'csp:volume' is not valid.");
  });

  it('handles a location-less error (whole message is the detail)', () => {
    const rows = toStructuralIssueRows([
      msg('FORMAT_UNRECOGNIZED', "could not detect format — expected XML (starts with '<')"),
    ]);
    expect(rows[0].issue).toBe('Unrecognized format');
    expect(rows[0].location).toBe('');
    expect(rows[0].detail).toBe("could not detect format — expected XML (starts with '<')");
  });

  it('falls back to a generic category for an unknown code', () => {
    expect(toStructuralIssueRows([msg('MYSTERY', 'line 1, col 1: boom')])[0].issue).toBe('XML content error');
  });
});

describe('structuralIssuesToCsv', () => {
  it('emits a header row and escapes commas and quotes', () => {
    const rows = toStructuralIssueRows([msg('XSD', 'line 4, col 33: bad element "foo", again')]);
    const csv = structuralIssuesToCsv(rows);
    const [header, first] = csv.split('\r\n');
    expect(header).toBe('Issue,File location,Detail');
    expect(first).toBe('XML content error,"line 4, col 33","bad element ""foo"", again"');
  });
});
