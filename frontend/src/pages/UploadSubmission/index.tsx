import { Download } from '@carbon/icons-react';
import { Button, Column, Grid, InlineLoading, InlineNotification, Link, TextInput } from '@carbon/react';
import { useState } from 'react';
import { useNavigate } from 'react-router-dom';

import DataPreviewTable, { type DataPreviewColumn, type RowIssues } from '@/components/core/DataPreviewTable';
import FileDropZone from '@/components/core/FileDropZone';
import PageTitle from '@/components/core/PageTitle';
import { ROUTES } from '@/routes/routePaths';
import {
  parseSubmission,
  submissionErrorBody,
  submitSubmission,
  validateSubmissionBusiness,
  type ParsedInvoice,
  type ParsedLineItem,
  type ParsedSubmission,
  type SubmissionParseResponse,
  type SubmissionSubmitResponse,
  type SubmissionValidationResponse,
} from '@/services/cspSubmission.service';
import { type ValidationMessageResponse } from '@/services/invoice.service';
import { formatCurrency, formatNumber } from '@/utils/format';
import { downloadBlob } from '@/utils/report';
import { SUBMISSION_METADATA_KEY_TO_FIELD, validateSubmissionMetadata } from '@/validations/submission';
import { splitMessages } from '@/validations/validationResult';

import InvoiceDetailPanel from './InvoiceDetailPanel';
import { InvoiceIssueBadge } from './InvoiceIssues';
import {
  collectInvoiceIssues,
  hasHardErrors,
  invoiceSeverity,
  mapSubmissionIssues,
  structuralIssuesToCsv,
  submissionSeverity,
  toStructuralIssueRows,
  type InvoiceSeverity,
  type MappedIssues,
  type StructuralIssueRow,
} from './submissionErrors';

import './index.scss';

type InvoiceRow = ParsedInvoice & { id: string };
type LineItemRow = ParsedLineItem & { id: string };

/** Where we are in the upload → parse → validate → submit pipeline. */
type Status = 'idle' | 'parsing' | 'structural-error' | 'validating' | 'done' | 'submitting' | 'network-error';

/**
 * The submission metadata fields the user can edit after a file is parsed.
 * Invoice / line-item rows stay read-only, so only these six are editable.
 */
interface EditableFields {
  email: string;
  telephone: string;
  monthComplete: string;
  sellerSubmission: string;
  submissionClientNumber: string;
  submissionClientLocnCode: string;
}

const EMPTY_FIELDS: EditableFields = {
  email: '',
  telephone: '',
  monthComplete: '',
  sellerSubmission: '',
  submissionClientNumber: '',
  submissionClientLocnCode: '',
};

/** Seeds the editable fields from a freshly parsed submission (nulls → ''). */
const fieldsFromSubmission = (s: ParsedSubmission): EditableFields => ({
  email: s.email ?? '',
  telephone: s.telephone ?? '',
  monthComplete: s.monthComplete ?? '',
  sellerSubmission: s.sellerSubmission ?? '',
  submissionClientNumber: s.submissionClientNumber ?? '',
  submissionClientLocnCode: s.submissionClientLocnCode ?? '',
});

const EMPTY_TABLE_MESSAGE = 'No data available — upload an XML file to populate this table.';

/**
 * The top status banner, keyed by the submission's worst severity: green when
 * clean, amber when only warnings remain, red when any error is present.
 */
const STATUS_BANNER: Record<InvoiceSeverity, { kind: 'success' | 'warning' | 'error'; title: string; subtitle: string }> =
  {
    none: {
      kind: 'success',
      title: 'No issues found.',
      subtitle: 'This submission passed all validation checks.',
    },
    warning: {
      kind: 'warning',
      title: 'Warnings found.',
      subtitle: 'Review the warnings below. You can still submit.',
    },
    error: {
      kind: 'error',
      title: 'Errors found.',
      subtitle: 'Correct the highlighted errors before submitting.',
    },
  };

export function UploadSubmissionPage() {
  const navigate = useNavigate();

  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [fileName, setFileName] = useState<string | null>(null);
  const [status, setStatus] = useState<Status>('idle');
  const [submission, setSubmission] = useState<ParsedSubmission | null>(null);
  const [fields, setFields] = useState<EditableFields>(EMPTY_FIELDS);
  const [structuralErrors, setStructuralErrors] = useState<ValidationMessageResponse[]>([]);
  const [businessResult, setBusinessResult] = useState<SubmissionValidationResponse | null>(null);
  const [issues, setIssues] = useState<MappedIssues | null>(null);
  const [notificationVisible, setNotificationVisible] = useState(true);
  // Client-side (required/pattern) errors on the editable metadata fields, keyed
  // by EditableFields key. Drives the inline red highlight the same way the
  // report filters do; cleared per field as the user edits.
  const [clientFieldErrors, setClientFieldErrors] = useState<Record<string, string>>({});
  // Which invoice rows are expanded (open) in the Invoice Details table. Keyed by
  // the invoice row id (`inv-${index}`).
  const [expandedRowIds, setExpandedRowIds] = useState<Set<string>>(new Set());

  const resetResults = () => {
    setSubmission(null);
    setFields(EMPTY_FIELDS);
    setStructuralErrors([]);
    setBusinessResult(null);
    setIssues(null);
    setClientFieldErrors({});
    setNotificationVisible(true);
    setExpandedRowIds(new Set());
  };

  const handleClear = () => {
    setSelectedFile(null);
    setFileName(null);
    resetResults();
    setStatus('idle');
  };

  const applyBusinessResult = (result: SubmissionValidationResponse | SubmissionSubmitResponse) => {
    setBusinessResult({
      valid: result.valid,
      code: result.code,
      message: result.message,
      acceptedInvoices: result.acceptedInvoices,
      rejectedInvoices: result.rejectedInvoices,
      errors: result.errors,
    });
    setIssues(mapSubmissionIssues(result.errors));
  };

  const handleSubmit = async () => {
    if (!selectedFile) return;

    // Client-side field checks first: on failure, mark the offending fields and
    // stay on the page (red inline errors) without calling the API.
    const clientResult = validateSubmissionMetadata({
      submissionClientNumber: fields.submissionClientNumber,
      submissionClientLocnCode: fields.submissionClientLocnCode,
      monthComplete: fields.monthComplete,
      sellerSubmission: fields.sellerSubmission,
    });
    const { fieldErrors } = splitMessages(clientResult.messages, SUBMISSION_METADATA_KEY_TO_FIELD);
    setClientFieldErrors(fieldErrors);
    if (clientResult.hasErrors()) return;

    setStatus('submitting');
    try {
      const result = await submitSubmission(selectedFile, {
        submissionClientNumber: fields.submissionClientNumber,
        submissionClientLocnCode: fields.submissionClientLocnCode,
        monthComplete: fields.monthComplete,
        sellerSubmission: fields.sellerSubmission,
      });
      if (result.valid && result.submissionId != null) {
        navigate(`${ROUTES.SUBMISSION_HISTORY}/${result.submissionId}`);
        return;
      }
      // Rejected at submit time — surface the issues instead of saving.
      applyBusinessResult(result);
      setStatus('done');
    } catch (error) {
      const body = submissionErrorBody<SubmissionSubmitResponse>(error);
      if (body) {
        applyBusinessResult(body);
        setStatus('done');
      } else {
        setStatus('network-error');
      }
    }
  };

  const handleFileSelected = async (file: File) => {
    setSelectedFile(file);
    setFileName(file.name);
    resetResults();
    setStatus('parsing');

    // Phase 1 — structural validation + parse (populates the form).
    let parsed: SubmissionParseResponse;
    try {
      parsed = await parseSubmission(file);
    } catch (error) {
      const body = submissionErrorBody<SubmissionParseResponse>(error);
      if (body) {
        setStructuralErrors(body.errors);
        setStatus('structural-error');
      } else {
        setStatus('network-error');
      }
      return;
    }

    if (!parsed.valid || !parsed.submission) {
      setStructuralErrors(parsed.errors);
      setStatus('structural-error');
      return;
    }

    setSubmission(parsed.submission);
    setFields(fieldsFromSubmission(parsed.submission));
    // Expand every invoice by default so all line items are visible at a glance;
    // the user can collapse rows individually or via "Collapse all".
    setExpandedRowIds(new Set(parsed.submission.invoices.map((inv) => `inv-${inv.index}`)));
    setStatus('validating');

    // Phase 2 — business-rule validation (drives the inline errors).
    try {
      applyBusinessResult(await validateSubmissionBusiness(file));
      setStatus('done');
    } catch (error) {
      const body = submissionErrorBody<SubmissionValidationResponse>(error);
      if (body) {
        applyBusinessResult(body);
        setStatus('done');
      } else {
        setStatus('network-error');
      }
    }
  };

  const isBusy = status === 'parsing' || status === 'validating' || status === 'submitting';
  const hasSubmission = submission != null;
  // Any ERROR-severity ("hard") validation issue disables Submit — the user must
  // resolve it before the submission can be sent. Errors on the editable metadata
  // fields lift as the user corrects them (setField clears them); errors on the
  // read-only invoice/line data require re-uploading a corrected file. Warnings
  // do not block submission.
  const hasHardValidationErrors = hasHardErrors(issues);
  const canSubmit = hasSubmission && !isBusy && !hasHardValidationErrors;

  // Progress text shown by the inline loader for each in-flight phase.
  const loadingDescription = (): string => {
    if (status === 'parsing') return 'Parsing and validating schema…';
    if (status === 'submitting') return 'Submitting…';
    return 'Running business validation…';
  };

  // Metadata field issues (submission-level messages that map to a field).
  const fieldIssue = (key: string): { errorText?: string; warningText?: string } => {
    const list = issues?.submissionFields[key];
    if (!list?.length) return {};
    const errorText = list
      .filter((i) => i.type === 'ERROR')
      .map((i) => i.message)
      .join(' ');
    const warningText = list
      .filter((i) => i.type === 'WARNING')
      .map((i) => i.message)
      .join(' ');
    return { errorText: errorText || undefined, warningText: warningText || undefined };
  };

  const setField = (key: keyof EditableFields, value: string) => {
    setFields((prev) => ({ ...prev, [key]: value }));
    // Clear this field's stale errors as soon as the user edits it (both the
    // client-side check and any server issue routed to it), so the red highlight
    // lifts while they correct it — matching the report pages. The submit
    // endpoint re-validates and re-surfaces anything still wrong.
    setClientFieldErrors((prev) => (prev[key] ? { ...prev, [key]: '' } : prev));
    setIssues((prev) => {
      if (!prev?.submissionFields[key]) return prev;
      const submissionFields = { ...prev.submissionFields };
      delete submissionFields[key];
      return { ...prev, submissionFields };
    });
  };

  /**
   * Renders one editable metadata field. Autofilled from the parsed submission
   * and disabled until a file has parsed. A client-side (required/pattern) error
   * takes precedence and shows inline red; otherwise any submission-level
   * validation message mapped to `issueKey` is surfaced (error → warning).
   */
  const editableField = (id: string, label: string, key: keyof EditableFields, issueKey?: string) => {
    const serverIssue = issueKey ? fieldIssue(issueKey) : {};
    const errorText = clientFieldErrors[key] || serverIssue.errorText;
    const warningText = serverIssue.warningText;
    return (
      <div className="upload-submission-page__field">
        <TextInput
          id={id}
          labelText={label}
          value={fields[key]}
          onChange={(e) => setField(key, e.target.value)}
          disabled={isBusy}
          invalid={!!errorText}
          invalidText={errorText}
          warn={!errorText && !!warningText}
          warnText={warningText}
        />
      </div>
    );
  };

  const invoiceRows: InvoiceRow[] = (submission?.invoices ?? []).map((inv) => ({ ...inv, id: `inv-${inv.index}` }));
  const lineItemRows: LineItemRow[] = (submission?.lineItems ?? []).map((li) => ({
    ...li,
    id: `li-${li.invoiceIndex}-${li.lineIndex}`,
  }));

  // Line items grouped by their parent invoice index, so each expanded invoice
  // row renders only its own lines underneath it.
  const lineItemsByInvoice = new Map<number, LineItemRow[]>();
  for (const li of lineItemRows) {
    const group = lineItemsByInvoice.get(li.invoiceIndex) ?? [];
    group.push(li);
    lineItemsByInvoice.set(li.invoiceIndex, group);
  }

  const expandAllInvoices = () => setExpandedRowIds(new Set(invoiceRows.map((row) => row.id)));
  const collapseAllInvoices = () => setExpandedRowIds(new Set());

  // Line-item cell markers keyed by line-item row id. Invoice-level issues are
  // surfaced by the per-invoice badge + the local issue list instead of parent-
  // row cell markers, so only line-item issues are mapped here.
  const lineIssuesByRowId: Record<string, RowIssues> = {};
  if (issues) {
    for (const [key, rowIssues] of Object.entries(issues.lineItems)) {
      const [invoiceIndex, lineIndex] = key.split(':');
      lineIssuesByRowId[`li-${invoiceIndex}-${lineIndex}`] = rowIssues;
    }
  }

  const invoiceColumns: DataPreviewColumn<InvoiceRow>[] = [
    {
      key: 'invoiceNumber',
      header: 'Invoice #',
      // Show the per-invoice severity badge only once business validation has
      // run (businessResult set); before then a "No issues" badge would be
      // misleading.
      renderCell: (r) => (
        <span className="upload-submission-page__invoice-cell">
          <span>{r.invoiceNumber ?? '—'}</span>
          {businessResult ? <InvoiceIssueBadge issues={collectInvoiceIssues(issues, r.index)} /> : null}
        </span>
      ),
    },
    { key: 'invoiceDate', header: 'Date', renderCell: (r) => r.invoiceDate ?? '—' },
    { key: 'invoiceType', header: 'Type', renderCell: (r) => r.invoiceType ?? '—' },
    { key: 'sellerClientNumber', header: 'Seller Client #', renderCell: (r) => r.sellerClientNumber ?? '—' },
    { key: 'buyerClientNumber', header: 'Buyer Client #', renderCell: (r) => r.buyerClientNumber ?? '—' },
    { key: 'maturity', header: 'Maturity', renderCell: (r) => r.maturity ?? '—' },
    { key: 'locationFOB', header: 'FOB Location', renderCell: (r) => r.locationFOB ?? '—' },
    { key: 'totalAmount', header: 'Total Amount', align: 'right', renderCell: (r) => formatCurrency(r.totalAmount) },
    { key: 'totalVolume', header: 'Total Volume', align: 'right', renderCell: (r) => formatNumber(r.totalVolume, 3) },
    { key: 'totalPieces', header: 'Total Pieces', align: 'right', renderCell: (r) => formatNumber(r.totalPieces) },
  ];

  const renderNetworkError = () => (
    <InlineNotification
      kind="error"
      lowContrast
      title="Upload failed."
      subtitle="Could not reach the server. Please try again."
      onClose={() => setNotificationVisible(false)}
      className="upload-submission-page__notification"
    />
  );

  const renderStructuralError = () => {
    const issueRows = toStructuralIssueRows(structuralErrors);
    const issueColumns: DataPreviewColumn<StructuralIssueRow>[] = [
      { key: 'issue', header: 'Issue', renderCell: (r) => r.issue },
      {
        key: 'location',
        header: 'File location',
        renderCell: (r) => <span className="upload-submission-page__issue-location">{r.location || '—'}</span>,
      },
      { key: 'detail', header: 'Detail', renderCell: (r) => r.detail },
    ];
    const count = issueRows.length;
    return (
      <>
        <div className="upload-submission-page__notification">
          <InlineNotification
            kind="error"
            lowContrast
            hideCloseButton
            title={`${count} issue${count === 1 ? '' : 's'} found in submission`}
            subtitle="Correct the issues in your source file, then replace the file to continue."
          />
        </div>
        <section className="upload-submission-page__card">
          <div className="upload-submission-page__card-header">
            <h2 className="upload-submission-page__card-title upload-submission-page__card-title--flush">
              Validation issues ({count})
            </h2>
            <Button
              kind="ghost"
              size="sm"
              renderIcon={Download}
              onClick={() =>
                downloadBlob(
                  new Blob([structuralIssuesToCsv(issueRows)], { type: 'text/csv;charset=utf-8;' }),
                  'validation-issues.csv',
                )
              }
            >
              Download issues (.csv)
            </Button>
          </div>
          <div className="upload-submission-page__card-table upload-submission-page__issues-table">
            <DataPreviewTable rows={issueRows} columns={issueColumns} emptyMessage="No issues." />
          </div>
        </section>
      </>
    );
  };

  const renderBusinessResult = () => {
    // The top notification reflects the worst severity actually present across
    // the whole submission (errors → red, warnings-only → amber, clean → green),
    // rather than the response's accept/reject flags — so a submission that
    // parsed with only warnings still reads as a warning, not a pass. It also
    // carries the submission-level (form) messages that aren't tied to any
    // invoice; per-invoice and per-line issues are surfaced locally on each
    // invoice (a severity badge on the row and a list in its expanded panel).
    const banner = STATUS_BANNER[submissionSeverity(issues)];
    return (
      <div className="upload-submission-page__notification">
        <InlineNotification
          kind={banner.kind}
          lowContrast
          title={banner.title}
          subtitle={banner.subtitle}
          hideCloseButton
        />
        {issues?.formIssues.map((issue) => (
          <InlineNotification
            key={`form-${issue.type}-${issue.message}`}
            className="upload-submission-page__issue-banner"
            kind={issue.type === 'ERROR' ? 'error' : 'warning'}
            lowContrast
            hideCloseButton
            title={issue.message}
          />
        ))}
      </div>
    );
  };

  const renderIdle = () => (
    <InlineNotification
      kind="info"
      lowContrast
      title="No XML file uploaded."
      subtitle="Please upload a valid XML file to populate the submission form."
      onClose={() => setNotificationVisible(false)}
      className="upload-submission-page__notification"
    />
  );

  const renderStatus = () => {
    if (status === 'network-error') return renderNetworkError();
    if (status === 'structural-error') return renderStructuralError();
    if (status === 'done' && businessResult) return renderBusinessResult();
    if (status === 'idle' && notificationVisible) return renderIdle();
    return null;
  };

  const statusBlock = renderStatus();

  return (
    <div className="upload-submission-page">
      <Grid fullWidth>
        <PageTitle title="CSP Submission" subtitle="Submit your monthly CSP report in XML format." />

        {/* Upload card */}
        <Column lg={16} md={8} sm={4} className="upload-submission-page__section">
          <section className="upload-submission-page__card">
            <h2 className="upload-submission-page__card-title">Upload XML File</h2>
            <div className="upload-submission-page__card-body">
              <FileDropZone accept=".xml" onFileSelected={handleFileSelected} disabled={isBusy} />
              {(isBusy || fileName) && (
                <div className="upload-submission-page__upload-status">
                  {isBusy ? (
                    <InlineLoading description={loadingDescription()} />
                  ) : (
                    <span className="upload-submission-page__file-name">Selected file: {fileName}</span>
                  )}
                </div>
              )}
            </div>
          </section>
        </Column>

        {/* Status notification */}
        {statusBlock && (
          <Column lg={16} md={8} sm={4} className="upload-submission-page__section">
            {statusBlock}
          </Column>
        )}

        {/* Everything below only appears once a valid file has parsed. */}
        {hasSubmission && (
          <>
            {/* Submission Metadata */}
            <Column lg={16} md={8} sm={4} className="upload-submission-page__section">
              <section className="upload-submission-page__card">
                <h2 className="upload-submission-page__card-title">Submission Metadata</h2>
                <div className="upload-submission-page__field-grid">
                  {editableField('field-email', 'Email Address', 'email')}
                  {editableField('field-telephone', 'Telephone Number', 'telephone')}
                </div>
              </section>
            </Column>

            {/* Submission Details */}
            <Column lg={16} md={8} sm={4} className="upload-submission-page__section">
              <section className="upload-submission-page__card">
                <h2 className="upload-submission-page__card-title">Submission Details</h2>
                <div className="upload-submission-page__field-grid">
                  {editableField('field-month-complete', 'Month Complete', 'monthComplete', 'monthComplete')}
                  {editableField(
                    'field-seller-submission',
                    'Seller Submission',
                    'sellerSubmission',
                    'sellerSubmission',
                  )}
                </div>
              </section>
            </Column>

            {/* Submitter Information */}
            <Column lg={16} md={8} sm={4} className="upload-submission-page__section">
              <section className="upload-submission-page__card">
                <h2 className="upload-submission-page__card-title">Submitter Information</h2>
                <div className="upload-submission-page__field-grid">
                  {editableField(
                    'field-submission-client-number',
                    'Submission Client Number',
                    'submissionClientNumber',
                    'submissionClientNumber',
                  )}
                  {editableField(
                    'field-submission-client-locn-code',
                    'Submission Client Location Code',
                    'submissionClientLocnCode',
                    'submissionClientLocnCode',
                  )}
                </div>
              </section>
            </Column>

            {/* Invoice Details — each invoice expands to reveal its line items. */}
            <Column lg={16} md={8} sm={4} className="upload-submission-page__section">
              <section className="upload-submission-page__card">
                <div className="upload-submission-page__card-header">
                  <div>
                    <h2 className="upload-submission-page__card-title upload-submission-page__card-title--flush">
                      Invoice Details
                    </h2>
                    <p className="upload-submission-page__card-count">
                      {invoiceRows.length} invoice {invoiceRows.length === 1 ? 'entry' : 'entries'} ·{' '}
                      {lineItemRows.length} line item {lineItemRows.length === 1 ? 'entry' : 'entries'}
                    </p>
                    {invoiceRows.length > 0 && (
                      <div className="upload-submission-page__expand-actions">
                        <Link
                          href="#"
                          onClick={(e) => {
                            e.preventDefault();
                            expandAllInvoices();
                          }}
                        >
                          Expand all
                        </Link>
                        <span aria-hidden="true">|</span>
                        <Link
                          href="#"
                          onClick={(e) => {
                            e.preventDefault();
                            collapseAllInvoices();
                          }}
                        >
                          Collapse all
                        </Link>
                      </div>
                    )}
                  </div>
                  <div className="upload-submission-page__actions">
                    <Button kind="tertiary" size="md" onClick={handleClear} disabled={isBusy}>
                      Clear
                    </Button>
                    <Button kind="primary" size="md" onClick={handleSubmit} disabled={isBusy || !canSubmit}>
                      Submit
                    </Button>
                  </div>
                </div>
                <div className="upload-submission-page__card-table upload-submission-page__invoice-table">
                  <DataPreviewTable
                    rows={invoiceRows}
                    columns={invoiceColumns}
                    emptyMessage={EMPTY_TABLE_MESSAGE}
                    expandable
                    expandedRowIds={expandedRowIds}
                    onExpandedRowIdsChange={setExpandedRowIds}
                    rowClassName={(row) => {
                      // Accent the row by severity, but only once validation has run.
                      if (!businessResult) return undefined;
                      const severity = invoiceSeverity(collectInvoiceIssues(issues, row.index));
                      return severity === 'none' ? undefined : `upload-submission-page__invoice-row--${severity}`;
                    }}
                    renderExpandedContent={(row) => (
                      <InvoiceDetailPanel
                        invoice={row}
                        invoiceNumber={row.invoiceNumber ?? '—'}
                        lineItems={lineItemsByInvoice.get(row.index) ?? []}
                        issuesByRowId={lineIssuesByRowId}
                        issues={collectInvoiceIssues(issues, row.index)}
                        fieldIssues={issues?.invoices[row.index]?.fields ?? {}}
                      />
                    )}
                  />
                </div>
              </section>
            </Column>
          </>
        )}
      </Grid>
    </div>
  );
}
