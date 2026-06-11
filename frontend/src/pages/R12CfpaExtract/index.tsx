import React from 'react';
import { Button, Column, Grid, InlineLoading, InlineNotification } from '@carbon/react';
import { DocumentExport } from '@carbon/icons-react';

import DateInput from '@/components/Form/DateInput';
import RequiredLabel from '@/components/Form/RequiredLabel';
import SingleSelect from '@/components/Form/SingleSelect';
import TaggedMultiSelect from '@/components/Form/TaggedMultiSelect';
import { useNotification } from '@/context/notification/useNotification';
import { type LookupItemResponse, useMaturityCodesWithCantsQuery } from '@/services/lookup.service';
import { useR12ReportMutation } from '@/services/r12.service';
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
import { validateR12 } from '@/validations/reports/r12';
import { MESSAGE_KEY_TO_FIELD } from './messageKeyMap';

import './index.scss';

const REPORT_YEAR_START = 2000;
const currentYear = new Date().getFullYear();
const YEAR_ITEMS: SelectItem[] = Array.from({ length: currentYear - REPORT_YEAR_START + 1 }, (_, i) => {
  const y = String(REPORT_YEAR_START + i);
  return { id: y, label: y };
});

const MONTH_NAMES = [
  'January',
  'February',
  'March',
  'April',
  'May',
  'June',
  'July',
  'August',
  'September',
  'October',
  'November',
  'December',
];
const MONTH_ITEMS: SelectItem[] = MONTH_NAMES.map((name, i) => ({
  id: String(i + 1),
  label: `${String(i + 1).padStart(2, '0')} - ${name}`,
}));

export function R12CfpaExtractPage() {
  const { mutate: generateReport, isPending } = useR12ReportMutation();
  const { addNotification } = useNotification();

  const { data: maturityItems = [], isLoading: maturityLoading } = useMaturityCodesWithCantsQuery();

  const [year, setYear] = React.useState('');
  const [month, setMonth] = React.useState('');
  const [dateFrom, setDateFrom] = React.useState<Date | null>(null);
  const [dateTo, setDateTo] = React.useState<Date | null>(null);
  const [timeFrame, setTimeFrame] = React.useState('');
  const [selectedMaturities, setSelectedMaturities] = React.useState<LookupItemResponse[]>([]);
  const [fieldErrors, setFieldErrors] = React.useState<Record<string, string>>({});
  const [formErrors, setFormErrors] = React.useState<string[]>([]);
  const [warnings, setWarnings] = React.useState<string[]>([]);

  const buildRequest = (reportFormat: 'PDF' | 'CSV') => ({
    reportFormat,
    ...(year && { year: parseInt(year, 10) }),
    ...(month && { month: parseInt(month, 10) }),
    ...(dateFrom && { dateFrom: formatDate(dateFrom) }),
    ...(dateTo && { dateTo: formatDate(dateTo) }),
    ...(timeFrame && { timeFrame }),
    ...(selectedMaturities.length > 0 && { logSaleTypeCode: selectedMaturities.map((m) => m.code).join(',') }),
  });

  const handleExport = (reportFormat: 'PDF' | 'CSV') => {
    const clientResult = validateR12(year, dateFrom, dateTo, timeFrame);
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
    <div className="r12-page">
      <Grid>
        <Column lg={16} md={8} sm={4}>
          <h1 className="r12-page__title">R12 CFPA Detailed data extract</h1>
        </Column>

        {(formErrors.length > 0 || warnings.length > 0) && (
          <Column lg={16} md={8} sm={4} className="r12-page__form-col">
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

        <Column lg={3} md={4} sm={4} className="r12-page__form-col r12-page__form-col--left">
          <SingleSelect
            size="lg"
            id="report-year"
            titleText={<RequiredLabel>Report year</RequiredLabel>}
            label="Select"
            items={YEAR_ITEMS}
            selectedItem={YEAR_ITEMS.find((i) => i.id === year) ?? null}
            itemToString={itemToString}
            onChange={({ selectedItem }) => setYear(selectedItem?.id ?? '')}
          />
        </Column>
        <Column lg={3} md={4} sm={4} className="r12-page__form-col">
          <SingleSelect
            size="lg"
            id="report-month"
            titleText={<RequiredLabel>Report month</RequiredLabel>}
            label="Select"
            items={MONTH_ITEMS}
            selectedItem={MONTH_ITEMS.find((i) => i.id === month) ?? null}
            itemToString={itemToString}
            onChange={({ selectedItem }) => setMonth(selectedItem?.id ?? '')}
          />
        </Column>
        <Column lg={10} md={8} sm={4} className="r12-page__row-break" />

        <Column lg={2} md={4} sm={4} className="r12-page__form-col r12-page__form-col--left">
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
        <Column lg={2} md={4} sm={4} className="r12-page__form-col r12-page__form-col--left">
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
        <Column lg={2} md={4} sm={4} className="r12-page__form-col">
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
        <Column lg={10} md={8} sm={4} className="r12-page__row-break" />

        <Column lg={3} md={4} sm={4} className="r12-page__form-col r12-page__form-col--left">
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
        <Column lg={13} md={8} sm={4} className="r12-page__row-break" />

        <Column lg={3} md={4} sm={2} className="r12-page__export-btn-col r12-page__export-btn-col--left">
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
        <Column lg={3} md={4} sm={2} className="r12-page__export-btn-col">
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
