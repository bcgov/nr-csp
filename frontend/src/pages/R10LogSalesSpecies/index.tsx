import React from 'react';
import { Button, Column, Grid, InlineLoading, InlineNotification } from '@carbon/react';
import { DocumentExport } from '@carbon/icons-react';

import ClientAutocomplete, { type ClientLocationResponse } from '@/components/Form/ClientAutocomplete';
import DateInput from '@/components/Form/DateInput';
import RequiredLabel from '@/components/Form/RequiredLabel';
import SingleSelect from '@/components/Form/SingleSelect';
import TaggedMultiSelect from '@/components/Form/TaggedMultiSelect';
import { useNotification } from '@/context/notification/useNotification';
import {
  type LookupItemResponse,
  useInvoiceTypesQuery,
  useMaturityCodesWithCantsQuery,
} from '@/services/lookup.service';
import { useR10ReportMutation } from '@/services/r10.service';
import {
  type SelectItem,
  TIME_FRAME_ITEMS,
  formatDate,
  downloadBlob,
  itemToString,
  lookupDescriptionToString,
  isNoDataError,
} from '@/utils/report';
import { parseReportValidationError } from '@/services/reportValidation';
import { splitMessages } from '@/validations/validationResult';
import { validateR10 } from '@/validations/reports/r10';
import { MESSAGE_KEY_TO_FIELD } from './messageKeyMap';

import './index.scss';

export function R10LogSalesSpeciesPage() {
  const { mutate: generateReport, isPending } = useR10ReportMutation();
  const { addNotification } = useNotification();

  const { data: maturityItems = [], isLoading: maturityLoading } = useMaturityCodesWithCantsQuery();
  const { data: typeItems = [], isLoading: typeLoading } = useInvoiceTypesQuery();

  const [dateFrom, setDateFrom] = React.useState<Date | null>(null);
  const [dateTo, setDateTo] = React.useState<Date | null>(null);
  const [timeFrame, setTimeFrame] = React.useState('');
  const [selectedMaturities, setSelectedMaturities] = React.useState<LookupItemResponse[]>([]);
  const [sellerClient, setSellerClient] = React.useState<ClientLocationResponse | null>(null);
  const [buyerClient, setBuyerClient] = React.useState<ClientLocationResponse | null>(null);
  const [selectedInvoiceType, setSelectedInvoiceType] = React.useState<LookupItemResponse | null>(null);
  const [fieldErrors, setFieldErrors] = React.useState<Record<string, string>>({});
  const [formErrors, setFormErrors] = React.useState<string[]>([]);
  const [warnings, setWarnings] = React.useState<string[]>([]);

  const buildRequest = (reportFormat: 'PDF' | 'CSV') => ({
    reportFormat,
    ...(dateFrom && { dateFrom: formatDate(dateFrom) }),
    ...(dateTo && { dateTo: formatDate(dateTo) }),
    ...(timeFrame && { timeFrame }),
    ...(selectedMaturities.length > 0 && { maturityCodes: selectedMaturities.map((m) => m.code).join(',') }),
    ...(sellerClient && { sellerClientNumber: sellerClient.clientNumber }),
    ...(buyerClient && { buyerClientNumber: buyerClient.clientNumber }),
    ...(selectedInvoiceType && { invoiceTypeCode: selectedInvoiceType.code }),
  });

  const handleExport = (reportFormat: 'PDF' | 'CSV') => {
    const clientResult = validateR10(dateFrom, dateTo, timeFrame);
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
    <div className="r10-page">
      <Grid>
        <Column lg={16} md={8} sm={4}>
          <h1 className="r10-page__title">R10 - Log sales species / sort grade report</h1>
        </Column>

        {(formErrors.length > 0 || warnings.length > 0) && (
          <Column lg={16} md={8} sm={4} className="r10-page__form-col">
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
          <h2 className="r10-page__section-heading">Report range</h2>
        </Column>

        <Column lg={2} md={4} sm={4} className="r10-page__form-col r10-page__form-col--left">
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
        <Column lg={2} md={4} sm={4} className="r10-page__form-col r10-page__form-col--left">
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
        <Column lg={2} md={4} sm={4} className="r10-page__form-col">
          <SingleSelect
            size="lg"
            id="time-frame"
            titleText={<RequiredLabel>Time frame</RequiredLabel>}
            label=""
            items={TIME_FRAME_ITEMS}
            selectedItem={TIME_FRAME_ITEMS.find((i) => i.id === timeFrame) ?? null}
            itemToString={itemToString}
            invalid={!!fieldErrors.timeFrame}
            invalidText={fieldErrors.timeFrame}
            onChange={({ selectedItem }) => {
              setTimeFrame(selectedItem?.id ?? '');
              setFieldErrors((prev) => ({ ...prev, timeFrame: '' }));
            }}
          />
        </Column>
        <Column lg={10} md={8} sm={4} className="r10-page__row-break" />

        <Column lg={16} md={8} sm={4}>
          <h2 className="r10-page__section-heading r10-page__section-heading--spaced">Invoice information</h2>
        </Column>

        <Column lg={3} md={4} sm={4} className="r10-page__form-col r10-page__form-col--left">
          <ClientAutocomplete
            id="seller-client"
            titleText="Seller name"
            selectedClient={sellerClient}
            onSelect={setSellerClient}
          />
        </Column>
        <Column lg={3} md={4} sm={4} className="r10-page__form-col">
          <ClientAutocomplete
            id="buyer-client"
            titleText="Buyer name"
            selectedClient={buyerClient}
            onSelect={setBuyerClient}
          />
        </Column>
        <Column lg={10} md={8} sm={4} className="r10-page__row-break" />

        <Column lg={3} md={4} sm={4} className="r10-page__form-col r10-page__form-col--left">
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
        <Column lg={3} md={4} sm={4} className="r10-page__form-col">
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
        <Column lg={10} md={8} sm={4} className="r10-page__row-break" />

        <Column lg={3} md={4} sm={2} className="r10-page__export-btn-col r10-page__export-btn-col--left">
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
        <Column lg={3} md={4} sm={2} className="r10-page__export-btn-col">
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
