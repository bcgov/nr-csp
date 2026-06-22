import React, { useState } from 'react';
import { Button, Column, Grid, InlineLoading, InlineNotification, TextInput } from '@carbon/react';
import { Add, DocumentExport } from '@carbon/icons-react';

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
  useMaturityCodesWithCantsQuery,
} from '@/services/lookup.service';

import { useR06ReportMutation } from '@/services/r06.service';
import { formatDate, downloadBlob, lookupDescriptionToString, isNoDataError } from '@/utils/report';
import { parseReportValidationError } from '@/services/reportValidation';
import { splitMessages } from '@/validations/validationResult';
import { validateR06 } from '@/validations/reports/r06';
import { MESSAGE_KEY_TO_FIELD } from './messageKeyMap';

import './index.scss';

type InvoiceRange = { id: string; from: string; to: string };

function buildInvoiceList(ranges: InvoiceRange[]): string {
  const tokens: string[] = [];
  for (const r of ranges) {
    const from = r.from.trim();
    const to = r.to.trim();
    if (!from && !to) continue;
    if (!from) tokens.push(to, to);
    else if (!to) tokens.push(from, from);
    else tokens.push(from, to);
  }
  return tokens.join(',');
}

export function R06InvoicePrintOutPage() {
  const { mutate: generateReport, isPending } = useR06ReportMutation();
  const { addNotification } = useNotification();

  const { data: maturityItems = [], isLoading: maturityLoading } = useMaturityCodesWithCantsQuery();
  const { data: statusItems = [], isLoading: statusLoading } = useInvoiceStatusesQuery();
  const { data: typeItems = [], isLoading: typeLoading } = useInvoiceTypesQuery();

  const [dateFrom, setDateFrom] = useState<Date | null>(null);
  const [dateTo, setDateTo] = useState<Date | null>(null);
  const [selectedMaturities, setSelectedMaturities] = useState<LookupItemResponse[]>([]);
  const [sellerClient, setSellerClient] = useState<ClientLocationResponse | null>(null);
  const [buyerClient, setBuyerClient] = useState<ClientLocationResponse | null>(null);
  const [sellerNumber, setSellerNumber] = useState('');
  const [buyerNumber, setBuyerNumber] = useState('');
  const [sellerTypedName, setSellerTypedName] = useState('');
  const [buyerTypedName, setBuyerTypedName] = useState('');
  const [submissionId, setSubmissionId] = useState('');
  const [selectedInvoiceStatus, setSelectedInvoiceStatus] = useState<LookupItemResponse | null>(null);
  const [selectedInvoiceType, setSelectedInvoiceType] = useState<LookupItemResponse | null>(null);
  const [invoiceRanges, setInvoiceRanges] = useState<InvoiceRange[]>([{ id: crypto.randomUUID(), from: '', to: '' }]);
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});
  const [formErrors, setFormErrors] = useState<string[]>([]);
  const [warnings, setWarnings] = useState<string[]>([]);

  const handleSellerSelect = (client: ClientLocationResponse | null) => {
    setSellerClient(client);
    setSellerNumber(client?.clientNumber ?? '');
    if (client) setSellerTypedName(client.clientName ?? '');
  };

  const handleBuyerSelect = (client: ClientLocationResponse | null) => {
    setBuyerClient(client);
    setBuyerNumber(client?.clientNumber ?? '');
    if (client) setBuyerTypedName(client.clientName ?? '');
  };

  const handleAddRange = () => {
    setInvoiceRanges((prev) => [...prev, { id: crypto.randomUUID(), from: '', to: '' }]);
  };

  const handleRangeChange = (id: string, field: 'from' | 'to', value: string) => {
    const upper = value.toUpperCase();
    setInvoiceRanges((prev) => prev.map((r) => (r.id === id ? { ...r, [field]: upper } : r)));
  };

  const buildRequest = (reportFormat: 'PDF' | 'CSV') => {
    const invoiceNumbers = buildInvoiceList(invoiceRanges);
    return {
      reportFormat,
      ...(dateFrom && { dateFrom: formatDate(dateFrom) }),
      ...(dateTo && { dateTo: formatDate(dateTo) }),
      ...(sellerNumber.trim() && { sellerClientNumber: sellerNumber.trim() }),
      ...(sellerClient?.clientLocnCode && { sellerLocCode: sellerClient.clientLocnCode }),
      ...(buyerNumber.trim() && { buyerClientNumber: buyerNumber.trim() }),
      ...(buyerClient?.clientLocnCode && { buyerLocCode: buyerClient.clientLocnCode }),
      ...(submissionId.trim() && { submissionId: Number(submissionId.trim()) }),
      ...(selectedMaturities.length > 0 && { maturityCodes: selectedMaturities.map((m) => m.code).join(',') }),
      ...(selectedInvoiceStatus && { logSaleEntryStatusCode: selectedInvoiceStatus.code }),
      ...(selectedInvoiceType && { cspInvoiceTypeCode: selectedInvoiceType.code }),
      ...(invoiceNumbers && { invoiceNumbers }),
    };
  };

  const handleExport = (reportFormat: 'PDF' | 'CSV') => {
    const hasInvoiceNumbers = invoiceRanges.some((r) => r.from.trim() || r.to.trim());
    const clientResult = validateR06({
      dateFrom,
      dateTo,
      hasInvoiceNumbers,
      submissionId,
      sellerName: sellerTypedName,
      sellerNumber,
      buyerName: buyerTypedName,
      buyerNumber,
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
    <div className="r06-page">
      <Grid>
        <Column lg={16} md={8} sm={4}>
          <h1 className="r06-page__title">R06 - Invoice print out report</h1>
        </Column>

        <Column lg={16} md={8} sm={4}>
          <h2 className="r06-page__section-heading">Report details</h2>
        </Column>

        {(formErrors.length > 0 || warnings.length > 0) && (
          <Column lg={16} md={8} sm={4} className="r06-page__form-col">
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

        <Column lg={3} md={4} sm={4} className="r06-page__form-col r06-page__form-col--left">
          <DateInput
            id="start-date"
            labelText={<RequiredLabel>Start date (report range)</RequiredLabel>}
            invalid={!!fieldErrors.startDate}
            invalidText={fieldErrors.startDate}
            onChange={(dates) => {
              setDateFrom(dates[0] ?? null);
              setFieldErrors((prev) => ({ ...prev, startDate: '' }));
            }}
          />
        </Column>
        <Column lg={3} md={4} sm={4} className="r06-page__form-col">
          <DateInput
            id="end-date"
            labelText={<RequiredLabel>End date (report range)</RequiredLabel>}
            invalid={!!fieldErrors.endDate}
            invalidText={fieldErrors.endDate}
            onChange={(dates) => {
              setDateTo(dates[0] ?? null);
              setFieldErrors((prev) => ({ ...prev, endDate: '' }));
            }}
          />
        </Column>
        <Column lg={10} md={8} sm={4} className="r06-page__row-break" />

        <Column lg={6} md={8} sm={4} className="r06-page__form-col">
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
        <Column lg={10} md={8} sm={4} className="r06-page__row-break" />

        <Column lg={6} md={8} sm={4} className="r06-page__form-col">
          <ClientAutocomplete
            id="seller-client"
            titleText="Seller name"
            selectedClient={sellerClient}
            onSelect={handleSellerSelect}
            onTypedChange={setSellerTypedName}
          />
        </Column>
        <Column lg={10} md={8} sm={4} className="r06-page__row-break" />

        <Column lg={6} md={8} sm={4} className="r06-page__form-col">
          <ClientNumberAutocomplete
            id="seller-number"
            titleText="Seller number"
            selectedClient={sellerClient}
            onSelect={handleSellerSelect}
          />
        </Column>
        <Column lg={10} md={8} sm={4} className="r06-page__row-break" />

        <Column lg={6} md={8} sm={4} className="r06-page__form-col">
          <ClientAutocomplete
            id="buyer-client"
            titleText="Buyer name"
            selectedClient={buyerClient}
            onSelect={handleBuyerSelect}
            onTypedChange={setBuyerTypedName}
          />
        </Column>
        <Column lg={10} md={8} sm={4} className="r06-page__row-break" />

        <Column lg={6} md={8} sm={4} className="r06-page__form-col">
          <ClientNumberAutocomplete
            id="buyer-number"
            titleText="Buyer number"
            selectedClient={buyerClient}
            onSelect={handleBuyerSelect}
          />
        </Column>
        <Column lg={10} md={8} sm={4} className="r06-page__row-break" />

        <Column lg={6} md={8} sm={4} className="r06-page__form-col">
          <TextInput
            id="submission-number"
            labelText="Submission number"
            value={submissionId}
            maxLength={10}
            invalid={!!fieldErrors.submissionNumber}
            invalidText={fieldErrors.submissionNumber}
            onChange={(e) => {
              setSubmissionId(e.target.value);
              setFieldErrors((prev) => ({ ...prev, submissionNumber: '' }));
            }}
          />
        </Column>
        <Column lg={10} md={8} sm={4} className="r06-page__row-break" />

        <Column lg={3} md={4} sm={4} className="r06-page__form-col r06-page__form-col--left">
          <SingleSelect
            size="lg"
            id="invoice-status"
            titleText="Invoice status"
            label=""
            items={statusItems}
            selectedItem={selectedInvoiceStatus}
            disabled={statusLoading}
            itemToString={lookupDescriptionToString}
            onChange={({ selectedItem }) => setSelectedInvoiceStatus(selectedItem ?? null)}
          />
        </Column>
        <Column lg={3} md={4} sm={4} className="r06-page__form-col">
          <SingleSelect
            size="lg"
            id="invoice-type"
            titleText="Invoice type"
            label=""
            items={typeItems}
            selectedItem={selectedInvoiceType}
            disabled={typeLoading}
            itemToString={lookupDescriptionToString}
            onChange={({ selectedItem }) => setSelectedInvoiceType(selectedItem ?? null)}
          />
        </Column>
        <Column lg={10} md={8} sm={4} className="r06-page__row-break" />

        <Column lg={16} md={8} sm={4}>
          <h2 className="r06-page__section-heading r06-page__section-heading--spaced">Add invoice number range</h2>
        </Column>

        {invoiceRanges.map((range) => (
          <React.Fragment key={range.id}>
            <Column lg={3} md={4} sm={4} className="r06-page__form-col r06-page__form-col--left">
              <TextInput
                id={`from-${range.id}`}
                labelText="From"
                value={range.from}
                maxLength={15}
                onChange={(e) => handleRangeChange(range.id, 'from', e.target.value)}
              />
            </Column>
            <Column lg={3} md={4} sm={4} className="r06-page__form-col">
              <TextInput
                id={`to-${range.id}`}
                labelText="To"
                value={range.to}
                maxLength={15}
                onChange={(e) => handleRangeChange(range.id, 'to', e.target.value)}
              />
            </Column>
            <Column lg={10} md={8} sm={4} className="r06-page__row-break" />
          </React.Fragment>
        ))}

        <Column lg={3} md={4} sm={4} className="r06-page__add-range-col">
          {!isPending && (
            <Button kind="ghost" renderIcon={Add} onClick={handleAddRange}>
              Add another range
            </Button>
          )}
        </Column>
        <Column lg={13} md={8} sm={4} className="r06-page__row-break" />

        <Column lg={3} md={4} sm={2} className="r06-page__export-btn-col r06-page__export-btn-col--left">
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
        <Column lg={3} md={4} sm={2} className="r06-page__export-btn-col">
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
