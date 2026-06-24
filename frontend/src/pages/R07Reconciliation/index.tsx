import React from 'react';
import { Button, Checkbox, Column, Grid, InlineLoading, InlineNotification, TextInput } from '@carbon/react';

import './index.scss';
import { DocumentExport } from '@carbon/icons-react';

import ClientAutocomplete, { type ClientLocationResponse } from '@/components/Form/ClientAutocomplete';
import ClientNumberAutocomplete from '@/components/Form/ClientNumberAutocomplete';
import DateInput from '@/components/Form/DateInput';
import RequiredLabel from '@/components/Form/RequiredLabel';
import SingleSelect from '@/components/Form/SingleSelect';
import TaggedMultiSelect from '@/components/Form/TaggedMultiSelect';
import { useNotification } from '@/context/notification/useNotification';
import {
  type LookupItemResponse,
  useInvoiceStatusesQuery,
  useInvoiceTypesQuery,
  useMaturityCodesNoCantsQuery,
  useSubmissionStatusesQuery,
} from '@/services/lookup.service';
import { useR07ReportMutation } from '@/services/r07.service';
import {
  TIME_FRAME_ITEMS,
  formatDate,
  formatYearMonth,
  downloadBlob,
  itemToString,
  lookupItemToString,
  lookupDescriptionToString,
  isNoDataError,
} from '@/utils/report';
import { parseReportValidationError } from '@/services/reportValidation';
import { splitMessages } from '@/validations/validationResult';
import { validateR07 } from '@/validations/reports/r07';
import { MESSAGE_KEY_TO_FIELD } from './messageKeyMap';

export function R07ReconciliationPage() {
  const { mutate: generateReport, isPending } = useR07ReportMutation();
  const { addNotification } = useNotification();

  const { data: typeItems = [], isLoading: typeLoading } = useInvoiceTypesQuery();
  const { data: statusItems = [], isLoading: statusLoading } = useInvoiceStatusesQuery();
  const { data: maturityItems = [], isLoading: maturityLoading } = useMaturityCodesNoCantsQuery();
  const { data: submissionStatusItems = [], isLoading: submissionStatusLoading } = useSubmissionStatusesQuery();

  const [reportingYearMonth, setReportingYearMonth] = React.useState<Date | null>(null);
  const [dateFrom, setDateFrom] = React.useState<Date | null>(null);
  const [dateTo, setDateTo] = React.useState<Date | null>(null);
  const [timeFrame, setTimeFrame] = React.useState('');
  const [sellerClient, setSellerClient] = React.useState<ClientLocationResponse | null>(null);
  const [sellerNumber, setSellerNumber] = React.useState('');
  const [buyerClient, setBuyerClient] = React.useState<ClientLocationResponse | null>(null);
  const [buyerNumber, setBuyerNumber] = React.useState('');
  const [selectedInvoiceTypes, setSelectedInvoiceTypes] = React.useState<LookupItemResponse[]>([]);
  const [selectedInvoiceStatuses, setSelectedInvoiceStatuses] = React.useState<LookupItemResponse[]>([]);
  const [selectedMaturities, setSelectedMaturities] = React.useState<LookupItemResponse[]>([]);
  const [showReplacesAdjusts, setShowReplacesAdjusts] = React.useState(false);
  const [selectedSubmissionStatuses, setSelectedSubmissionStatuses] = React.useState<LookupItemResponse[]>([]);
  const [submissionNumber, setSubmissionNumber] = React.useState('');
  const [submissionYearMonth, setSubmissionYearMonth] = React.useState<Date | null>(null);
  const [fieldErrors, setFieldErrors] = React.useState<Record<string, string>>({});
  const [formErrors, setFormErrors] = React.useState<string[]>([]);
  const [warnings, setWarnings] = React.useState<string[]>([]);

  const handleSellerSelect = (client: ClientLocationResponse | null) => {
    setSellerClient(client);
    setSellerNumber(client?.clientNumber ?? '');
  };

  const handleBuyerSelect = (client: ClientLocationResponse | null) => {
    setBuyerClient(client);
    setBuyerNumber(client?.clientNumber ?? '');
  };

  const buildRequest = (reportFormat: 'PDF' | 'CSV') => ({
    reportFormat,
    ...(reportingYearMonth && {
      year: reportingYearMonth.getFullYear(),
      month: reportingYearMonth.getMonth() + 1,
    }),
    ...(dateFrom && { dateFrom: formatDate(dateFrom) }),
    ...(dateTo && { dateTo: formatDate(dateTo) }),
    ...(timeFrame && { timeFrame }),
    ...(sellerNumber.trim() && { sellerClientNumber: sellerNumber.trim() }),
    ...(sellerClient?.clientName && { sellerClientName: sellerClient.clientName }),
    ...(sellerClient?.clientLocnCode && { sellerLocCode: sellerClient.clientLocnCode }),
    ...(buyerNumber.trim() && { buyerClientNumber: buyerNumber.trim() }),
    ...(buyerClient?.clientName && { buyerClientName: buyerClient.clientName }),
    ...(buyerClient?.clientLocnCode && { buyerLocCode: buyerClient.clientLocnCode }),
    ...(selectedInvoiceTypes.length > 0 && { invoiceType: selectedInvoiceTypes.map((i) => i.code).join(',') }),
    ...(selectedInvoiceStatuses.length > 0 && { invoiceStatus: selectedInvoiceStatuses.map((i) => i.code).join(',') }),
    ...(selectedMaturities.length > 0 && { maturityCodes: selectedMaturities.map((m) => m.code).join(',') }),
    showReplacesAdjusts,
    ...(selectedSubmissionStatuses.length > 0 && {
      submissionStatus: selectedSubmissionStatuses.map((s) => s.code).join(','),
    }),
    ...(submissionNumber.trim() && { submissionNumber: submissionNumber.trim() }),
    ...(submissionYearMonth && { submissionYearMonth: formatYearMonth(submissionYearMonth) }),
  });

  const handleExport = (reportFormat: 'PDF' | 'CSV') => {
    const clientResult = validateR07({
      reportingYearMonth,
      dateFrom,
      dateTo,
      sellerNumber,
      buyerNumber,
      submissionNumber,
      timeFrame,
    });
    const clientSplit = splitMessages(clientResult.messages, MESSAGE_KEY_TO_FIELD);
    setFieldErrors(clientSplit.fieldErrors);
    setFormErrors(clientSplit.formErrors);
    setWarnings(clientSplit.warnings);
    if (clientResult.hasErrors()) return;

    generateReport(buildRequest(reportFormat), {
      onSuccess: ({ blob, filename }) => {
        downloadBlob(blob, filename);
      },
      onError: async (error) => {
        const messages = await parseReportValidationError(error);
        if (messages.length > 0) {
          const split = splitMessages(messages, MESSAGE_KEY_TO_FIELD);
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

  return (
    <div className="r07-page">
      <Grid>
        <Column lg={16} md={8} sm={4}>
          <h1 className="r07-page__title">R07 - Reconciliation report</h1>
        </Column>

        {(formErrors.length > 0 || warnings.length > 0) && (
          <Column lg={16} md={8} sm={4} className="r07-page__form-col">
            {formErrors.map((message, i) => (
              <InlineNotification
                key={`error-${i}`}
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
          </Column>
        )}

        <Column lg={16} md={8} sm={4}>
          <h2 className="r07-page__section-heading">Reporting month and report range information</h2>
        </Column>

        <Column lg={3} md={4} sm={4} className="r07-page__form-col r07-page__form-col--left">
          <DateInput
            id="reporting-year-month"
            labelText={<RequiredLabel>Year / month</RequiredLabel>}
            dateFormat="Y/m"
            onChange={(dates) => setReportingYearMonth(dates[0] ?? null)}
          />
        </Column>
        <Column lg={3} md={4} sm={4} className="r07-page__form-col">
          <SingleSelect
            size="lg"
            id="time-frame"
            titleText={<RequiredLabel>Time frame (in months)</RequiredLabel>}
            label=""
            items={TIME_FRAME_ITEMS}
            selectedItem={TIME_FRAME_ITEMS.find((i) => i.id === timeFrame) ?? null}
            itemToString={itemToString}
            onChange={({ selectedItem }) => setTimeFrame(selectedItem?.id ?? '')}
          />
        </Column>
        <Column lg={10} md={8} sm={4} className="r07-page__row-break" />

        <Column lg={3} md={4} sm={4} className="r07-page__form-col r07-page__form-col--left">
          <DateInput
            id="start-date"
            labelText={<RequiredLabel>Start date</RequiredLabel>}
            invalid={!!fieldErrors.startDate}
            invalidText={fieldErrors.startDate}
            onChange={(dates) => {
              setDateFrom(dates[0] ?? null);
              setFieldErrors((prev) => ({ ...prev, startDate: '' }));
            }}
          />
        </Column>
        <Column lg={3} md={4} sm={4} className="r07-page__form-col">
          <DateInput
            id="end-date"
            labelText={<RequiredLabel>End date</RequiredLabel>}
            invalid={!!fieldErrors.endDate}
            invalidText={fieldErrors.endDate}
            onChange={(dates) => {
              setDateTo(dates[0] ?? null);
              setFieldErrors((prev) => ({ ...prev, endDate: '' }));
            }}
          />
        </Column>
        <Column lg={10} md={8} sm={4} className="r07-page__row-break" />

        <Column lg={16} md={8} sm={4}>
          <h2 className="r07-page__section-heading r07-page__section-heading--spaced">Client information</h2>
        </Column>

        <Column lg={6} md={8} sm={4} className="r07-page__form-col">
          <ClientAutocomplete
            id="seller-client"
            titleText="Seller name"
            selectedClient={sellerClient}
            onSelect={handleSellerSelect}
          />
        </Column>
        <Column lg={10} md={8} sm={4} className="r07-page__row-break" />

        <Column lg={6} md={8} sm={4} className="r07-page__form-col">
          <ClientNumberAutocomplete
            id="seller-number"
            titleText="Seller number"
            selectedClient={sellerClient}
            onSelect={handleSellerSelect}
          />
        </Column>
        <Column lg={10} md={8} sm={4} className="r07-page__row-break" />

        <Column lg={6} md={8} sm={4} className="r07-page__form-col">
          <ClientAutocomplete
            id="buyer-client"
            titleText="Buyer name"
            selectedClient={buyerClient}
            onSelect={handleBuyerSelect}
          />
        </Column>
        <Column lg={10} md={8} sm={4} className="r07-page__row-break" />

        <Column lg={6} md={8} sm={4} className="r07-page__form-col">
          <ClientNumberAutocomplete
            id="buyer-number"
            titleText="Buyer number"
            selectedClient={buyerClient}
            onSelect={handleBuyerSelect}
          />
        </Column>
        <Column lg={10} md={8} sm={4} className="r07-page__row-break" />

        <Column lg={16} md={8} sm={4}>
          <h2 className="r07-page__section-heading r07-page__section-heading--spaced">Invoice information</h2>
        </Column>

        <Column lg={3} md={4} sm={4} className="r07-page__form-col r07-page__form-col--left">
          <TaggedMultiSelect
            size="lg"
            id="invoice-type"
            titleText="Invoice type"
            placeholder="Search and select..."
            items={typeItems}
            selectedItems={selectedInvoiceTypes}
            disabled={typeLoading}
            itemToString={lookupDescriptionToString}
            itemToKey={(i) => i.code}
            onChange={({ selectedItems }) => setSelectedInvoiceTypes(selectedItems ?? [])}
          />
        </Column>
        <Column lg={3} md={4} sm={4} className="r07-page__form-col">
          <TaggedMultiSelect
            size="lg"
            id="invoice-status"
            titleText="Invoice status"
            placeholder="Search and select..."
            items={statusItems}
            selectedItems={selectedInvoiceStatuses}
            disabled={statusLoading}
            itemToString={lookupDescriptionToString}
            itemToKey={(i) => i.code}
            onChange={({ selectedItems }) => setSelectedInvoiceStatuses(selectedItems ?? [])}
          />
        </Column>
        <Column lg={10} md={8} sm={4} className="r07-page__row-break" />

        <Column lg={3} md={4} sm={4} className="r07-page__form-col r07-page__form-col--left">
          <TaggedMultiSelect
            size="lg"
            id="maturity"
            titleText="Maturity"
            placeholder="Search and select..."
            items={maturityItems}
            selectedItems={selectedMaturities}
            disabled={maturityLoading}
            itemToString={lookupDescriptionToString}
            itemToKey={(m) => m.code}
            onChange={({ selectedItems }) => setSelectedMaturities(selectedItems ?? [])}
          />
        </Column>
        <Column lg={3} md={4} sm={4} className="r07-page__checkbox-col">
          <Checkbox
            id="replaces-adjust"
            labelText="Replaces / Adjust"
            checked={showReplacesAdjusts}
            onChange={(_e, { checked }) => setShowReplacesAdjusts(checked)}
          />
        </Column>
        <Column lg={10} md={8} sm={4} className="r07-page__row-break" />

        <Column lg={16} md={8} sm={4}>
          <h2 className="r07-page__section-heading r07-page__section-heading--spaced">Submission information</h2>
        </Column>

        <Column lg={3} md={4} sm={4} className="r07-page__form-col r07-page__form-col--left">
          <TaggedMultiSelect
            size="lg"
            id="submission-status"
            titleText={<RequiredLabel>Submission status</RequiredLabel>}
            placeholder="Search and select..."
            items={submissionStatusItems}
            selectedItems={selectedSubmissionStatuses}
            disabled={submissionStatusLoading}
            itemToString={lookupItemToString}
            itemToKey={(s) => s.code}
            onChange={({ selectedItems }) => setSelectedSubmissionStatuses(selectedItems ?? [])}
          />
        </Column>
        <Column lg={3} md={4} sm={4} className="r07-page__form-col">
          <TextInput
            id="submission-number"
            labelText={<RequiredLabel>Submission number</RequiredLabel>}
            value={submissionNumber}
            maxLength={10}
            invalid={!!fieldErrors.submissionNumber}
            invalidText={fieldErrors.submissionNumber}
            onChange={(e) => {
              setSubmissionNumber(e.target.value);
              setFieldErrors((prev) => ({ ...prev, submissionNumber: '' }));
            }}
          />
        </Column>
        <Column lg={10} md={8} sm={4} className="r07-page__row-break" />

        <Column lg={3} md={4} sm={4} className="r07-page__form-col r07-page__form-col--left">
          <DateInput
            id="submission-year-month"
            labelText={<RequiredLabel>Year / month</RequiredLabel>}
            dateFormat="Y/m"
            onChange={(dates) => setSubmissionYearMonth(dates[0] ?? null)}
          />
        </Column>
        <Column lg={13} md={8} sm={4} className="r07-page__row-break" />

        <Column lg={3} md={4} sm={2} className="r07-page__export-btn-col r07-page__export-btn-col--left">
          {isPending ? (
            <InlineLoading description="Generating..." />
          ) : (
            <Button
              kind="secondary"
              renderIcon={DocumentExport}
              onClick={() => handleExport('PDF')}
              disabled={isPending}
            >
              Generate PDF
            </Button>
          )}
        </Column>
        <Column lg={3} md={4} sm={2} className="r07-page__export-btn-col">
          {isPending ? null : (
            <Button kind="primary" renderIcon={DocumentExport} onClick={() => handleExport('CSV')} disabled={isPending}>
              Export CSV
            </Button>
          )}
        </Column>
      </Grid>
    </div>
  );
}
