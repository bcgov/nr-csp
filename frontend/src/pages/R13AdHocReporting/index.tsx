import { DocumentExport, Information } from '@carbon/icons-react';
import { Button, Checkbox, InlineLoading, InlineNotification, TextInput } from '@carbon/react';
import React from 'react';

import PageTitle from '@/components/core/PageTitle';
import ClientAutocomplete, { type ClientLocationResponse } from '@/components/Form/ClientAutocomplete';
import ClientNumberAutocomplete from '@/components/Form/ClientNumberAutocomplete';
import DateInput from '@/components/Form/DateInput';
import RequiredLabel from '@/components/Form/RequiredLabel';
import SelectAllMultiSelect from '@/components/Form/SelectAllMultiSelect';
import SingleSelect from '@/components/Form/SingleSelect';
import { useNotification } from '@/context/notification/useNotification';
import { ROUTES } from '@/routes/routePaths';
import {
  type LookupItemResponse,
  useInvoiceStatusesQuery,
  useInvoiceTypesQuery,
  useSpeciesLookupQuery,
  useSortCodesLookupQuery,
  useGradeLookupQuery,
  useSubmissionStatusesQuery,
} from '@/services/lookup.service';
import { parseReportValidationError } from '@/services/reportValidation';
import { splitMessages } from '@/validations/validationResult';
import { useR13ReportMutation, type R13ShowOptions } from '@/services/r13.service';
import { formatDisplayDateV2 as formatDisplayDate } from '@/utils/format';
import {
  type SelectItem,
  TIME_FRAME_ITEMS,
  formatDate,
  formatYearMonth,
  downloadBlob,
  itemToString,
  lookupDescriptionToString,
  isNoDataError,
} from '@/utils/report';
import { validateR13 } from '@/validations/reports/r13';

import './index.scss';
import { MESSAGE_KEY_TO_FIELD } from './messageKeyMap';

// ── Constants ──────────────────────────────────────────────────────────────────

const SUBMISSION_TYPE_ITEMS: SelectItem[] = [
  { id: 'Electronic', label: 'Electronic' },
  { id: 'Manual', label: 'Manual' },
];

const MATURITY_ITEMS: LookupItemResponse[] = [
  { code: 'O', description: 'Old Growth' },
  { code: 'S', description: 'Second Growth' },
  { code: 'M', description: 'Mixed Growth' },
  { code: 'C', description: 'Cants' },
];

// ── Range field state ──────────────────────────────────────────────────────────
// Each row supports an exact value filter OR a from/to range (mutually exclusive).
// Typing in Value clears From & To; typing in From or To clears Value.

interface RangeState {
  value: string;
  from: string;
  to: string;
}

const DEFAULT_RANGE: RangeState = { value: '', from: '', to: '' };

// ── Filter state — centralised in a reducer ────────────────────────────────────
// Putting all form fields in one place means handleClear is a single action
// and adding a new filter only requires updating FilterState + INITIAL_FILTERS.

interface FilterState {
  // Report information
  reportName: string;
  startDate: Date | null;
  endDate: Date | null;
  timeFrame: string;
  // Submission & approval
  selectedSubmissionStatuses: LookupItemResponse[];
  selectedSubmissionTypes: SelectItem[];
  invoiceSubmitter: string;
  approvalIdNumber: string;
  approvedBy: string;
  approvalMonthYear: Date | null;
  // Invoice information
  selectedInvoiceTypes: LookupItemResponse[];
  selectedInvoiceStatuses: LookupItemResponse[];
  invoiceNumber: RangeState;
  replacesAdjusts: RangeState;
  boomNumber: RangeState;
  timberMark: RangeState;
  weighSlip: RangeState;
  // Client information
  sellerClient: ClientLocationResponse | null;
  buyerClient: ClientLocationResponse | null;
  // Invoice detail
  selectedMaturities: LookupItemResponse[];
  selectedSpecies: LookupItemResponse[];
  selectedSortCodes: LookupItemResponse[];
  selectedGrades: LookupItemResponse[];
  // Show options
  showOptions: R13ShowOptions;
}

const INITIAL_FILTERS: FilterState = {
  reportName: '',
  startDate: null,
  endDate: null,
  timeFrame: '',
  selectedSubmissionStatuses: [],
  selectedSubmissionTypes: [],
  invoiceSubmitter: '',
  approvalIdNumber: '',
  approvedBy: '',
  approvalMonthYear: null,
  selectedInvoiceTypes: [],
  selectedInvoiceStatuses: [],
  invoiceNumber: DEFAULT_RANGE,
  replacesAdjusts: DEFAULT_RANGE,
  boomNumber: DEFAULT_RANGE,
  timberMark: DEFAULT_RANGE,
  weighSlip: DEFAULT_RANGE,
  sellerClient: null,
  buyerClient: null,
  selectedMaturities: [],
  selectedSpecies: [],
  selectedSortCodes: [],
  selectedGrades: [],
  showOptions: {},
};

type FilterAction = { type: 'RESET' } | { type: 'UPDATE'; patch: Partial<FilterState> };

function filterReducer(state: FilterState, action: FilterAction): FilterState {
  switch (action.type) {
    case 'RESET':
      return INITIAL_FILTERS;
    case 'UPDATE':
      return { ...state, ...action.patch };
  }
}

// ── Helpers ────────────────────────────────────────────────────────────────────

const gradeItemToString = (item: LookupItemResponse | null): string => (item ? `Grade ${item.code}` : '');

// ── FilterRow ──────────────────────────────────────────────────────────────────
// 3-column table row: label | filter input | Show on report checkbox

interface FilterRowProps {
  label: string;
  showKey: keyof R13ShowOptions;
  showValue: boolean;
  onShowChange: (key: keyof R13ShowOptions, value: boolean) => void;
  children: React.ReactNode;
}

const FilterRow = ({ label, showKey, showValue, onShowChange, children }: FilterRowProps) => (
  <tr>
    <td className="r13-page__filter-table--col-label">{label}</td>
    <td>{children}</td>
    <td className="r13-page__filter-table--col-show">
      <div className="r13-page__show-wrapper">
        <Checkbox
          id={`show-${showKey}`}
          labelText="Show on report"
          hideLabel
          checked={!!showValue}
          onChange={(_e, { checked }) => onShowChange(showKey, checked)}
        />
      </div>
    </td>
  </tr>
);

// ── RangeFilterRow ─────────────────────────────────────────────────────────────
// 5-column table row: label | Value (no header) | From | To | Show on report
//
// All three inputs are independent. Typical usage:
//   • Value only → sent as an exact-match range (From = To = Value)
//   • From and/or To only → open- or closed-ended range
//   • All three → Value is used as the exact-match; From / To are also sent

interface RangeFilterRowProps {
  label: string;
  state: RangeState;
  onStateChange: (s: RangeState) => void;
  showKey: keyof R13ShowOptions;
  showValue: boolean;
  onShowChange: (key: keyof R13ShowOptions, value: boolean) => void;
  fieldId: string;
}

const RangeFilterRow = ({
  label,
  state,
  onStateChange,
  showKey,
  showValue,
  onShowChange,
  fieldId,
}: RangeFilterRowProps) => (
  <tr>
    <td className="r13-page__filter-table--col-label">{label}</td>
    <td>
      <TextInput
        id={`${fieldId}-value`}
        labelText=""
        hideLabel
        size="md"
        value={state.value}
        onChange={(e) => onStateChange({ ...state, value: e.target.value })}
      />
    </td>
    <td>
      <TextInput
        id={`${fieldId}-from`}
        labelText=""
        hideLabel
        size="md"
        value={state.from}
        onChange={(e) => onStateChange({ ...state, from: e.target.value })}
      />
    </td>
    <td>
      <TextInput
        id={`${fieldId}-to`}
        labelText=""
        hideLabel
        size="md"
        value={state.to}
        onChange={(e) => onStateChange({ ...state, to: e.target.value })}
      />
    </td>
    <td className="r13-page__filter-table--col-show">
      <div className="r13-page__show-wrapper">
        <Checkbox
          id={`show-${showKey}`}
          labelText="Show on report"
          hideLabel
          checked={!!showValue}
          onChange={(_e, { checked }) => onShowChange(showKey, checked)}
        />
      </div>
    </td>
  </tr>
);

// ── Page component ─────────────────────────────────────────────────────────────

export function R13AdHocReportingPage() {
  const { mutate: generateReport, isPending } = useR13ReportMutation();
  const { addNotification } = useNotification();

  // Lookup data
  const { data: submissionStatusItems = [], isLoading: submissionStatusLoading } = useSubmissionStatusesQuery();
  const { data: invoiceStatusItems = [], isLoading: invoiceStatusLoading } = useInvoiceStatusesQuery();
  const { data: invoiceTypeItems = [], isLoading: invoiceTypeLoading } = useInvoiceTypesQuery();
  const { data: speciesItems = [], isLoading: speciesLoading } = useSpeciesLookupQuery();
  const { data: sortCodeItems = [], isLoading: sortCodeLoading } = useSortCodesLookupQuery();
  const { data: gradeItems = [], isLoading: gradeLoading } = useGradeLookupQuery();

  // ── All filter fields in one reducer ───────────────────────────────────
  const [filters, dispatch] = React.useReducer(filterReducer, INITIAL_FILTERS);
  /** Partial update — mirrors useState's setter signature for single fields. */
  const set = (patch: Partial<FilterState>) => dispatch({ type: 'UPDATE', patch });

  // ── Validation errors ───────────────────────────────────────────────────
  const [fieldErrors, setFieldErrors] = React.useState<Record<string, string>>({});
  const [formErrors, setFormErrors] = React.useState<string[]>([]);
  const [warnings, setWarnings] = React.useState<string[]>([]);
  const clearFieldError = (field: string) => setFieldErrors((prev) => ({ ...prev, [field]: '' }));

  // ── Date input remount key ──────────────────────────────────────────────
  // Incrementing this forces all DateInputs to remount (clears flatpickr).
  const [dateKey, setDateKey] = React.useState(0);

  // ── Calculated end date ─────────────────────────────────────────────────
  // When both startDate and timeFrame are set, the end date is derived:
  // timeFrame N = last day of the month that is (N-1) months after startDate's month.
  // e.g. start = March 15 + timeFrame '01' → March 31
  //      start = March 15 + timeFrame '02' → April 30
  const calculatedEndDate = React.useMemo((): Date | null => {
    if (!filters.startDate || !filters.timeFrame) return null;
    const months = parseInt(filters.timeFrame, 10);
    return new Date(Date.UTC(filters.startDate.getUTCFullYear(), filters.startDate.getUTCMonth() + months, 0));
  }, [filters.startDate, filters.timeFrame]);

  // The date used for validation and the request payload. When timeFrame is set
  // the calculated date takes precedence over any manually-entered end date.
  const effectiveEndDate = filters.timeFrame && filters.startDate ? calculatedEndDate : filters.endDate;

  // ── Show options helper ─────────────────────────────────────────────────
  const toggleShow = (key: keyof R13ShowOptions, value: boolean) =>
    set({ showOptions: { ...filters.showOptions, [key]: value } });

  // ── Validation ──────────────────────────────────────────────────────────
  const validate = (): boolean => {
    const result = validateR13(
      filters.reportName,
      filters.startDate,
      effectiveEndDate,
      filters.timeFrame,
      filters.showOptions,
    );
    const split = splitMessages(result.messages, MESSAGE_KEY_TO_FIELD);
    setFieldErrors(split.fieldErrors);
    setFormErrors(split.formErrors);
    setWarnings(split.warnings);
    return !result.hasErrors();
  };

  // ── Range field request params ──────────────────────────────────────────
  // Exact value → send as both From and To (= exact-match range).
  const rangeToParams = (fromKey: string, toKey: string, rs: RangeState): Record<string, string> => {
    if (rs.value.trim()) return { [fromKey]: rs.value.trim(), [toKey]: rs.value.trim() };
    return {
      ...(rs.from.trim() && { [fromKey]: rs.from.trim() }),
      ...(rs.to.trim() && { [toKey]: rs.to.trim() }),
    };
  };

  // ── Build request ───────────────────────────────────────────────────────
  const buildRequest = (reportFormat: 'PDF' | 'CSV') => ({
    reportName: filters.reportName.trim(),
    reportFormat,
    ...(filters.startDate && { invoiceDateFrom: formatDate(filters.startDate) }),
    ...(effectiveEndDate && { invoiceDateTo: formatDate(effectiveEndDate) }),
    ...(filters.timeFrame && { timeFrame: filters.timeFrame }),
    ...(filters.selectedSubmissionStatuses.length > 0 && {
      submissionStatus: filters.selectedSubmissionStatuses.map((s) => s.code),
    }),
    ...(filters.selectedSubmissionTypes.length > 0 && {
      submissionTypes: filters.selectedSubmissionTypes.map((t) => t.id),
    }),
    ...(filters.invoiceSubmitter.trim() && { entryUserId: filters.invoiceSubmitter.trim() }),
    ...(filters.approvalIdNumber.trim() && { submissionNumber: filters.approvalIdNumber.trim() }),
    ...(filters.approvedBy.trim() && { approvedBy: [filters.approvedBy.trim()] }),
    ...(filters.approvalMonthYear && { approvalMonthYear: formatYearMonth(filters.approvalMonthYear) }),
    ...(filters.selectedInvoiceTypes.length > 0 && { invoiceTypes: filters.selectedInvoiceTypes.map((t) => t.code) }),
    ...(filters.selectedInvoiceStatuses.length > 0 && {
      invoiceStatus: filters.selectedInvoiceStatuses.map((s) => s.code),
    }),
    ...rangeToParams('invoiceNumberFrom', 'invoiceNumberTo', filters.invoiceNumber),
    ...rangeToParams('invoiceReplacesAdjustsFrom', 'invoiceReplacesAdjustsTo', filters.replacesAdjusts),
    ...rangeToParams('invoiceBoomNumberFrom', 'invoiceBoomNumberTo', filters.boomNumber),
    ...rangeToParams('invoiceTimberMarkFrom', 'invoiceTimberMarkTo', filters.timberMark),
    ...rangeToParams('invoiceWeighSlipFrom', 'invoiceWeighSlipTo', filters.weighSlip),
    ...(filters.sellerClient?.clientName && { sellerName: filters.sellerClient.clientName }),
    ...(filters.sellerClient?.clientNumber && { sellerNumbers: [filters.sellerClient.clientNumber] }),
    ...(filters.buyerClient?.clientName && { buyerName: filters.buyerClient.clientName }),
    ...(filters.buyerClient?.clientNumber && { buyerNumbers: [filters.buyerClient.clientNumber] }),
    ...(filters.selectedMaturities.length > 0 && { maturityCodes: filters.selectedMaturities.map((m) => m.code) }),
    ...(filters.selectedSpecies.length > 0 && { species: filters.selectedSpecies.map((s) => s.code) }),
    ...(filters.selectedSortCodes.length > 0 && { sortCodes: filters.selectedSortCodes.map((s) => s.code) }),
    ...(filters.selectedGrades.length > 0 && { grades: filters.selectedGrades.map((g) => g.code) }),
    showOptions: filters.showOptions,
  });

  // ── Export handler ──────────────────────────────────────────────────────
  const handleExport = (reportFormat: 'PDF' | 'CSV') => {
    if (!validate()) return;
    generateReport(buildRequest(reportFormat), {
      onSuccess: ({ blob, filename }) => {
        downloadBlob(blob, filename);
      },
      onError: async (error) => {
        const serverMessages = await parseReportValidationError(error);
        if (serverMessages.length > 0) {
          const split = splitMessages(serverMessages, MESSAGE_KEY_TO_FIELD);
          setFieldErrors(split.fieldErrors);
          setFormErrors(split.formErrors);
          setWarnings(split.warnings);
          return;
        }
        addNotification(
          isNoDataError(error)
            ? { kind: 'warning', title: 'No data found. No records matched the selected criteria.' }
            : { kind: 'error', title: 'Report generation failed.' },
        );
      },
    });
  };

  // ── Clear all ───────────────────────────────────────────────────────────
  const handleClear = () => {
    dispatch({ type: 'RESET' });
    setFieldErrors({});
    setFormErrors([]);
    setWarnings([]);
    setDateKey((prev) => prev + 1);
  };

  // ── Render ──────────────────────────────────────────────────────────────
  return (
    <div className="r13-page">
      {/* ── Breadcrumb + title ── */}
      <PageTitle
        title="R13 — Ad hoc reporting"
        breadCrumbs={[
          { name: 'Reports', path: ROUTES.R06_INVOICE_PRINT_OUT },
          { name: 'R13 — Ad hoc', path: ROUTES.R13_AD_HOC },
        ]}
      />

      {/* ── Description + info banner ── */}
      <p className="r13-page__description">
        Build a custom invoice report. Enter a value in the <strong>Filter</strong> column to narrow records, and tick{' '}
        <strong>Show on report</strong> to include that field as a column in the output.
      </p>
      <div className="r13-page__info-banner">
        <Information size={16} />
        Filter and show are independent — you can filter without showing, or show without filtering.
      </div>

      {/* ── Form-level validation errors / warnings ── */}
      {formErrors.map((message, i) => (
        <InlineNotification
          key={`form-error-${i}`}
          kind="error"
          lowContrast
          title="Validation error"
          subtitle={message}
          hideCloseButton
        />
      ))}
      {warnings.map((message, i) => (
        <InlineNotification
          key={`warning-${i}`}
          kind="warning"
          lowContrast
          title="Warning"
          subtitle={message}
          hideCloseButton
        />
      ))}

      {/* ── Report information ── */}
      <div className="r13-page__report-info">
        <h2>Report information</h2>
        <p>Name your report and set the reporting period. Start date and either end date or time frame are required.</p>
      </div>

      <div className="r13-page__field-row">
        <div className="r13-page__field--wide">
          <TextInput
            id="report-name"
            size="md"
            labelText={<RequiredLabel>Report name</RequiredLabel>}
            placeholder="e.g. Q1 2026 cedar sales — buyer Smith"
            value={filters.reportName}
            invalid={!!fieldErrors.reportName}
            invalidText={fieldErrors.reportName}
            onChange={(e) => {
              set({ reportName: e.target.value });
              clearFieldError('reportName');
            }}
          />
        </div>
      </div>

      <div className="r13-page__field-row">
        <div className="r13-page__field--narrow">
          <DateInput
            key={`start-date-${dateKey}`}
            id="start-date"
            labelText={<RequiredLabel>Start date</RequiredLabel>}
            invalid={!!fieldErrors.startDate}
            invalidText={fieldErrors.startDate}
            onChange={(dates) => {
              set({ startDate: dates[0] ?? null });
              clearFieldError('startDate');
            }}
          />
        </div>

        {/* End date — shows calculated date when time frame is set, otherwise a DateInput */}
        <div className="r13-page__field--narrow">
          {filters.timeFrame ? (
            <TextInput
              id="end-date-calculated"
              size="md"
              labelText={<RequiredLabel>End date (from time frame)</RequiredLabel>}
              value={calculatedEndDate ? formatDisplayDate(calculatedEndDate) : ''}
              readOnly
              invalid={!!fieldErrors.endDate}
              invalidText={fieldErrors.endDate}
            />
          ) : (
            <DateInput
              key={`end-date-${dateKey}`}
              id="end-date"
              labelText={<RequiredLabel>End date</RequiredLabel>}
              invalid={!!fieldErrors.endDate}
              invalidText={fieldErrors.endDate}
              onChange={(dates) => {
                set({ endDate: dates[0] ?? null });
                clearFieldError('endDate');
              }}
            />
          )}
        </div>

        <div className="r13-page__field--narrow">
          <SingleSelect
            size="md"
            id="time-frame"
            titleText={<RequiredLabel>Time frame (months)</RequiredLabel>}
            label="Select..."
            items={TIME_FRAME_ITEMS}
            selectedItem={TIME_FRAME_ITEMS.find((i) => i.id === filters.timeFrame) ?? null}
            itemToString={itemToString}
            invalid={!!fieldErrors.timeFrame}
            invalidText={fieldErrors.timeFrame}
            onChange={({ selectedItem }) => {
              set({ timeFrame: selectedItem?.id ?? '' });
              clearFieldError('timeFrame');
              clearFieldError('endDate');
            }}
          />
        </div>
      </div>

      {/* ── Submission & approval ── */}
      <div className="r13-page__section">
        <h2>Submission &amp; approval</h2>
        <p className="r13-page__section-hint">
          Filter by where an invoice sits in the workflow, who submitted it, or who approved it.
        </p>
        <table className="r13-page__filter-table">
          <thead>
            <tr>
              <th className="r13-page__filter-table--col-label">Field</th>
              <th>Filter</th>
              <th className="r13-page__filter-table--col-show">Show on report</th>
            </tr>
          </thead>
          <tbody>
            <FilterRow
              label="Submission status"
              showKey="showSubmissionStatus"
              showValue={!!filters.showOptions.showSubmissionStatus}
              onShowChange={toggleShow}
            >
              <SelectAllMultiSelect
                id="submission-status"
                titleText=""
                hideLabel
                size="md"
                placeholder="Search and select..."
                items={submissionStatusItems}
                selectedItems={filters.selectedSubmissionStatuses}
                disabled={submissionStatusLoading}
                itemToString={lookupDescriptionToString}
                itemToKey={(s) => s.code}
                onChange={({ selectedItems }) => set({ selectedSubmissionStatuses: selectedItems })}
              />
            </FilterRow>

            <FilterRow
              label="Submission type"
              showKey="showSubmissionType"
              showValue={!!filters.showOptions.showSubmissionType}
              onShowChange={toggleShow}
            >
              <SelectAllMultiSelect
                id="submission-type"
                titleText=""
                hideLabel
                size="md"
                placeholder="Search and select..."
                items={SUBMISSION_TYPE_ITEMS}
                selectedItems={filters.selectedSubmissionTypes}
                itemToString={itemToString}
                itemToKey={(t) => t.id}
                onChange={({ selectedItems }) => set({ selectedSubmissionTypes: selectedItems })}
              />
            </FilterRow>

            <FilterRow
              label="Invoice submitter"
              showKey="showEntryUserid"
              showValue={!!filters.showOptions.showEntryUserid}
              onShowChange={toggleShow}
            >
              <TextInput
                id="invoice-submitter"
                labelText=""
                hideLabel
                size="md"
                placeholder="Name or ID"
                value={filters.invoiceSubmitter}
                onChange={(e) => set({ invoiceSubmitter: e.target.value })}
              />
            </FilterRow>

            <FilterRow
              label="Approval ID number"
              showKey="showSubmissionNumber"
              showValue={!!filters.showOptions.showSubmissionNumber}
              onShowChange={toggleShow}
            >
              <TextInput
                id="approval-id-number"
                labelText=""
                hideLabel
                size="md"
                value={filters.approvalIdNumber}
                onChange={(e) => set({ approvalIdNumber: e.target.value })}
              />
            </FilterRow>

            <FilterRow
              label="Approved by"
              showKey="showApprovedBy"
              showValue={!!filters.showOptions.showApprovedBy}
              onShowChange={toggleShow}
            >
              <TextInput
                id="approved-by"
                labelText=""
                hideLabel
                size="md"
                placeholder="Approver name"
                value={filters.approvedBy}
                onChange={(e) => set({ approvedBy: e.target.value })}
              />
            </FilterRow>

            <FilterRow
              label="Approval year / month"
              showKey="showApprovalMonthYear"
              showValue={!!filters.showOptions.showApprovalMonthYear}
              onShowChange={toggleShow}
            >
              <DateInput
                key={`approval-month-year-${dateKey}`}
                id="approval-month-year"
                labelText=""
                hideLabel
                dateFormat="Y-m"
                onChange={(dates) => set({ approvalMonthYear: dates[0] ?? null })}
              />
            </FilterRow>
          </tbody>
        </table>
      </div>

      {/* ── Invoice information ── */}
      <div className="r13-page__section">
        <h2>Invoice information</h2>
        <p className="r13-page__section-hint">
          Filter invoices by type or status using the dropdowns, or narrow by numeric identifier using an exact value or
          a from/to range. For ranges, leave From or To empty for an open-ended range.
        </p>

        {/* Dropdown rows */}
        <table className="r13-page__filter-table">
          <thead>
            <tr>
              <th className="r13-page__filter-table--col-label">Field</th>
              <th>Filter</th>
              <th className="r13-page__filter-table--col-show">Show on report</th>
            </tr>
          </thead>
          <tbody>
            <FilterRow
              label="Invoice type"
              showKey="showInvoiceType"
              showValue={!!filters.showOptions.showInvoiceType}
              onShowChange={toggleShow}
            >
              <SelectAllMultiSelect
                id="invoice-type"
                titleText=""
                hideLabel
                size="md"
                placeholder="Search and select..."
                items={invoiceTypeItems}
                selectedItems={filters.selectedInvoiceTypes}
                disabled={invoiceTypeLoading}
                itemToString={lookupDescriptionToString}
                itemToKey={(t) => t.code}
                onChange={({ selectedItems }) => set({ selectedInvoiceTypes: selectedItems })}
              />
            </FilterRow>

            <FilterRow
              label="Invoice status"
              showKey="showInvoiceStatus"
              showValue={!!filters.showOptions.showInvoiceStatus}
              onShowChange={toggleShow}
            >
              <SelectAllMultiSelect
                id="invoice-status"
                titleText=""
                hideLabel
                size="md"
                placeholder="Search and select..."
                items={invoiceStatusItems}
                selectedItems={filters.selectedInvoiceStatuses}
                disabled={invoiceStatusLoading}
                itemToString={lookupDescriptionToString}
                itemToKey={(s) => s.code}
                onChange={({ selectedItems }) => set({ selectedInvoiceStatuses: selectedItems })}
              />
            </FilterRow>
          </tbody>
        </table>

        {/* Range rows — blank second header keeps the Value column unlabelled */}
        <table className="r13-page__filter-table" style={{ marginTop: '1px' }}>
          <thead>
            <tr>
              <th className="r13-page__filter-table--col-label">Field</th>
              <th>{/* Value — no header label */}</th>
              <th>From</th>
              <th>To</th>
              <th className="r13-page__filter-table--col-show">Show on report</th>
            </tr>
          </thead>
          <tbody>
            <RangeFilterRow
              label="Invoice number"
              fieldId="invoice-number"
              state={filters.invoiceNumber}
              onStateChange={(s) => set({ invoiceNumber: s })}
              showKey="showInvoiceNumber"
              showValue={!!filters.showOptions.showInvoiceNumber}
              onShowChange={toggleShow}
            />
            <RangeFilterRow
              label="Replaces / adjusts"
              fieldId="replaces-adjusts"
              state={filters.replacesAdjusts}
              onStateChange={(s) => set({ replacesAdjusts: s })}
              showKey="showInvoiceReplacesAdjusts"
              showValue={!!filters.showOptions.showInvoiceReplacesAdjusts}
              onShowChange={toggleShow}
            />
            <RangeFilterRow
              label="Boom number"
              fieldId="boom-number"
              state={filters.boomNumber}
              onStateChange={(s) => set({ boomNumber: s })}
              showKey="showInvoiceBoomNumber"
              showValue={!!filters.showOptions.showInvoiceBoomNumber}
              onShowChange={toggleShow}
            />
            <RangeFilterRow
              label="Timber mark"
              fieldId="timber-mark"
              state={filters.timberMark}
              onStateChange={(s) => set({ timberMark: s })}
              showKey="showInvoiceTimberMark"
              showValue={!!filters.showOptions.showInvoiceTimberMark}
              onShowChange={toggleShow}
            />
            <RangeFilterRow
              label="Weigh slip"
              fieldId="weigh-slip"
              state={filters.weighSlip}
              onStateChange={(s) => set({ weighSlip: s })}
              showKey="showInvoiceWeighSlip"
              showValue={!!filters.showOptions.showInvoiceWeighSlip}
              onShowChange={toggleShow}
            />
          </tbody>
        </table>
      </div>

      {/* ── Client information ── */}
      <div className="r13-page__section">
        <h2>Client information</h2>
        <p className="r13-page__section-hint">Filter by seller, buyer, or both.</p>
        <table className="r13-page__filter-table">
          <thead>
            <tr>
              <th className="r13-page__filter-table--col-label">Field</th>
              <th>Filter</th>
              <th className="r13-page__filter-table--col-show">Show on report</th>
            </tr>
          </thead>
          <tbody>
            <FilterRow
              label="Seller name"
              showKey="showSellerName"
              showValue={!!filters.showOptions.showSellerName}
              onShowChange={toggleShow}
            >
              <ClientAutocomplete
                id="seller-name"
                titleText=""
                hideLabel
                size="md"
                selectedClient={filters.sellerClient}
                onSelect={(c) => set({ sellerClient: c })}
              />
            </FilterRow>

            <FilterRow
              label="Seller number"
              showKey="showSellerNumber"
              showValue={!!filters.showOptions.showSellerNumber}
              onShowChange={toggleShow}
            >
              <ClientNumberAutocomplete
                id="seller-number"
                titleText=""
                hideLabel
                size="md"
                selectedClient={filters.sellerClient}
                onSelect={(c) => set({ sellerClient: c })}
              />
            </FilterRow>

            <FilterRow
              label="Buyer name"
              showKey="showBuyerName"
              showValue={!!filters.showOptions.showBuyerName}
              onShowChange={toggleShow}
            >
              <ClientAutocomplete
                id="buyer-name"
                titleText=""
                hideLabel
                size="md"
                selectedClient={filters.buyerClient}
                onSelect={(c) => set({ buyerClient: c })}
              />
            </FilterRow>

            <FilterRow
              label="Buyer number"
              showKey="showBuyerNumber"
              showValue={!!filters.showOptions.showBuyerNumber}
              onShowChange={toggleShow}
            >
              <ClientNumberAutocomplete
                id="buyer-number"
                titleText=""
                hideLabel
                size="md"
                selectedClient={filters.buyerClient}
                onSelect={(c) => set({ buyerClient: c })}
              />
            </FilterRow>
          </tbody>
        </table>
      </div>

      {/* ── Invoice detail ── */}
      <div className="r13-page__section">
        <h2>Invoice detail</h2>
        <p className="r13-page__section-hint">
          Restrict results to specific maturities, species, sort codes, or grades. Leave a filter empty to include all
          values.
        </p>
        <table className="r13-page__filter-table">
          <thead>
            <tr>
              <th className="r13-page__filter-table--col-label">Field</th>
              <th>Filter</th>
              <th className="r13-page__filter-table--col-show">Show on report</th>
            </tr>
          </thead>
          <tbody>
            <FilterRow
              label="Maturity"
              showKey="showMaturity"
              showValue={!!filters.showOptions.showMaturity}
              onShowChange={toggleShow}
            >
              <SelectAllMultiSelect
                id="maturity"
                titleText=""
                hideLabel
                size="md"
                placeholder="Search and select..."
                items={MATURITY_ITEMS}
                selectedItems={filters.selectedMaturities}
                itemToString={lookupDescriptionToString}
                itemToKey={(m) => m.code}
                onChange={({ selectedItems }) => set({ selectedMaturities: selectedItems })}
              />
            </FilterRow>

            <FilterRow
              label="Species"
              showKey="showSpecies"
              showValue={!!filters.showOptions.showSpecies}
              onShowChange={toggleShow}
            >
              <SelectAllMultiSelect
                id="species"
                titleText=""
                hideLabel
                size="md"
                placeholder="Search and select..."
                items={speciesItems}
                selectedItems={filters.selectedSpecies}
                disabled={speciesLoading}
                itemToString={lookupDescriptionToString}
                itemToKey={(s) => s.code}
                onChange={({ selectedItems }) => set({ selectedSpecies: selectedItems })}
              />
            </FilterRow>

            <FilterRow
              label="Secondary sort code"
              showKey="showSortCodeSecondary"
              showValue={!!filters.showOptions.showSortCodeSecondary}
              onShowChange={toggleShow}
            >
              <SelectAllMultiSelect
                id="secondary-sort-code"
                titleText=""
                hideLabel
                size="md"
                placeholder="Search and select..."
                items={sortCodeItems}
                selectedItems={filters.selectedSortCodes}
                disabled={sortCodeLoading}
                itemToString={lookupDescriptionToString}
                itemToKey={(s) => s.code}
                onChange={({ selectedItems }) => set({ selectedSortCodes: selectedItems })}
              />
            </FilterRow>

            <FilterRow
              label="Grade"
              showKey="showGrade"
              showValue={!!filters.showOptions.showGrade}
              onShowChange={toggleShow}
            >
              <SelectAllMultiSelect
                id="grade"
                titleText=""
                hideLabel
                size="md"
                placeholder="Search and select..."
                items={gradeItems}
                selectedItems={filters.selectedGrades}
                disabled={gradeLoading}
                itemToString={gradeItemToString}
                itemToKey={(g) => g.code}
                onChange={({ selectedItems }) => set({ selectedGrades: selectedItems })}
              />
            </FilterRow>
          </tbody>
        </table>
      </div>

      {/* ── Additional columns ── */}
      <div className="r13-page__additional-columns">
        <h2>Additional columns</h2>
        <p>Extra fields that aren&apos;t filtered above but can be added to the exported report.</p>

        <div className="r13-page__col-card">
          {/* MEASURES */}
          <div className="r13-page__col-group">
            <span className="r13-page__col-group-label">Measures</span>
            <Checkbox
              id="show-fob"
              labelText="FOB"
              checked={!!filters.showOptions.showFobPoint}
              onChange={(_e, { checked }) => toggleShow('showFobPoint', checked)}
            />
            <Checkbox
              id="show-pieces"
              labelText="# Pieces"
              checked={!!filters.showOptions.showPieces}
              onChange={(_e, { checked }) => toggleShow('showPieces', checked)}
            />
            <Checkbox
              id="show-volume"
              labelText={
                <>
                  Volume (m<sup>3</sup>)
                </>
              }
              checked={!!filters.showOptions.showVolume}
              onChange={(_e, { checked }) => toggleShow('showVolume', checked)}
            />
            <Checkbox
              id="show-amount"
              labelText="Amount"
              checked={!!filters.showOptions.showAmount}
              onChange={(_e, { checked }) => toggleShow('showAmount', checked)}
            />
          </div>

          {/* PRICES */}
          <div className="r13-page__col-group">
            <span className="r13-page__col-group-label">Prices</span>
            <Checkbox
              id="show-flat-price"
              labelText="Original flat"
              checked={!!filters.showOptions.showFlatPrice}
              onChange={(_e, { checked }) => toggleShow('showFlatPrice', checked)}
            />
            <Checkbox
              id="show-spread-price"
              labelText="Original spread"
              checked={!!filters.showOptions.showSpreadPrice}
              onChange={(_e, { checked }) => toggleShow('showSpreadPrice', checked)}
            />
            <Checkbox
              id="show-price"
              labelText="Relative"
              checked={!!filters.showOptions.showPrice}
              onChange={(_e, { checked }) => toggleShow('showPrice', checked)}
            />
          </div>

          {/* IDENTIFIERS */}
          <div className="r13-page__col-group">
            <span className="r13-page__col-group-label">Identifiers</span>
            <Checkbox
              id="show-sort-code-primary"
              labelText="Client — primary sort code"
              checked={!!filters.showOptions.showSortCodePrimary}
              onChange={(_e, { checked }) => toggleShow('showSortCodePrimary', checked)}
            />
          </div>

          {/* COMMENTS */}
          <div className="r13-page__col-group">
            <span className="r13-page__col-group-label">Comments</span>
            <Checkbox
              id="show-reviewer"
              labelText="Reviewer"
              checked={!!filters.showOptions.showReviewer}
              onChange={(_e, { checked }) => toggleShow('showReviewer', checked)}
            />
            <Checkbox
              id="show-comments"
              labelText="Submitted"
              checked={!!filters.showOptions.showComments}
              onChange={(_e, { checked }) => toggleShow('showComments', checked)}
            />
          </div>
        </div>

        {fieldErrors.showOptions && <p className="r13-page__show-error">{fieldErrors.showOptions}</p>}
      </div>

      {/* ── Action buttons ── */}
      <div className="r13-page__actions">
        {isPending ? (
          <InlineLoading description="Generating..." />
        ) : (
          <Button kind="secondary" renderIcon={DocumentExport} onClick={() => handleExport('PDF')}>
            Generate PDF
          </Button>
        )}
        {!isPending && (
          <Button kind="primary" renderIcon={DocumentExport} onClick={() => handleExport('CSV')}>
            Export CSV
          </Button>
        )}
        {!isPending && (
          <Button kind="ghost" onClick={handleClear}>
            Clear all
          </Button>
        )}
      </div>
    </div>
  );
}
