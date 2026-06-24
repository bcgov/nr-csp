import { Add, Checkmark, Download, Edit, Save, TrashCan } from '@carbon/icons-react';
import {
  Accordion,
  AccordionItem,
  Button,
  ButtonSkeleton,
  Column,
  Grid,
  IconButton,
  InlineNotification,
  Loading,
  MenuButton,
  MenuItem,
  SkeletonPlaceholder,
  SkeletonText,
  TableCell,
  TableRow,
  TableToolbar,
  TableToolbarContent,
  TextInput,
} from '@carbon/react';
import { useCallback, useEffect, useMemo, useRef, useState, type ReactNode } from 'react';
import { useLocation, useNavigate, useParams } from 'react-router-dom';

import FormModal from '@/components/core/FormModal';
import PageTitle from '@/components/core/PageTitle';
import InvoiceDetailsTag from '@/components/core/Tags/InvoiceDetailsTag';
import InvoiceStatusTag from '@/components/core/Tags/InvoiceStatusTag';
import ClientAutocomplete, { type ClientLocationResponse } from '@/components/Form/ClientAutocomplete';
import ClientNumberAutocomplete from '@/components/Form/ClientNumberAutocomplete';
import DateInput from '@/components/Form/DateInput';
import EditableLineItemsTable, {
  type EditableLineItemDraft,
  type EditableLineItemRow,
} from '@/components/Form/EditableLineItemsTable';
import RequiredLabel from '@/components/Form/RequiredLabel';
import ResultsTable, { type ResultsTableColumn } from '@/components/Form/ResultsTable';
import SingleSelect from '@/components/Form/SingleSelect';
import TagInput from '@/components/Form/TagInput';
import TextArea from '@/components/Form/TextArea';
import { useNotification } from '@/context/notification/useNotification';
import { ROUTES } from '@/routes/routePaths';
import { useFobCodesQuery } from '@/services/fob.service';
import { usePermission } from '@/context/auth/usePermission';
import {
  INVOICE_DETAILS_SAVE,
  INVOICE_DETAILS_SUBMIT,
  INVOICE_DETAILS_APPROVE,
  INVOICE_DETAILS_DUPLICATE,
  INVOICE_DETAILS_CANCEL,
  INVOICE_DETAILS_REJECT,
  INVOICE_DETAILS_DELETE,
} from '@/context/auth/permissions';
import {
  type CreateInvoiceRequest,
  type LineItemRequest,
  type LineItemResponse,
  type ValidationMessageResponse,
  extractApiErrorMessage,
  extractValidationErrors,
  useAddInvoiceLineItemMutation,
  useChangeInvoiceStatusMutation,
  useCreateInvoiceMutation,
  useDeleteInvoiceLineItemMutation,
  useDeleteInvoiceMutation,
  useDuplicateInvoiceMutation,
  useExportInvoiceGroupSummaryMutation,
  useInvoiceQuery,
  useSubmitInvoiceMutation,
  useUpdateInvoiceLineItemMutation,
  useUpdateInvoiceMutation,
} from '@/services/invoice.service';
import { type LookupItemResponse, useInvoiceTypesQuery, useMaturityCodesQuery } from '@/services/lookup.service';
import { downloadBlob } from '@/utils/report';
import {
  useGradeLookupQuery,
  useSortCodesLookupQuery,
  useSpeciesGradeCombosQuery,
  useSpeciesLookupQuery,
} from '@/services/lookup.service';
import { getClientsByNumber } from '@/services/search.service';
import { formatCurrency, formatIsoDate, formatNumber } from '@/utils/format';

import {
  validate as validateInvoiceFields,
  validateLineItem,
  type InvoiceFieldValues,
} from '@/validations/invoice/invoice.ts';
import { splitMessages } from '@/validations/validationResult';
import {
  CLIENT_LINE_ITEM_MESSAGE_KEY_TO_FIELD,
  CLIENT_MESSAGE_KEY_TO_FIELD,
  LINE_ITEM_MESSAGE_KEY_TO_FIELD,
  MESSAGE_KEY_TO_FIELD,
} from './messageKeyMap';
import './index.scss';

// -------------------------------------------------------------------
// Wiring lives below — each user action runs the matching React Query
// mutation from `@/services/invoice.service`. Per-field validation
// errors arrive in `ValidationErrorResponse.errors[]` keyed by
// `messageKey`; warnings come back on every successful response in
// `InvoiceResponse.warnings[]`.
// -------------------------------------------------------------------

const SUBMITTED_BY_OPTIONS: LookupItemResponse[] = [
  { code: 'Buyer', description: 'Buyer' },
  { code: 'Seller', description: 'Seller' },
];

// Status codes that allow editing the invoice header detail fields (and Save).
// A brand-new (unsaved) invoice is always editable; an existing one's header is
// editable only in DFT/PRO/UNA. APP, REJ and CAN lock the header (matching the
// legacy app — rejected/cancelled/approved invoices are read-only headers).
// Saving a PRO/UNA invoice reverts the record to DFT (backend).
const EDITABLE_STATUSES = new Set(['DFT', 'PRO', 'UNA']);
// Statuses in which line items / groups can be edited or deleted. Allowed in
// every status (legacy parity): APP keeps group-only editing (see
// `canEditLineRows`), the rest allow both row- and group-level edit/delete.
const LINE_ITEM_EDITABLE_STATUSES = new Set(['DFT', 'PRO', 'APP', 'UNA', 'REJ', 'CAN']);
// Statuses in which Submit is offered (plus NEW / unsaved). Note PRO is NOT
// submittable — a processing invoice is already submitted.
const SUBMITTABLE_STATUSES = new Set(['DFT', 'UNA']);
// Statuses in which Duplicate is offered (existing invoices only).
const DUPLICABLE_STATUSES = new Set(['DFT', 'PRO', 'UNA']);
// Statuses in which Delete is offered (existing invoices only — not UNA).
const DELETABLE_STATUSES = new Set(['DFT', 'PRO']);
// Status codes from which Approve / Reject / Cancel are valid transitions.
const STATUS_CHANGEABLE = new Set(['PRO', 'UNA']);

const lookupDescription = (item: LookupItemResponse | null | undefined): string => item?.description ?? '';

const findByCode = (items: LookupItemResponse[], code: string | null | undefined): LookupItemResponse | null =>
  code ? (items.find((i) => i.code === code) ?? null) : null;

// Maps a backend LineItemResponse into the row shape expected by EditableLineItemsTable.
const toLineItemRow = (line: LineItemResponse): EditableLineItemRow => ({
  id: String(line.lineItemID),
  secondarySort: line.secondSort,
  species: line.species,
  clientSecondarySort: line.clientSecondarySort ?? '',
  numberPieces: line.numOfPieces,
  grade: line.grade,
  volume: Number(line.volume ?? 0),
  price: Number(line.price ?? 0),
  amount: Number(line.amount ?? 0),
});

type GroupRow = {
  id: string;
  groupNumber: number;
  secondarySort: string;
  description: string;
  species: string;
  totalPieces: number;
  totalVolume: number;
  totalAmount: number;
  priceConversion: string;
  lineItems: LineItemResponse[];
};

// Group flat line items by (species + secondSort + EXACT price) and compute
// group totals. Pure function — used both for the loaded invoice's lines and
// for any lines added/removed during the edit session.
//
// Two line items end up in DIFFERENT groups when any of these three differ:
//   • species
//   • secondSort
//   • price (exact — $312.42 and $312.45 are separate groups)
//
// Resulting groups are ordered by species → secondSort → price.
function groupLineItems(lines: LineItemResponse[], sortCodeItems: LookupItemResponse[] = []): GroupRow[] {
  const sortDescriptionByCode = new Map(sortCodeItems.map((s) => [s.code, s.description]));
  const groups = new Map<string, LineItemResponse[]>();
  lines.forEach((line) => {
    const priceKey = String(Number(line.price ?? 0));
    const key = `${line.species ?? ''}::${line.secondSort ?? ''}::${priceKey}`;
    const existing = groups.get(key) ?? [];
    existing.push(line);
    groups.set(key, existing);
  });

  const sortedEntries = Array.from(groups.entries()).sort(([keyA], [keyB]) => {
    const [speciesA, sortA, priceA] = keyA.split('::');
    const [speciesB, sortB, priceB] = keyB.split('::');
    const speciesCmp = speciesA.localeCompare(speciesB, undefined, { sensitivity: 'base' });
    if (speciesCmp !== 0) return speciesCmp;
    const sortCmp = sortA.localeCompare(sortB, undefined, { sensitivity: 'base' });
    if (sortCmp !== 0) return sortCmp;
    return Number(priceA) - Number(priceB);
  });

  return sortedEntries.map(([key, lineItems], i) => {
    const [species, secondSort] = key.split('::');
    // Resolve the secondary sort code to its description from the sort-code
    // lookup; fall back to the raw code when it isn't found (e.g. an empty
    // lookup during local-only "added" lines before save).
    const secondSortDescription = sortDescriptionByCode.get(secondSort) ?? secondSort;
    return {
      id: `g-${i + 1}-${key}`,
      groupNumber: i + 1,
      secondarySort: secondSort,
      description: secondSortDescription,
      species,
      totalPieces: lineItems.reduce((s, l) => s + (l.numOfPieces ?? 0), 0),
      totalVolume: lineItems.reduce((s, l) => s + Number(l.volume ?? 0), 0),
      totalAmount: lineItems.reduce((s, l) => s + Number(l.amount ?? 0), 0),
      priceConversion: lineItems.some((l) => l.convertedPrice != null) ? 'Y' : 'N',
      lineItems,
    };
  });
}

// "Group <g> Line <l> - <message>", where l is the line's 1-based position
// within its group. A "New"/unresolvable id drops the reference entirely
// (just the message is kept).
const LINE_LABEL_RE = / ?Line #(\d+|New)\b/;

const buildLineLabelMap = (groups: GroupRow[]): Map<number, string> => {
  const map = new Map<number, string>();
  groups.forEach((group) => {
    group.lineItems.forEach((line, idx) => {
      if (line.lineItemID != null) {
        map.set(line.lineItemID, `Group ${group.groupNumber} Line ${idx + 1}`);
      }
    });
  });
  return map;
};

const rewriteLineLabel = (text: string, labelMap: Map<number, string>): string => {
  if (!text) return text;
  let prefix = '';
  const body = text.replace(LINE_LABEL_RE, (_full, token: string) => {
    if (token !== 'New') {
      const friendly = labelMap.get(Number(token));
      if (friendly) prefix = `${friendly} - `;
    }
    return ''; // strip the trailing label from the body
  });
  const tidied = body
    .replace(/\s*\.\s*$/, '.')
    .replace(/\s{2,}/g, ' ')
    .trim();
  return prefix + tidied;
};

export function InvoicePage() {
  const { id: idParam } = useParams<{ id?: string }>();
  const invoiceId = idParam ? Number(idParam) : undefined;
  const isExisting = invoiceId !== undefined && !Number.isNaN(invoiceId);
  const navigate = useNavigate();
  const location = useLocation();
  const { addNotification } = useNotification();

  const fromSearch = (location.state as { fromSearch?: boolean } | null)?.fromSearch === true;

  // -----------------------------------------------------------------
  // Lookup data
  // -----------------------------------------------------------------
  const { data: maturityItems = [] } = useMaturityCodesQuery();
  const { data: invoiceTypeItems = [] } = useInvoiceTypesQuery();
  const { data: speciesItems = [] } = useSpeciesLookupQuery();
  const { data: sortCodeItems = [] } = useSortCodesLookupQuery();
  const { data: gradeItems = [] } = useGradeLookupQuery();
  const { data: fobItems = [] } = useFobCodesQuery();
  // Full active (species, grade) xref — cached once per session and used
  // to filter the Add New Line Item form's Species and Grade dropdowns
  // against each other so the user can only pick valid combinations.
  const { data: speciesGradeCombos = [] } = useSpeciesGradeCombosQuery();

  // -----------------------------------------------------------------
  // Server data — loaded invoice + mutations
  // -----------------------------------------------------------------
  const { data: loadedInvoice, isLoading: invoiceLoading } = useInvoiceQuery(invoiceId);
  const createMutation = useCreateInvoiceMutation();
  const updateMutation = useUpdateInvoiceMutation();
  const submitMutation = useSubmitInvoiceMutation();
  const duplicateMutation = useDuplicateInvoiceMutation();
  const deleteMutation = useDeleteInvoiceMutation();
  const changeStatusMutation = useChangeInvoiceStatusMutation();
  const addLineItemMutation = useAddInvoiceLineItemMutation();
  const deleteLineItemMutation = useDeleteInvoiceLineItemMutation();
  const updateLineItemMutation = useUpdateInvoiceLineItemMutation();
  const exportMutation = useExportInvoiceGroupSummaryMutation();

  // -----------------------------------------------------------------
  // Form state — primitive codes/strings, lookup objects derived via
  // `findByCode(items, code)` so we don't have to wait for lookup data
  // to load before hydrating from the server response.
  // -----------------------------------------------------------------
  // Section 1 — Invoice details
  const [invNumber, setInvNumber] = useState('');
  const [invTypeCode, setInvTypeCode] = useState('');
  const [invDate, setInvDate] = useState('');
  const [replaceInvNum, setReplaceInvNum] = useState<string[]>([]);
  const [adjustInvNum, setAdjustInvNum] = useState<string[]>([]);

  const breadCrumbs = [
    ...(fromSearch ? [{ name: 'Search Results', path: ROUTES.SEARCH }] : []),
    { name: 'Invoice', path: '#' },
    ...(invNumber ? [{ name: invNumber, path: '#' }] : []),
  ];

  // Section 2 — Invoice address information
  const [submittedByCode, setSubmittedByCode] = useState('');
  const [submittingClientNumber, setSubmittingClientNumber] = useState('');
  const [submittingClientLocation, setSubmittingClientLocation] = useState('');
  const [otherClientNumber, setOtherClientNumber] = useState('');
  const [otherClientLocation, setOtherClientLocation] = useState('');
  const [otherClientName, setOtherClientName] = useState('');
  const [submittingClientCity, setSubmittingClientCity] = useState('');
  const [submittingClientProvState, setSubmittingClientProvState] = useState('');
  const [otherClientCity, setOtherClientCity] = useState('');
  const [otherClientProvState, setOtherClientProvState] = useState('');
  // Resolved client objects — shared between each pair of name + number
  // autocompletes so a selection in one field mirrors into the other.
  const [submittingClient, setSubmittingClient] = useState<ClientLocationResponse | null>(null);
  const [otherClient, setOtherClient] = useState<ClientLocationResponse | null>(null);

  const needsSubmittingClient = !!loadedInvoice?.submitterClientNum;
  const needsOtherClient = !!loadedInvoice?.otherClientNum;
  const clientsResolved =
    (!needsSubmittingClient || submittingClient !== null) && (!needsOtherClient || otherClient !== null);

  // Latch the "loaded" state once the invoice and its client lookups have
  // resolved for the first time. We must NOT derive loading from the live
  // `clientsResolved` alone: the user clearing a client field flips
  // `submittingClient`/`otherClient` back to null, and that should leave the
  // form on screen — not snap it back to the skeleton. Reset on invoiceId
  // change (see the reset effect below).
  const [initialLoadComplete, setInitialLoadComplete] = useState(false);
  useEffect(() => {
    if (isExisting && !invoiceLoading && loadedInvoice && clientsResolved) {
      setInitialLoadComplete(true);
    }
  }, [isExisting, invoiceLoading, loadedInvoice, clientsResolved]);

  const isLoadingInvoice = isExisting && !initialLoadComplete;

  // Section 3 — Invoice detail information
  const [maturityCode, setMaturityCode] = useState('');
  const [fobCodeValue, setFobCodeValue] = useState('');
  const [primarySortCodeValue, setPrimarySortCodeValue] = useState('');
  const [boomNumbers, setBoomNumbers] = useState<string[]>([]);
  const [timberMarks, setTimberMarks] = useState<string[]>([]);
  const [weighSlips, setWeighSlips] = useState<string[]>([]);
  const [clientPrimarySortCode, setClientPrimarySortCode] = useState('');
  const [reviewerComment, setReviewerComment] = useState('');

  // Section 4 — Comments
  const [submitterComment, setSubmitterComment] = useState('');

  // Section 5 — Line items (the source of truth; group rows are derived)
  const [lineItems, setLineItems] = useState<LineItemResponse[]>([]);

  // Section 6 — Add New Line Item form
  const [newLineSecondarySort, setNewLineSecondarySort] = useState<LookupItemResponse | null>(null);
  const [newLineSpecies, setNewLineSpecies] = useState<LookupItemResponse | null>(null);
  const [newLineClientSort, setNewLineClientSort] = useState('');
  const [newLinePieces, setNewLinePieces] = useState('');
  const [newLineGrade, setNewLineGrade] = useState<LookupItemResponse | null>(null);
  const [newLineVolume, setNewLineVolume] = useState('');
  const [newLinePrice, setNewLinePrice] = useState('');

  // -----------------------------------------------------------------
  // Filtered species / grade lists for the Add New Line Item form.
  // Both lists react to the *other* dropdown's current value so the user
  // can only pick valid (species, grade) pairs from csp_species_grade_xref.
  // When neither side is selected, both fall back to the unfiltered
  // lookup so the dropdowns aren't empty on first render.
  // -----------------------------------------------------------------
  const filteredSpeciesItems = useMemo(() => {
    if (!newLineGrade) return speciesItems;
    const allowed = new Set(speciesGradeCombos.filter((c) => c.grade === newLineGrade.code).map((c) => c.species));
    return speciesItems.filter((s) => allowed.has(s.code));
  }, [speciesItems, speciesGradeCombos, newLineGrade]);

  const filteredGradeItems = useMemo(() => {
    if (!newLineSpecies) return gradeItems;
    const allowed = new Set(speciesGradeCombos.filter((c) => c.species === newLineSpecies.code).map((c) => c.grade));
    return gradeItems.filter((g) => allowed.has(g.code));
  }, [gradeItems, speciesGradeCombos, newLineSpecies]);

  // Read-only preview of the line amount, derived from price × volume and
  // rounded to 2 decimals (matches Utils.roundBigDecimalTwoDecimalPlace on
  // the backend). The backend is canonical — it recomputes price × volume
  // on save, with an extra sign-handling tweak for adjustment invoices
  // (`invType === "ADJ"`), and the resulting `amount` lands in the
  // response. The preview just helps the user verify their inputs.
  const computedNewLineAmount = (() => {
    // Empty price/volume count as 0 so the amount still calculates (e.g.
    // price × 0 = 0.00) instead of showing blank.
    const p = parseFloat(newLinePrice) || 0;
    const v = parseFloat(newLineVolume) || 0;
    return (Math.round(p * v * 100) / 100).toFixed(2);
  })();

  // Inline error / warning state
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});
  const [pageErrors, setPageErrors] = useState<ValidationMessageResponse[]>([]);
  const [warnings, setWarnings] = useState<ValidationMessageResponse[]>([]);

  // Scroll to the top whenever banner errors appear so the user doesn't miss them.
  useEffect(() => {
    if (pageErrors.length > 0) window.scrollTo({ top: 0, behavior: 'smooth' });
  }, [pageErrors]);
  const [reviewerCommentError, setReviewerCommentError] = useState('');
  const [newLineFieldErrors, setNewLineFieldErrors] = useState<Record<string, string>>({});
  // Controls the "Add New Line Item" modal launched from the table toolbar.
  const [addLineItemOpen, setAddLineItemOpen] = useState(false);

  // Maps each line's DB id to its on-screen "Group G Line L" position, used to
  // rewrite the backend's "Line #<id>" tags in validation messages.
  const lineLabelById = useMemo(() => buildLineLabelMap(groupLineItems(lineItems)), [lineItems]);
  const relabel = useCallback((text: string) => rewriteLineLabel(text, lineLabelById), [lineLabelById]);
  const relabelRecord = useCallback(
    (rec: Record<string, string>): Record<string, string> =>
      Object.fromEntries(Object.entries(rec).map(([field, msg]) => [field, relabel(msg)])),
    [relabel],
  );

  // ----- As-you-type field validation (mirrors the backend request DTO) -----
  const invoiceFieldValues: InvoiceFieldValues = {
    invNumber,
    invDate,
    invType: invTypeCode,
    submittedBy: submittedByCode,
    submitterLocation: submittingClientLocation,
    otherClientLocation,
  };
  const clientFieldErrors = useMemo(
    // Validator pushes message keys; map them to fields (same key->field +
    // split-messages pipeline the report pages use).
    () => splitMessages(validateInvoiceFields(invoiceFieldValues).messages, CLIENT_MESSAGE_KEY_TO_FIELD).fieldErrors,
    // Re-derive whenever any validated field changes.
    // eslint-disable-next-line react-hooks/exhaustive-deps
    [invNumber, invDate, invTypeCode, submittedByCode, submittingClientLocation, otherClientLocation],
  );
  // Map each structural error key to the value of the field it belongs to
  const errorFieldValues: Record<string, string> = {
    invNumber,
    invDate,
    invType: invTypeCode,
    submittedBy: submittedByCode,
    submittingClientLocation,
    otherClientLocation,
  };
  // Server (business-rule) errors always show; the live structural ones show
  // only for fields that currently have a value.
  const displayFieldErrors = useMemo(
    () => {
      const visible: Record<string, string> = { ...fieldErrors };
      for (const [field, msg] of Object.entries(clientFieldErrors)) {
        if ((errorFieldValues[field] ?? '').trim() !== '') visible[field] = msg;
      }
      return visible;
    },
    // eslint-disable-next-line react-hooks/exhaustive-deps
    [
      fieldErrors,
      clientFieldErrors,
      invNumber,
      invDate,
      invTypeCode,
      submittedByCode,
      submittingClientLocation,
      otherClientLocation,
    ],
  );
  const hasStructuralErrors = Object.keys(clientFieldErrors).length > 0;

  // Every required (asterisked) header field must be filled before Save/Submit
  // are available.
  const requiredFieldsFilled =
    invNumber.trim() !== '' &&
    invTypeCode.trim() !== '' &&
    invDate.trim() !== '' &&
    submittedByCode.trim() !== '' &&
    submittingClientNumber.trim() !== '' &&
    submittingClientLocation.trim() !== '' &&
    maturityCode.trim() !== '' &&
    fobCodeValue.trim() !== '' &&
    (otherClientNumber.trim() !== '' || otherClientName.trim() !== '');

  const clientNewLineErrors = useMemo(
    () =>
      splitMessages(
        validateLineItem({ pieces: newLinePieces, volume: newLineVolume, price: newLinePrice, invType: invTypeCode })
          .messages,
        CLIENT_LINE_ITEM_MESSAGE_KEY_TO_FIELD,
      ).fieldErrors,
    [newLinePieces, newLineVolume, newLinePrice, invTypeCode],
  );
  const displayNewLineErrors = useMemo(
    () => relabelRecord({ ...newLineFieldErrors, ...clientNewLineErrors }),
    [newLineFieldErrors, clientNewLineErrors, relabelRecord],
  );

  // ----- Inline single-row edit state -----
  // Draft shape is owned by EditableLineItemsTable so the wiring stays 1:1.
  const [editLineDraft, setEditLineDraft] = useState<EditableLineItemDraft | null>(null);
  const [editLineFieldErrors, setEditLineFieldErrors] = useState<Record<string, string>>({});

  // ----- Group edit modal state -----
  type EditGroupDraft = {
    groupId: string;
    lineItemIds: number[];
    originalSecondSort: string;
    originalSpecies: string;
    secondSort: string;
    species: string;
  };
  const [editGroupDraft, setEditGroupDraft] = useState<EditGroupDraft | null>(null);
  const [editGroupFieldErrors, setEditGroupFieldErrors] = useState<Record<string, string>>({});
  const [fobValidationError, setFobValidationError] = useState('');

  // Which group rows are expanded, keyed by GroupRow.id.
  const [expandedGroupIds, setExpandedGroupIds] = useState<Set<string>>(new Set());
  // Tracks whether we've already auto-expanded every group for the currently
  // loaded invoice.
  const hasAutoExpandedGroupsRef = useRef(false);

  // Pending delete-confirmation modal. Each delete action opens this with a
  // title/message and the callback to run on confirm; the shared FormModal at
  // the bottom of the render drives it.
  type DeleteConfirm = { title: string; message: string; onConfirm: () => void };
  const [deleteConfirm, setDeleteConfirm] = useState<DeleteConfirm | null>(null);

  // Disable Save/Submit while ANY field is showing a validation error: the
  // live structural checks, server-returned field errors, the FOB lookup
  // check, or the reviewer-comment check.
  const hasAnyFieldError =
    hasStructuralErrors ||
    Object.keys(fieldErrors).length > 0 ||
    fobValidationError !== '' ||
    reviewerCommentError !== '';

  // -----------------------------------------------------------------
  // Filtered species list for the group-edit modal — only species that
  // form a valid combination with EVERY grade currently in the group.
  // Falls back to the full list if no group is being edited.
  // -----------------------------------------------------------------
  const groupEditFilteredSpecies = useMemo(() => {
    if (!editGroupDraft) return speciesItems;
    const gradesInGroup = Array.from(
      new Set(lineItems.filter((l) => editGroupDraft.lineItemIds.includes(l.lineItemID)).map((l) => l.grade)),
    );
    if (gradesInGroup.length === 0) return speciesItems;
    return speciesItems.filter((s) =>
      gradesInGroup.every((g) => speciesGradeCombos.some((c) => c.species === s.code && c.grade === g)),
    );
  }, [editGroupDraft, lineItems, speciesItems, speciesGradeCombos]);

  // Run on blur of the FOB text input: check the entered code against the
  // /api/lookup/fob list. Empty values clear the error (required-ness is
  // enforced server-side via the standard fieldErrors flow). Skip while
  // the FOB list is still loading so we don't false-flag a valid code.
  const validateFobOnBlur = () => {
    if (fobCodeValue.trim() === '') {
      setFobValidationError('');
      return;
    }
    if (fobItems.length === 0) return;
    const exists = fobItems.some((f) => f.code === fobCodeValue.trim());
    setFobValidationError(exists ? '' : `FOB code "${fobCodeValue}" is not a valid FOB location.`);
  };

  // -----------------------------------------------------------------
  // Reset all form state whenever the URL invoice id changes — this
  // prevents stale data from a previously-loaded invoice leaking into a
  // new-invoice page (`/invoice/123` → `/invoice`) or showing momentarily
  // when switching between two existing invoices.
  //
  // The hydration effect below then re-fills the form once the new
  // invoice's GET resolves.
  // -----------------------------------------------------------------
  useEffect(() => {
    setInvNumber('');
    setInvTypeCode('');
    setInvDate('');
    setReplaceInvNum([]);
    setAdjustInvNum([]);
    setSubmittedByCode('');
    setSubmittingClientNumber('');
    setSubmittingClientLocation(isExisting ? '' : '00');
    setOtherClientNumber('');
    setOtherClientLocation(isExisting ? '' : '00');
    setOtherClientName('');
    setSubmittingClientCity('');
    setSubmittingClientProvState('');
    setOtherClientCity('');
    setOtherClientProvState('');
    setSubmittingClient(null);
    setOtherClient(null);
    setMaturityCode(isExisting ? '' : 'O');
    setFobCodeValue('');
    setPrimarySortCodeValue('');
    setBoomNumbers([]);
    setTimberMarks([]);
    setWeighSlips([]);
    setClientPrimarySortCode('');
    setReviewerComment('');
    setSubmitterComment('');
    setLineItems([]);
    setNewLineSecondarySort(null);
    setNewLineSpecies(null);
    setNewLineClientSort('');
    setNewLinePieces('');
    setNewLineGrade(null);
    setNewLineVolume('');
    setNewLinePrice('');
    setFieldErrors({});
    setPageErrors([]);
    setWarnings([]);
    setReviewerCommentError('');
    setFobValidationError('');
    setNewLineFieldErrors({});
    setEditLineDraft(null);
    setEditLineFieldErrors({});
    setEditGroupDraft(null);
    setEditGroupFieldErrors({});
    setExpandedGroupIds(new Set());
    hasAutoExpandedGroupsRef.current = false;
    setInitialLoadComplete(false);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [invoiceId]);

  // -----------------------------------------------------------------
  // Hydrate form state from a freshly-loaded / mutation-returned invoice.
  // Triggered whenever `loadedInvoice` (from the query) changes.
  // -----------------------------------------------------------------
  useEffect(() => {
    if (!loadedInvoice) return;
    setInvNumber(loadedInvoice.invNumber ?? '');
    setInvTypeCode(loadedInvoice.invType ?? '');
    setInvDate(loadedInvoice.invoiceDate ?? '');
    setReplaceInvNum(
      loadedInvoice.replaceInvNum
        ?.split(',')
        .map((s) => s.trim())
        .filter(Boolean) ?? [],
    );
    setAdjustInvNum(
      loadedInvoice.adjustInvNum
        ?.split(',')
        .map((s) => s.trim())
        .filter(Boolean) ?? [],
    );
    setSubmittedByCode(loadedInvoice.submittedBy ?? '');
    setSubmittingClientNumber(loadedInvoice.submitterClientNum ?? '');
    setSubmittingClientLocation(loadedInvoice.submitterLocation ?? '');
    setOtherClientNumber(loadedInvoice.otherClientNum ?? '');
    setOtherClientLocation(loadedInvoice.otherClientLocation ?? '');
    setOtherClientName(loadedInvoice.otherClientName ?? '');
    setOtherClientCity(loadedInvoice.otherClientCity ?? '');
    setOtherClientProvState(loadedInvoice.otherClientProvState ?? '');
    setMaturityCode(loadedInvoice.maturity ?? '');
    setFobCodeValue(loadedInvoice.fobCode ?? '');
    setPrimarySortCodeValue(loadedInvoice.primarySortCode ?? '');
    // The backend stores the same value in both `log_sale_sort_code` and
    // `client_primary_sort_code` columns but only returns the former on the
    // DTO. Mirror it into the Client-primary-sort TextArea so the field
    // doesn't appear blank on a loaded invoice.
    setClientPrimarySortCode(loadedInvoice.primarySortCode ?? '');
    setBoomNumbers(loadedInvoice.boomNumbers ?? []);
    setTimberMarks(loadedInvoice.timberMarks ?? []);
    setWeighSlips(loadedInvoice.weightSlips ?? []);
    setReviewerComment(loadedInvoice.reviewComments ?? '');
    setSubmitterComment(loadedInvoice.submitComments ?? '');
    const loadedLines = loadedInvoice.lineItems ?? [];
    setLineItems(loadedLines);
    // On first load of this invoice, expand every line-item group. Guarded by a
    // ref so a later refetch (e.g. after a mutation) doesn't re-expand groups the
    // user has since collapsed. Group ids are independent of the sort-code
    // lookup, so this lines up with `groupRows` even before that lookup resolves.
    if (!hasAutoExpandedGroupsRef.current) {
      setExpandedGroupIds(new Set(groupLineItems(loadedLines, sortCodeItems).map((g) => g.id)));
      hasAutoExpandedGroupsRef.current = true;
    }
    setWarnings(loadedInvoice.warnings ?? []);
    // The GET response also carries any `ERROR`-type messages from running
    // the validator on the loaded record. Route them through the same
    // field/page splitter that mutation 400s use so the user sees them
    // inline on the field (when mapped) or in the page-level banner.
    applyServerErrors(loadedInvoice.errors ?? []);
  }, [loadedInvoice]);

  // -----------------------------------------------------------------
  // Resolve the full ClientLocationResponse for each client number on the
  // loaded invoice. The skeleton view in the JSX waits for both of these
  // (when applicable) to land before showing the real form, so the
  // autocomplete fields never flash empty before their autofill arrives.
  // -----------------------------------------------------------------
  useEffect(() => {
    if (!loadedInvoice) return;
    let cancelled = false;
    const pickMatch = (results: ClientLocationResponse[], locn: string | null | undefined) =>
      (locn && results.find((r) => r.clientLocnCode === locn)) || results[0] || null;
    if (loadedInvoice.submitterClientNum) {
      getClientsByNumber(loadedInvoice.submitterClientNum)
        .then((results) => {
          if (cancelled) return;
          const match = pickMatch(results, loadedInvoice.submitterLocation);
          if (match) handleSubmittingClientSelect(match);
        })
        .catch(() => {
          /* swallow — combo box stays empty if lookup fails */
        });
    }
    if (loadedInvoice.otherClientNum) {
      getClientsByNumber(loadedInvoice.otherClientNum)
        .then((results) => {
          if (cancelled) return;
          const match = pickMatch(results, loadedInvoice.otherClientLocation);
          if (match) handleOtherClientSelect(match);
        })
        .catch(() => {});
    }
    return () => {
      cancelled = true;
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [loadedInvoice]);

  // Convenience flags
  const currentStatus = loadedInvoice?.invStatus ?? (isExisting ? '' : 'DFT');
  const submissionNumber = loadedInvoice?.submissionNumber;

  // Permission flags
  const canSavePerm = usePermission(INVOICE_DETAILS_SAVE);
  const canSubmitPerm = usePermission(INVOICE_DETAILS_SUBMIT);
  const canApprovePerm = usePermission(INVOICE_DETAILS_APPROVE);
  const canDuplicatePerm = usePermission(INVOICE_DETAILS_DUPLICATE);
  const canCancelPerm = usePermission(INVOICE_DETAILS_CANCEL);
  const canRejectPerm = usePermission(INVOICE_DETAILS_REJECT);
  const canDeletePerm = usePermission(INVOICE_DETAILS_DELETE);

  // Brand-new invoice (no id) is editable; existing invoice is editable
  // only after it has loaded AND has an editable status (DFT).
  const canEdit = isExisting ? !!loadedInvoice && EDITABLE_STATUSES.has(currentStatus) : true;
  const canChangeStatus = STATUS_CHANGEABLE.has(currentStatus);
  // Unapprove reverts an APPROVED invoice back to UNAPPROVED — only offered in APP.
  const canUnapprove = isExisting && currentStatus === 'APP';
  // Submit is offered only on a saved invoice in DFT/UNA — a brand-new
  // (unsaved) invoice must be saved first, so NEW shows Save only.
  const canSubmit = isExisting && SUBMITTABLE_STATUSES.has(currentStatus);
  // Duplicate operates on a saved invoice and is offered only in DFT/PRO/UNA.
  const canDuplicate = isExisting && DUPLICABLE_STATUSES.has(currentStatus);
  // Delete is offered on a saved invoice in DFT/PRO only (not UNA).
  const canDelete = isExisting && DELETABLE_STATUSES.has(currentStatus);
  // Adding a line item needs the save permission (a viewer can't) plus a saved,
  // non-APP invoice.
  const canAddLineItem = canSavePerm && isExisting && !!loadedInvoice && currentStatus !== 'APP';
  // Group-level edit/delete is allowed in DFT/APP/UNA — and needs save permission
  // (this also gates the per-row edit/delete via `canEditLineRows`).
  const canEditLineItems =
    canSavePerm && isExisting && !!loadedInvoice && LINE_ITEM_EDITABLE_STATUSES.has(currentStatus);
  // Individual line-item rows are editable/deletable in the same statuses EXCEPT
  // APP — in APP only the group as a whole can be edited/deleted.
  const canEditLineRows = canEditLineItems && currentStatus !== 'APP';
  const hasLineItems = lineItems.length > 0;

  // ── Inline button loading ────────────────────────────────────────────────
  // Any in-flight update locks every action button on the page; the button
  // that triggered the work swaps its label for an inline spinner. Approve /
  // Unapprove / Cancel / Reject all share `changeStatusMutation`, so we read
  // the in-flight target status to know which one to spin.
  const statusInFlight = changeStatusMutation.isPending ? changeStatusMutation.variables?.body?.status : undefined;
  const anyMutationPending =
    createMutation.isPending ||
    updateMutation.isPending ||
    submitMutation.isPending ||
    duplicateMutation.isPending ||
    deleteMutation.isPending ||
    changeStatusMutation.isPending ||
    addLineItemMutation.isPending ||
    deleteLineItemMutation.isPending ||
    updateLineItemMutation.isPending;
  const savePending = createMutation.isPending || updateMutation.isPending;
  const approvePending = statusInFlight === 'APP';
  const unapprovePending = statusInFlight === 'UNA';
  const cancelPending = statusInFlight === 'CAN';
  const rejectPending = statusInFlight === 'REJ';
  // Keep the button's label and, while its own action is running, show a small
  // inline spinner on the right (in place of / alongside the button's icon).
  const actionLabel = (loading: boolean, label: string): ReactNode => (
    <>
      {label}
      {loading ? <Loading small withOverlay={false} description={label} className="invoice-page__btn-spinner" /> : null}
    </>
  );

  // Derived totals (from current line items, which mirror what would be saved)
  const totalPieces = lineItems.reduce((s, l) => s + (l.numOfPieces ?? 0), 0);
  const totalVolume = lineItems.reduce((s, l) => s + Number(l.volume ?? 0), 0);
  const totalAmount = lineItems.reduce((s, l) => s + Number(l.amount ?? 0), 0);

  // Read-only meta values (not yet returned from any endpoint). For a brand-new
  // invoice created from scratch there's no stored received date, so default the
  // "Date entered/received" to today (local yyyy-MM-dd).
  const dateInvoiceReceived = isExisting ? (loadedInvoice?.invoiceDate ?? '—') : formatIsoDate(new Date());
  const enteredSubmittedBy = loadedInvoice?.entryUserID ?? '—';

  // ------ Add New Line Item validity ------
  const isAddLineItemValid =
    newLineSecondarySort !== null &&
    newLineSpecies !== null &&
    newLinePieces.trim() !== '' &&
    newLineGrade !== null &&
    Object.keys(clientNewLineErrors).length === 0;

  const handleSaveNewLineItem = () => {
    if (!isAddLineItemValid) return;
    if (!invoiceId) {
      addNotification({ kind: 'warning', title: 'Save the invoice before adding line items.' });
      return;
    }
    const body: LineItemRequest = {
      secondSort: newLineSecondarySort?.code ?? null,
      clientSecondarySort: newLineClientSort || null,
      species: newLineSpecies?.code ?? null,
      grade: newLineGrade?.code ?? null,
      numOfPieces: Number(newLinePieces) || 0,
      price: Number(newLinePrice) || 0,
      volume: Number(newLineVolume) || 0,
      convertedPrice: null,
    };
    clearErrors();
    addLineItemMutation.mutate(
      { invoiceId, body },
      {
        onSuccess: (data) => {
          // Hydrate from the server response so the table + totals reflect
          // the freshly-validated server state.
          const nextLines = data.lineItems ?? [];
          // Expand the group the newly-added line lands in (often a brand-new
          // group). The added line(s) are whichever ids weren't present before.
          const prevIds = new Set(lineItems.map((l) => l.lineItemID));
          const addedIds = nextLines.map((l) => l.lineItemID).filter((id) => !prevIds.has(id));
          setLineItems(nextLines);
          keepGroupsExpandedForLines(nextLines, addedIds);
          setWarnings(data.warnings ?? []);
          applyServerErrors(data.errors ?? []);
          setNewLineFieldErrors({});
          handleClearNewLineItem();
          setAddLineItemOpen(false);
          addNotification({ kind: 'success', title: 'Line item added.' });
        },
        onError: (err) => {
          // Use the line-item-specific mapping so errors light up the Add
          // form's fields instead of the generic invoice header inputs.
          const errors = extractValidationErrors(err);
          if (errors.length > 0) {
            applyLineItemServerErrors(errors);
          } else {
            addNotification({
              kind: 'error',
              title: 'Failed to add line item.',
              subtitle: extractApiErrorMessage(err),
            });
          }
        },
      },
    );
  };

  const handleClearNewLineItem = () => {
    setNewLineSecondarySort(null);
    setNewLineSpecies(null);
    setNewLineClientSort('');
    setNewLinePieces('');
    setNewLineGrade(null);
    setNewLineVolume('');
    setNewLinePrice('');
    setNewLineFieldErrors({});
  };

  const handleSubmittingClientSelect = (client: ClientLocationResponse | null) => {
    setSubmittingClient(client);
    setSubmittingClientNumber(client?.clientNumber ?? '');
    setSubmittingClientLocation(client?.clientLocnCode ?? '');
    setSubmittingClientCity(client?.city ?? '');
    setSubmittingClientProvState(client?.province ?? '');
  };

  const handleOtherClientSelect = (client: ClientLocationResponse | null) => {
    setOtherClient(client);
    setOtherClientNumber(client?.clientNumber ?? '');
    setOtherClientLocation(client?.clientLocnCode ?? '');
    setOtherClientName(client?.clientName ?? '');
    setOtherClientCity(client?.city ?? '');
    setOtherClientProvState(client?.province ?? '');
  };

  // ---------------------------------------------------------------
  // Error / warning handling
  // ---------------------------------------------------------------
  // Split validator messages into field-level inline errors (keys present in
  // `fieldMap`) and page-level banner errors (everything else), then push each
  // set to state. Shared by the three handlers below, which differ only in the
  // field map consulted and the inline-error setter targeted.
  const routeServerErrors = (
    errors: ValidationMessageResponse[],
    fieldMap: Record<string, string>,
    setInline: (errors: Record<string, string>) => void,
  ) => {
    const inline: Record<string, string> = {};
    const page: ValidationMessageResponse[] = [];
    errors.forEach((err) => {
      const field = fieldMap[err.messageKey];
      // Store the resolved message text on field-mapped errors.
      if (field) inline[field] = err.message || err.messageKey;
      else page.push(err);
    });
    setInline(inline);
    setPageErrors(page);
  };

  const applyServerErrors = (errors: ValidationMessageResponse[]) =>
    routeServerErrors(errors, MESSAGE_KEY_TO_FIELD, setFieldErrors);

  // Variant for the POST/PATCH /line-items mutations — routes mapped keys to
  // the Add New Line Item form's inline state and unmapped keys to the banner.
  const applyLineItemServerErrors = (errors: ValidationMessageResponse[]) =>
    routeServerErrors(errors, LINE_ITEM_MESSAGE_KEY_TO_FIELD, setNewLineFieldErrors);

  // Same mapping logic but routed into the inline-row-edit error state.
  const applyEditLineItemServerErrors = (errors: ValidationMessageResponse[]) =>
    routeServerErrors(errors, LINE_ITEM_MESSAGE_KEY_TO_FIELD, setEditLineFieldErrors);

  const clearErrors = () => {
    setFieldErrors({});
    setPageErrors([]);
    setReviewerCommentError('');
    setNewLineFieldErrors({});
    setEditLineFieldErrors({});
    setEditGroupFieldErrors({});
  };

  // -----------------------------------------------------------------
  // Auto-clear a server (business-rule) field error as soon as the user edits
  // that field — the stale error shouldn't linger after a new value is typed.
  // The current value of every server-error-capable header field, keyed by the
  // same name used in `fieldErrors`.
  const serverErrorFieldValues: Record<string, string> = {
    invNumber,
    invType: invTypeCode,
    invDate,
    replaceInvNum: replaceInvNum.join(','),
    adjustInvNum: adjustInvNum.join(','),
    submittedBy: submittedByCode,
    submittingClientNumber,
    submittingClientLocation,
    otherClientLocation,
    otherClientName,
    maturity: maturityCode,
    fobCode: fobCodeValue,
    primarySortCode: primarySortCodeValue,
    reviewerComment,
    boomNumbers: boomNumbers.join(''),
    timberMarks: timberMarks.join(''),
    weighSlips: weighSlips.join(''),
  };
  const prevFieldValuesRef = useRef(serverErrorFieldValues);
  const prevFieldErrorsRef = useRef(fieldErrors);
  useEffect(() => {
    // When the error set itself changed (errors were (re)applied, e.g. on load
    // or save, or cleared) just re-baseline the values — don't treat it as an
    // edit, so freshly-applied errors aren't immediately wiped.
    if (fieldErrors !== prevFieldErrorsRef.current) {
      prevFieldErrorsRef.current = fieldErrors;
      prevFieldValuesRef.current = serverErrorFieldValues;
      return;
    }
    // Otherwise a tracked field changed value — drop the server error for each
    // field whose value now differs from the baseline.
    const changed = Object.keys(serverErrorFieldValues).filter(
      (k) => serverErrorFieldValues[k] !== prevFieldValuesRef.current[k],
    );
    prevFieldValuesRef.current = serverErrorFieldValues;
    if (changed.length === 0) return;
    setFieldErrors((prev) => {
      let mutated = false;
      const next = { ...prev };
      for (const k of changed) {
        if (k in next) {
          delete next[k];
          mutated = true;
        }
      }
      return mutated ? next : prev;
    });
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [
    fieldErrors,
    invNumber,
    invTypeCode,
    invDate,
    replaceInvNum,
    adjustInvNum,
    submittedByCode,
    submittingClientNumber,
    submittingClientLocation,
    otherClientLocation,
    otherClientName,
    maturityCode,
    fobCodeValue,
    primarySortCodeValue,
    reviewerComment,
    boomNumbers,
    timberMarks,
    weighSlips,
  ]);

  const handleMutationError = (err: unknown, fallback: string) => {
    const errors = extractValidationErrors(err);
    // Keep the detailed validation errors in the top banner...
    if (errors.length > 0) {
      applyServerErrors(errors);
    }
    // ...and always flag the failure as a bottom toast too. For validation
    // failures the specifics live in the top banner, so the toast is just the
    // summary; for non-validation errors it carries the server message.
    addNotification({
      kind: 'error',
      title: fallback,
      subtitle: errors.length > 0 ? undefined : extractApiErrorMessage(err),
    });
  };

  // ---------------------------------------------------------------
  // Build the create/update request body from current form state.
  // `clientNumber` / `clientLocation` are derived from `submittedBy`:
  // when the submitter IS the seller, they're the same as submitter;
  // when the submitter is the buyer, they come from the other-party.
  // ---------------------------------------------------------------
  const buildRequestBody = (): CreateInvoiceRequest => {
    const isSeller = submittedByCode === 'Seller';
    const lineItemPayload: LineItemRequest[] = lineItems.map((l) => ({
      lineItemID: l.lineItemID && l.lineItemID > 0 ? l.lineItemID : null,
      secondSort: l.secondSort,
      clientSecondarySort: l.clientSecondarySort ?? null,
      species: l.species,
      grade: l.grade,
      // Coerce null → 0: a loaded line can carry a null pieces/price/volume
      // (the backend reads them as BigDecimal/Integer, which can be null),
      // and sending null fails server validation. Empty means zero here.
      numOfPieces: l.numOfPieces ?? 0,
      price: l.price ?? 0,
      volume: l.volume ?? 0,
      convertedPrice: l.convertedPrice ?? null,
    }));
    return {
      invNumber,
      invoiceDate: invDate,
      invType: invTypeCode,
      maturity: maturityCode || null,
      fobCode: fobCodeValue || null,
      primarySortCode: primarySortCodeValue || null,
      totalAmt: totalAmount,
      totalPieces,
      totalVol: totalVolume,
      submitterClientNum: submittingClientNumber,
      submitterLocation: submittingClientLocation,
      submittedBy: (submittedByCode || 'Seller') as 'Buyer' | 'Seller',
      clientNumber: isSeller ? submittingClientNumber : otherClientNumber,
      clientLocation: isSeller ? submittingClientLocation : otherClientLocation,
      otherClientNum: otherClientNumber || null,
      otherClientLocation: otherClientLocation || null,
      otherClientName: otherClientNumber ? null : otherClientName || null,
      otherClientCity: otherClientNumber ? null : otherClientCity || null,
      otherClientProvState: otherClientNumber ? null : otherClientProvState || null,
      boomNumbers,
      timberMarks,
      weightSlips: weighSlips,
      replaceInvNum: replaceInvNum.length ? replaceInvNum.join(',') : null,
      adjustInvNum: adjustInvNum.length ? adjustInvNum.join(',') : null,
      reviewComments: reviewerComment || null,
      submitComments: submitterComment || null,
      // A manual invoice has no business submission number; an ESF invoice does.
      // (submissionId is the surrogate join key and is always present once saved.)
      manual: submissionNumber == null,
      lineItems: lineItemPayload,
    };
  };

  const groupRows = useMemo(() => groupLineItems(lineItems, sortCodeItems), [lineItems, sortCodeItems]);

  // After an edit that replaces the line items, re-map group expansion onto the
  // new group ids.
  const keepGroupsExpandedForLines = (nextLines: LineItemResponse[], affectedLineIds: number[]) => {
    const nextGroups = groupLineItems(nextLines, sortCodeItems);
    const validIds = new Set(nextGroups.map((g) => g.id));
    const affected = new Set(affectedLineIds);
    setExpandedGroupIds((prev) => {
      const next = new Set<string>();
      prev.forEach((id) => {
        if (validIds.has(id)) next.add(id);
      });
      nextGroups.forEach((g) => {
        if (g.lineItems.some((l) => affected.has(l.lineItemID))) next.add(g.id);
      });
      return next;
    });
  };

  // Group-row action handlers. Wrapped in useCallback and declared above
  // `groupColumns` so the memo can list them as dependencies: the memoised
  // columns reference stays stable (these refs never change), and if either
  // handler later starts reading component state/props, its useCallback deps
  // will force a new identity so the renderCell closures can't go stale.
  // Both take the GroupRow directly (passed from renderCell at click time) so
  // they never read from the closed-over `groupRows`.
  const handleStartGroupEdit = useCallback((group: GroupRow) => {
    setEditGroupDraft({
      groupId: group.id,
      lineItemIds: group.lineItems.map((l) => l.lineItemID).filter((id) => id > 0),
      originalSecondSort: group.secondarySort,
      originalSpecies: group.species,
      secondSort: group.secondarySort,
      species: group.species,
    });
    setEditGroupFieldErrors({});
  }, []);

  // Deleting a group removes each of its line items
  const performGroupDelete = async (group: GroupRow) => {
    if (!invoiceId) return;
    clearErrors();
    const ids = group.lineItems.map((l) => l.lineItemID).filter((id) => id > 0);
    let lastResponse: typeof loadedInvoice = undefined;
    for (const lineId of ids) {
      try {
        // eslint-disable-next-line no-await-in-loop
        lastResponse = await deleteLineItemMutation.mutateAsync({ invoiceId, lineId });
      } catch (err) {
        setDeleteConfirm(null);
        handleMutationError(err, 'Failed to delete group.');
        return;
      }
    }
    if (lastResponse) {
      setLineItems(lastResponse.lineItems ?? []);
      setWarnings(lastResponse.warnings ?? []);
      applyServerErrors(lastResponse.errors ?? []);
    }
    setDeleteConfirm(null);
    addNotification({ kind: 'success', title: 'Group deleted.' });
  };
  const performGroupDeleteRef = useRef(performGroupDelete);
  performGroupDeleteRef.current = performGroupDelete;

  const requestDeleteGroup = useCallback(
    (group: GroupRow) =>
      setDeleteConfirm({
        title: 'Delete group',
        message: 'Are you sure you want to delete all line items in this group?',
        onConfirm: () => performGroupDeleteRef.current(group),
      }),
    [],
  );

  const groupColumns = useMemo<ResultsTableColumn<GroupRow>[]>(
    () => [
      { key: 'groupNumber', header: 'Group number' },
      {
        key: 'secondarySort',
        header: 'Secondary sort',
        renderCell: (r) => <span style={{ display: 'block', textAlign: 'center' }}>{r.secondarySort}</span>,
      },
      { key: 'description', header: 'Description' },
      {
        key: 'species',
        header: 'Species',
        headerAlign: 'center',
        renderCell: (r) => <span style={{ display: 'block', textAlign: 'center' }}>{r.species}</span>,
      },
      {
        key: 'totalPieces',
        header: 'Total pieces',
        headerAlign: 'center',
        renderCell: (r) => <span style={{ display: 'block', textAlign: 'center' }}>{formatNumber(r.totalPieces)}</span>,
      },
      {
        key: 'totalVolume',
        header: 'Total volume',
        headerAlign: 'center',
        renderCell: (r) => (
          <span style={{ display: 'block', textAlign: 'center' }}>{formatNumber(r.totalVolume, 3)}</span>
        ),
      },
      {
        key: 'totalAmount',
        header: 'Total $ amount',
        headerAlign: 'center',
        renderCell: (r) => (
          <span style={{ display: 'block', textAlign: 'center' }}>{formatCurrency(r.totalAmount)}</span>
        ),
      },
      {
        key: 'priceConversion',
        header: 'Price conversion',
        headerAlign: 'center',
        renderCell: (r) => <span style={{ display: 'block', textAlign: 'center' }}>{r.priceConversion}</span>,
      },
      {
        key: 'id',
        header: 'Actions',
        // Group edit/delete are line-item edits/deletes — only offered when the
        // status allows them (DFT or APP).
        renderCell: (r) =>
          canEditLineItems ? (
            <div className="invoice-page__group-actions">
              <IconButton
                kind="ghost"
                size="sm"
                label="Edit group"
                align="top"
                autoAlign
                onClick={() => handleStartGroupEdit(r)}
              >
                <Edit />
              </IconButton>
              <IconButton
                kind="ghost"
                size="sm"
                label="Delete group"
                align="top"
                autoAlign
                onClick={() => requestDeleteGroup(r)}
              >
                <TrashCan />
              </IconButton>
            </div>
          ) : null,
      },
    ],
    [handleStartGroupEdit, requestDeleteGroup, canEditLineItems],
  );

  const groupTotalsRow = useMemo(() => {
    const totalPieces = groupRows.reduce((s, r) => s + r.totalPieces, 0);
    const totalVolume = groupRows.reduce((s, r) => s + r.totalVolume, 0);
    const totalAmount = groupRows.reduce((s, r) => s + r.totalAmount, 0);
    return (
      <TableRow className="invoice-page__line-items-totals-row">
        <TableCell colSpan={4} />
        <TableCell>
          <strong style={{ display: 'block', textAlign: 'center' }}>Invoice totals</strong>
        </TableCell>
        <TableCell>
          <strong style={{ display: 'block', textAlign: 'center' }}>{formatNumber(totalPieces)}</strong>
        </TableCell>
        <TableCell>
          <strong style={{ display: 'block', textAlign: 'center' }}>{formatNumber(totalVolume, 3)}</strong>
        </TableCell>
        <TableCell>
          <strong style={{ display: 'block', textAlign: 'center' }}>{formatCurrency(totalAmount)}</strong>
        </TableCell>
        <TableCell />
        <TableCell />
      </TableRow>
    );
  }, [groupRows]);

  // -----------------------------------------------------------------
  // Action handlers — each runs the matching mutation and surfaces the
  // result via the notification context + per-field error mapping.
  // -----------------------------------------------------------------
  const handleSave = () => {
    clearErrors();
    const body = buildRequestBody();
    if (isExisting && invoiceId) {
      updateMutation.mutate(
        { id: invoiceId, body },
        {
          onSuccess: (data) => {
            setWarnings(data.warnings ?? []);
            addNotification({ kind: 'success', title: `Invoice '${data.invNumber}' saved.` });
          },
          onError: (err) => handleMutationError(err, 'Failed to save invoice.'),
        },
      );
    } else {
      createMutation.mutate(body, {
        onSuccess: (data) => {
          setWarnings(data.warnings ?? []);
          addNotification({ kind: 'success', title: `Invoice '${data.invNumber}' created.` });
          // Switch the URL to edit mode so subsequent saves PUT instead of POST.
          navigate(`/invoice/${data.invID}`, { replace: true, state: location.state });
        },
        onError: (err) => handleMutationError(err, 'Failed to create invoice.'),
      });
    }
  };

  const handleSubmit = () => {
    if (!invoiceId) {
      addNotification({ kind: 'warning', title: 'Save the invoice before submitting.' });
      return;
    }
    clearErrors();
    submitMutation.mutate(invoiceId, {
      onSuccess: (data) => {
        setWarnings(data.warnings ?? []);
        addNotification({ kind: 'success', title: `Invoice '${data.invNumber}' submitted.` });
      },
      onError: (err) => handleMutationError(err, 'Failed to submit invoice.'),
    });
  };

  const runStatusChange = (status: 'APP' | 'REJ' | 'CAN' | 'UNA', requiresComment: boolean) => {
    if (!invoiceId) return;
    if (requiresComment && reviewerComment.trim() === '') {
      setReviewerCommentError('Reviewer comment is required for this action.');
      return;
    }
    setReviewerCommentError('');
    clearErrors();
    changeStatusMutation.mutate(
      { id: invoiceId, body: { status, reviewComments: reviewerComment || null } },
      {
        onSuccess: (data) => {
          setWarnings(data.warnings ?? []);
          const verb =
            status === 'APP'
              ? 'approved'
              : status === 'REJ'
                ? 'rejected'
                : status === 'CAN'
                  ? 'cancelled'
                  : 'unapproved';
          addNotification({ kind: 'success', title: `Invoice '${data.invNumber}' ${verb}.` });
        },
        onError: (err) => handleMutationError(err, 'Failed to change invoice status.'),
      },
    );
  };

  const handleApprove = () => runStatusChange('APP', false);
  const handleReject = () => runStatusChange('REJ', true);
  const handleCancel = () => runStatusChange('CAN', true);
  const handleUnapprove = () => runStatusChange('UNA', true);

  const handleDuplicate = () => {
    if (!invoiceId) {
      addNotification({ kind: 'warning', title: 'Save the invoice before duplicating.' });
      return;
    }
    duplicateMutation.mutate(invoiceId, {
      onSuccess: (data) => {
        addNotification({ kind: 'success', title: `Invoice '${data.invNumber}' duplicated.` });
        navigate(`/invoice/${data.invID}`, { replace: false });
      },
      onError: (err) => handleMutationError(err, 'Failed to duplicate invoice.'),
    });
  };

  const handleDelete = () => {
    if (!invoiceId) return;
    deleteMutation.mutate(invoiceId, {
      onSuccess: () => {
        // Navigate to the base "new invoice" screen.
        setDeleteConfirm(null);
        addNotification({ kind: 'success', title: 'Invoice deleted.' });
        navigate('/invoice', { replace: true });
      },
      onError: (err) => {
        setDeleteConfirm(null);
        handleMutationError(err, 'Failed to delete invoice.');
      },
    });
  };

  const requestDeleteInvoice = () =>
    setDeleteConfirm({
      title: 'Delete invoice',
      message: 'Are you sure you want to delete this invoice? This action cannot be undone.',
      onConfirm: handleDelete,
    });

  const requestDeleteLineItem = (groupId: string, lineId: string) =>
    setDeleteConfirm({
      title: 'Delete line item',
      message: 'Are you sure you want to delete this line item?',
      onConfirm: () => handleDeleteLineItem(groupId, lineId),
    });

  const handleDeleteLineItem = (_groupId: string, lineId: string) => {
    if (!invoiceId) return;
    const numeric = Number(lineId);
    if (Number.isNaN(numeric)) return;
    clearErrors();
    deleteLineItemMutation.mutate(
      { invoiceId, lineId: numeric },
      {
        onSuccess: (data) => {
          setLineItems(data.lineItems ?? []);
          setWarnings(data.warnings ?? []);
          applyServerErrors(data.errors ?? []);
          setDeleteConfirm(null);
          addNotification({ kind: 'success', title: 'Line item deleted.' });
        },
        onError: (err) => {
          setDeleteConfirm(null);
          handleMutationError(err, 'Failed to delete line item.');
        },
      },
    );
  };

  // ---------------------------------------------------------------
  // Inline single-row edit
  // ---------------------------------------------------------------
  const handleStartLineEdit = (lineId: string) => {
    const line = lineItems.find((l) => String(l.lineItemID) === lineId);
    if (!line) return;
    setEditLineDraft({
      id: String(line.lineItemID),
      secondSort: line.secondSort ?? '',
      species: line.species ?? '',
      grade: line.grade ?? '',
      numOfPieces: String(line.numOfPieces ?? ''),
      volume: String(line.volume ?? ''),
      price: String(line.price ?? ''),
      clientSecondarySort: line.clientSecondarySort ?? '',
    });
    setEditLineFieldErrors({});
  };

  const handleCancelLineEdit = () => {
    setEditLineDraft(null);
    setEditLineFieldErrors({});
  };

  const handleSaveLineEdit = () => {
    if (!invoiceId || !editLineDraft) return;
    const lineItemID = Number(editLineDraft.id);
    const body: LineItemRequest = {
      lineItemID,
      secondSort: editLineDraft.secondSort || null,
      clientSecondarySort: editLineDraft.clientSecondarySort || null,
      species: editLineDraft.species || null,
      grade: editLineDraft.grade || null,
      numOfPieces: Number(editLineDraft.numOfPieces) || 0,
      price: Number(editLineDraft.price) || 0,
      volume: Number(editLineDraft.volume) || 0,
      convertedPrice: null,
    };
    clearErrors();
    updateLineItemMutation.mutate(
      { invoiceId, lineId: lineItemID, body },
      {
        onSuccess: (data) => {
          const nextLines = data.lineItems ?? [];
          setLineItems(nextLines);
          keepGroupsExpandedForLines(nextLines, [lineItemID]);
          setWarnings(data.warnings ?? []);
          applyServerErrors(data.errors ?? []);
          setEditLineDraft(null);
          setEditLineFieldErrors({});
          addNotification({ kind: 'success', title: 'Line item updated.' });
        },
        onError: (err) => {
          const errors = extractValidationErrors(err);
          if (errors.length > 0) {
            applyEditLineItemServerErrors(errors);
          } else {
            addNotification({
              kind: 'error',
              title: 'Failed to update line item.',
              subtitle: extractApiErrorMessage(err),
            });
          }
        },
      },
    );
  };

  // ---------------------------------------------------------------
  // Group edit modal
  // ---------------------------------------------------------------
  const handleCancelGroupEdit = () => {
    setEditGroupDraft(null);
    setEditGroupFieldErrors({});
  };

  const handleSaveGroupEdit = async () => {
    if (!invoiceId || !editGroupDraft) return;
    const { lineItemIds, secondSort, species } = editGroupDraft;
    if (!secondSort || !species) {
      setEditGroupFieldErrors({
        ...(secondSort ? {} : { secondSort: 'Secondary sort is required.' }),
        ...(species ? {} : { species: 'Species is required.' }),
      });
      return;
    }
    clearErrors();
    let lastResponse: typeof loadedInvoice = undefined;
    for (const lineId of lineItemIds) {
      const sourceLines = lastResponse?.lineItems ?? lineItems;
      const existing = sourceLines.find((l) => l.lineItemID === lineId);
      if (!existing) continue;
      const body: LineItemRequest = {
        lineItemID: lineId,
        secondSort,
        clientSecondarySort: existing.clientSecondarySort ?? null,
        species,
        grade: existing.grade,
        numOfPieces: existing.numOfPieces,
        price: existing.price,
        volume: existing.volume,
        convertedPrice: existing.convertedPrice ?? null,
      };
      try {
        // eslint-disable-next-line no-await-in-loop
        lastResponse = await updateLineItemMutation.mutateAsync({ invoiceId, lineId, body });
      } catch (err) {
        const errors = extractValidationErrors(err);
        if (errors.length > 0) {
          const inline: Record<string, string> = {};
          errors.forEach((e) => {
            const field = LINE_ITEM_MESSAGE_KEY_TO_FIELD[e.messageKey];
            if (field === 'secondarySort') inline.secondSort = e.message || e.messageKey;
            else if (field === 'grade' || field === 'pieces' || field === 'volume' || field === 'price') {
              // Cross-field rules between the new species and the lines'
              // existing data — surface against the species dropdown.
              inline.species = e.message || e.messageKey;
            }
          });
          setEditGroupFieldErrors(inline);
        } else {
          addNotification({
            kind: 'error',
            title: 'Failed to update group.',
            subtitle: extractApiErrorMessage(err),
          });
        }
        return; // bail out of the loop on the first failure
      }
    }
    if (lastResponse) {
      const nextLines = lastResponse.lineItems ?? [];
      setLineItems(nextLines);
      // Keep the edited group open under its new (sort/species-derived) id.
      keepGroupsExpandedForLines(nextLines, lineItemIds);
      setWarnings(lastResponse.warnings ?? []);
      applyServerErrors(lastResponse.errors ?? []);
    }
    setEditGroupDraft(null);
    setEditGroupFieldErrors({});
    addNotification({ kind: 'success', title: 'Group updated.' });
  };

  const handleExport = (format: 'csv' | 'pdf') => {
    exportMutation.mutate(
      {
        format,
        request: {
          invoiceNumber: invNumber || null,
          rows: groupRows.map((r) => ({
            groupNumber: r.groupNumber,
            secondarySort: r.secondarySort,
            description: r.description,
            species: r.species,
            totalPieces: r.totalPieces,
            totalVolume: r.totalVolume,
            totalAmount: r.totalAmount,
            priceConversion: r.priceConversion,
            lineItems: r.lineItems.map((l) => ({
              secondarySort: l.secondSort,
              species: l.species,
              clientSecondarySort: l.clientSecondarySort ?? '',
              numberPieces: l.numOfPieces,
              grade: l.grade,
              volume: l.volume,
              price: l.price,
              amount: l.amount,
            })),
          })),
        },
      },
      {
        onSuccess: ({ blob, filename }) => downloadBlob(blob, filename),
        onError: () =>
          addNotification({ kind: 'error', title: `Failed to export the table as ${format.toUpperCase()}.` }),
      },
    );
  };

  const handleExportCsv = () => handleExport('csv');
  const handleExportPdf = () => handleExport('pdf');

  return (
    <div className="invoice-page">
      <Grid>
        <PageTitle title="Invoice" breadCrumbs={breadCrumbs}>
          <span className="invoice-page__status-tag">
            {/* Hide the status icon entirely while the page is loading. */}
            {isLoadingInvoice ? null : isExisting && loadedInvoice ? (
              <InvoiceStatusTag status={loadedInvoice.invStatus} />
            ) : (
              <InvoiceDetailsTag label="New" />
            )}
          </span>
        </PageTitle>

        {/* Page-level warning + error notifications — hidden until the page
            has finished loading. */}
        {!isLoadingInvoice && warnings.length > 0 ? (
          <Column sm={4} md={8} lg={16} className="invoice-page__notification-col">
            {warnings.map((w, i) => (
              <InlineNotification
                key={`warn-${i}-${w.messageKey}`}
                className="invoice-page__notification"
                kind="warning"
                title={relabel(w.message || w.messageKey)}
                lowContrast
                onClose={() => setWarnings((prev) => prev.filter((_w, idx) => idx !== i))}
              />
            ))}
          </Column>
        ) : null}
        {!isLoadingInvoice && pageErrors.length > 0 ? (
          <Column sm={4} md={8} lg={16} className="invoice-page__notification-col">
            {pageErrors.map((err, i) => (
              <InlineNotification
                key={`err-${i}-${err.messageKey}`}
                className="invoice-page__notification"
                kind="error"
                title="Validation error"
                subtitle={relabel(err.message || err.messageKey)}
                lowContrast
                onClose={() => setPageErrors((prev) => prev.filter((_e, idx) => idx !== i))}
              />
            ))}
          </Column>
        ) : null}

        <Column sm={4} md={8} lg={16}>
          {isLoadingInvoice ? (
            <InvoiceFormSkeleton />
          ) : (
            <Accordion className="invoice-page__accordion" align="end">
              {/* ------------------------------------------------- */}
              {/* Section 1 — Invoice details                       */}
              {/* ------------------------------------------------- */}
              <AccordionItem title="Invoice details" open>
                <Grid className="invoice-page__section-grid" condensed>
                  <Column sm={4} md={3} lg={5} className="invoice-page__meta-col">
                    <span className="invoice-page__meta-label">Date entered/received</span>
                    <span className="invoice-page__meta-value">{dateInvoiceReceived || '—'}</span>
                  </Column>
                  <Column sm={4} md={3} lg={6} className="invoice-page__meta-col">
                    <span className="invoice-page__meta-label">Entered/Submitted by</span>
                    <span className="invoice-page__meta-value">{enteredSubmittedBy || '—'}</span>
                  </Column>
                  <Column sm={4} md={2} lg={5} className="invoice-page__meta-col">
                    <span className="invoice-page__meta-label">Submission ID</span>
                    <span className="invoice-page__meta-value">{submissionNumber ?? '—'}</span>
                  </Column>

                  <Column sm={4} md={8} lg={16} className="invoice-page__field-col">
                    <TextInput
                      id="inv-number"
                      labelText={<RequiredLabel>Invoice number</RequiredLabel>}
                      value={invNumber}
                      onChange={(e) => setInvNumber(e.target.value)}
                      invalid={!!displayFieldErrors.invNumber}
                      invalidText={displayFieldErrors.invNumber}
                      disabled={!canEdit}
                    />
                  </Column>

                  <Column sm={4} md={8} lg={16} className="invoice-page__field-col">
                    <SingleSelect
                      id="inv-type"
                      titleText={<RequiredLabel>Invoice type</RequiredLabel>}
                      items={invoiceTypeItems}
                      itemToString={lookupDescription}
                      selectedItem={findByCode(invoiceTypeItems, invTypeCode)}
                      onChange={({ selectedItem }) => setInvTypeCode(selectedItem?.code ?? '')}
                      invalid={!!displayFieldErrors.invType}
                      invalidText={displayFieldErrors.invType}
                      disabled={!canEdit}
                    />
                  </Column>

                  <Column sm={4} md={8} lg={16} className="invoice-page__field-col">
                    <DateInput
                      id="inv-date"
                      labelText={<RequiredLabel>Invoice date</RequiredLabel>}
                      value={invDate || undefined}
                      onChange={(dates) => {
                        const d = dates?.[0];
                        setInvDate(d ? formatIsoDate(d) : '');
                      }}
                      invalid={!!displayFieldErrors.invDate}
                      invalidText={displayFieldErrors.invDate}
                      disabled={!canEdit}
                    />
                  </Column>

                  <Column sm={4} md={4} lg={8} className="invoice-page__field-col invoice-page__field-col--left">
                    <TagInput
                      id="replace-inv-num"
                      labelText="Replaces Invoice#(s)"
                      values={replaceInvNum}
                      onChange={setReplaceInvNum}
                      invalid={!!displayFieldErrors.replaceInvNum}
                      invalidText={displayFieldErrors.replaceInvNum}
                      disabled={!canEdit}
                      maxTags={5}
                    />
                  </Column>
                  <Column sm={4} md={4} lg={8} className="invoice-page__field-col">
                    <TagInput
                      id="adjust-inv-num"
                      labelText="Adjusts Invoice#(s)"
                      values={adjustInvNum}
                      onChange={setAdjustInvNum}
                      invalid={!!displayFieldErrors.adjustInvNum}
                      invalidText={displayFieldErrors.adjustInvNum}
                      disabled={!canEdit}
                      maxTags={5}
                    />
                  </Column>
                </Grid>
              </AccordionItem>

              {/* ------------------------------------------------- */}
              {/* Section 2 — Invoice address information           */}
              {/* ------------------------------------------------- */}
              <AccordionItem title="Invoice address information" open>
                <Grid className="invoice-page__section-grid" condensed>
                  <Column sm={4} md={8} lg={16} className="invoice-page__field-col">
                    <SingleSelect
                      id="submitted-by"
                      titleText={<RequiredLabel>Submitted by</RequiredLabel>}
                      items={SUBMITTED_BY_OPTIONS}
                      itemToString={lookupDescription}
                      selectedItem={findByCode(SUBMITTED_BY_OPTIONS, submittedByCode)}
                      onChange={({ selectedItem }) => setSubmittedByCode(selectedItem?.code ?? '')}
                      invalid={!!displayFieldErrors.submittedBy}
                      invalidText={displayFieldErrors.submittedBy}
                      disabled={!canEdit}
                    />
                  </Column>

                  <Column sm={4} md={8} lg={16} className="invoice-page__field-col">
                    <ClientAutocomplete
                      id="submitting-client-name"
                      titleText={<RequiredLabel>Submitting client name</RequiredLabel>}
                      size="md"
                      selectedClient={submittingClient}
                      onSelect={handleSubmittingClientSelect}
                      disabled={!canEdit}
                    />
                  </Column>

                  <Column sm={4} md={4} lg={8} className="invoice-page__field-col invoice-page__field-col--left">
                    <ClientNumberAutocomplete
                      id="submitting-client-number"
                      titleText={<RequiredLabel>Submitting client number</RequiredLabel>}
                      size="md"
                      selectedClient={submittingClient}
                      onSelect={handleSubmittingClientSelect}
                      disabled={!canEdit}
                    />
                  </Column>
                  <Column sm={4} md={4} lg={8} className="invoice-page__field-col">
                    <TextInput
                      id="submitting-client-location"
                      labelText={<RequiredLabel>Location</RequiredLabel>}
                      value={submittingClientLocation}
                      onChange={(e) => setSubmittingClientLocation(e.target.value)}
                      invalid={!!displayFieldErrors.submittingClientLocation}
                      invalidText={displayFieldErrors.submittingClientLocation}
                      disabled={!canEdit}
                    />
                  </Column>

                  <Column sm={4} md={4} lg={8} className="invoice-page__meta-col">
                    <span className="invoice-page__meta-label">City</span>
                    <span className="invoice-page__meta-value">{submittingClientCity || '—'}</span>
                  </Column>
                  <Column sm={4} md={4} lg={8} className="invoice-page__meta-col">
                    <span className="invoice-page__meta-label">Prov/State</span>
                    <span className="invoice-page__meta-value">{submittingClientProvState || '—'}</span>
                  </Column>

                  <Column sm={4} md={8} lg={16} className="invoice-page__subsection-heading-col">
                    <h3 className="invoice-page__subsection-heading">
                      Other party (
                      {submittedByCode === 'Buyer'
                        ? 'Seller'
                        : submittedByCode === 'Seller'
                          ? 'Buyer'
                          : 'Seller or Buyer'}
                      )
                    </h3>
                  </Column>

                  <Column sm={4} md={8} lg={16} className="invoice-page__field-col">
                    <ClientAutocomplete
                      id="other-client-name"
                      titleText={<RequiredLabel>Other party name</RequiredLabel>}
                      size="md"
                      selectedClient={otherClient}
                      onSelect={handleOtherClientSelect}
                      disabled={!canEdit}
                    />
                  </Column>

                  <Column sm={4} md={4} lg={8} className="invoice-page__field-col invoice-page__field-col--left">
                    <ClientNumberAutocomplete
                      id="other-client-number"
                      titleText={<RequiredLabel>Other party client number</RequiredLabel>}
                      size="md"
                      selectedClient={otherClient}
                      onSelect={handleOtherClientSelect}
                      disabled={!canEdit}
                    />
                  </Column>
                  <Column sm={4} md={4} lg={8} className="invoice-page__field-col">
                    <TextInput
                      id="other-client-location"
                      labelText="Location"
                      value={otherClientLocation}
                      onChange={(e) => setOtherClientLocation(e.target.value)}
                      invalid={!!displayFieldErrors.otherClientLocation}
                      invalidText={displayFieldErrors.otherClientLocation}
                      disabled={!canEdit}
                    />
                  </Column>

                  <Column sm={4} md={4} lg={8} className="invoice-page__meta-col">
                    <span className="invoice-page__meta-label">City</span>
                    <span className="invoice-page__meta-value">{otherClientCity || '—'}</span>
                  </Column>
                  <Column sm={4} md={4} lg={8} className="invoice-page__meta-col">
                    <span className="invoice-page__meta-label">Prov/State</span>
                    <span className="invoice-page__meta-value">{otherClientProvState || '—'}</span>
                  </Column>
                </Grid>
              </AccordionItem>

              {/* ------------------------------------------------- */}
              {/* Section 3 — Invoice detail information            */}
              {/* ------------------------------------------------- */}
              <AccordionItem title="Invoice detail information" open>
                <Grid className="invoice-page__section-grid" condensed>
                  <Column sm={4} md={3} lg={5} className="invoice-page__field-col invoice-page__field-col--left">
                    <SingleSelect
                      id="maturity"
                      titleText={<RequiredLabel>Maturity</RequiredLabel>}
                      items={maturityItems}
                      itemToString={lookupDescription}
                      selectedItem={findByCode(maturityItems, maturityCode)}
                      onChange={({ selectedItem }) => setMaturityCode(selectedItem?.code ?? '')}
                      invalid={!!displayFieldErrors.maturity}
                      invalidText={displayFieldErrors.maturity}
                      disabled={!canEdit}
                    />
                  </Column>
                  <Column sm={4} md={3} lg={5} className="invoice-page__field-col">
                    <TextInput
                      id="fob-code"
                      labelText={<RequiredLabel>FOB</RequiredLabel>}
                      value={fobCodeValue}
                      onChange={(e) => {
                        setFobCodeValue(e.target.value);
                        // Clear the inline error while the user is editing —
                        // it'll be re-checked on blur.
                        if (fobValidationError) setFobValidationError('');
                      }}
                      onBlur={validateFobOnBlur}
                      invalid={!!fobValidationError || !!displayFieldErrors.fobCode}
                      invalidText={fobValidationError || displayFieldErrors.fobCode}
                      disabled={!canEdit}
                    />
                  </Column>
                  <Column sm={4} md={2} lg={6} className="invoice-page__field-col">
                    <SingleSelect
                      id="primary-sort-code"
                      titleText="Primary sort code"
                      items={sortCodeItems}
                      itemToString={(item) => (item ? `${item.code} - ${item.description}` : '')}
                      selectedItem={findByCode(sortCodeItems, primarySortCodeValue)}
                      onChange={({ selectedItem }) => setPrimarySortCodeValue(selectedItem?.code ?? '')}
                      invalid={!!displayFieldErrors.primarySortCode}
                      invalidText={displayFieldErrors.primarySortCode}
                      disabled={!canEdit}
                    />
                  </Column>

                  <Column sm={4} md={3} lg={5} className="invoice-page__meta-col">
                    <span className="invoice-page__meta-label">Total pieces</span>
                    <span className="invoice-page__meta-value">
                      {totalPieces > 0 ? formatNumber(totalPieces) : '—'}
                    </span>
                  </Column>
                  <Column sm={4} md={3} lg={5} className="invoice-page__meta-col">
                    <span className="invoice-page__meta-label">Total volume (m3)</span>
                    <span className="invoice-page__meta-value">
                      {totalVolume > 0 ? formatNumber(totalVolume, 3) : '—'}
                    </span>
                  </Column>
                  <Column sm={4} md={2} lg={6} className="invoice-page__meta-col">
                    <span className="invoice-page__meta-label">Total amount</span>
                    <span className="invoice-page__meta-value">
                      {totalAmount > 0 ? formatCurrency(totalAmount) : '—'}
                    </span>
                  </Column>

                  <Column sm={4} md={3} lg={5} className="invoice-page__field-col invoice-page__field-col--left">
                    <TagInput
                      id="boom-numbers"
                      labelText="Boom numbers"
                      values={boomNumbers}
                      onChange={setBoomNumbers}
                      invalid={!!displayFieldErrors.boomNumbers}
                      invalidText={displayFieldErrors.boomNumbers}
                      disabled={!canEdit}
                      maxTags={5}
                    />
                  </Column>
                  <Column sm={4} md={3} lg={5} className="invoice-page__field-col">
                    <TagInput
                      id="timber-marks"
                      labelText="Timber marks"
                      values={timberMarks}
                      onChange={setTimberMarks}
                      invalid={!!displayFieldErrors.timberMarks}
                      invalidText={displayFieldErrors.timberMarks}
                      disabled={!canEdit}
                      maxTags={5}
                    />
                  </Column>
                  <Column sm={4} md={2} lg={6} className="invoice-page__field-col">
                    <TagInput
                      id="weigh-slips"
                      labelText="Weigh slips"
                      values={weighSlips}
                      onChange={setWeighSlips}
                      invalid={!!displayFieldErrors.weighSlips}
                      invalidText={displayFieldErrors.weighSlips}
                      disabled={!canEdit}
                      maxTags={5}
                    />
                  </Column>

                  <Column sm={4} md={8} lg={16} className="invoice-page__field-col">
                    <TextArea
                      id="client-primary-sort-code"
                      labelText="Client primary sort code"
                      value={clientPrimarySortCode}
                      onChange={(e) => setClientPrimarySortCode(e.target.value)}
                      rows={6}
                      disabled={!canEdit}
                    />
                  </Column>

                  <Column sm={4} md={8} lg={16} className="invoice-page__subsection-heading-col">
                    <h3 className="invoice-page__subsection-heading">Comments</h3>
                  </Column>

                  <Column sm={4} md={8} lg={16} className="invoice-page__field-col">
                    <TextArea
                      id="reviewer-comment"
                      labelText="Reviewer comment"
                      value={reviewerComment}
                      onChange={(e) => setReviewerComment(e.target.value)}
                      rows={6}
                      invalid={!!reviewerCommentError || !!displayFieldErrors.reviewerComment}
                      invalidText={reviewerCommentError || displayFieldErrors.reviewerComment}
                    />
                  </Column>
                  <Column sm={4} md={8} lg={16} className="invoice-page__field-col">
                    <TextArea
                      id="submitter-comment"
                      labelText="Submitted comment"
                      value={submitterComment}
                      onChange={(e) => setSubmitterComment(e.target.value)}
                      rows={6}
                      disabled={!canEdit}
                    />
                  </Column>
                </Grid>
              </AccordionItem>

              {/* ------------------------------------------------- */}
              {/* Section 5 — Invoice group summary (hidden until the invoice
                is saved; nothing to summarise on a brand-new NEW invoice). */}
              {/* ------------------------------------------------- */}
              {isExisting ? (
                <AccordionItem title="Invoice group summary" open className="invoice-page__group-summary-section">
                  {/* Toolbar above the line-items table: section heading on
                  the left, Export menu + Add-new-line-item trigger on the
                  right. The trigger opens a modal (see <FormModal> below) and
                  is hidden in NEW (unsaved) and APP, since adding a line
                  reverts the invoice to DFT on the backend. */}
                  <div className="invoice-page__group-summary-toolbar">
                    <TableToolbar>
                      <h3 className="invoice-page__group-summary-heading">Details</h3>
                      <TableToolbarContent>
                        <MenuButton label="Export table" kind="ghost" className="invoice-page__export-btn">
                          <MenuItem label="Export to CSV" renderIcon={Download} onClick={handleExportCsv} />
                          <MenuItem label="Export to PDF" renderIcon={Download} onClick={handleExportPdf} />
                        </MenuButton>
                        {canAddLineItem ? (
                          <Button
                            kind="primary"
                            renderIcon={Add}
                            onClick={() => setAddLineItemOpen(true)}
                            className="invoice-page__add-line-item-btn"
                          >
                            Add new item
                          </Button>
                        ) : null}
                      </TableToolbarContent>
                    </TableToolbar>
                  </div>

                  <ResultsTable
                    rows={groupRows}
                    columns={groupColumns}
                    size="md"
                    expandable
                    expandedRowIds={expandedGroupIds}
                    onExpandedRowIdsChange={setExpandedGroupIds}
                    withZebraStyles={false}
                    footerRow={lineItems.length > 0 ? groupTotalsRow : undefined}
                    emptyTitle="No line items"
                    emptyDescription="Use the Add new line item button above to start populating this invoice."
                    renderExpandedContent={(group) => (
                      <EditableLineItemsTable
                        rows={group.lineItems.map(toLineItemRow)}
                        // Per-row edit/delete everywhere except APP, where only
                        // the group (via the group-row actions) can be changed.
                        canEdit={canEditLineRows}
                        sortCodeItems={sortCodeItems}
                        speciesItems={speciesItems}
                        gradeItems={gradeItems}
                        speciesGradeCombos={speciesGradeCombos}
                        editDraft={editLineDraft}
                        fieldErrors={relabelRecord(editLineFieldErrors)}
                        onStartEdit={handleStartLineEdit}
                        onCancelEdit={handleCancelLineEdit}
                        onSaveEdit={handleSaveLineEdit}
                        onDraftChange={setEditLineDraft}
                        onDeleteItem={(lineId) => requestDeleteLineItem(group.id, lineId)}
                      />
                    )}
                  />
                </AccordionItem>
              ) : null}
            </Accordion>
          )}
        </Column>

        {/* --------------------------------------------------- */}
        {/* Action buttons row                                  */}
        {/* --------------------------------------------------- */}
        <Column sm={4} md={8} lg={16}>
          {isLoadingInvoice ? (
            <div className="invoice-page__actions">
              {[1, 2, 3, 4, 5, 6, 7].map((i) => (
                <div key={i} style={{ flex: '1 1 0' }}>
                  <ButtonSkeleton style={{ width: '100%' }} />
                </div>
              ))}
            </div>
          ) : (
            <div className="invoice-page__actions">
              <Button
                kind="primary"
                size="md"
                className="invoice-page__action-btn"
                renderIcon={savePending ? undefined : Save}
                onClick={handleSave}
                disabled={!canEdit || !canSavePerm || hasAnyFieldError || !requiredFieldsFilled || anyMutationPending}
              >
                {actionLabel(savePending, 'Save')}
              </Button>
              <Button
                kind="tertiary"
                size="md"
                className="invoice-page__action-btn invoice-page__action-btn--submit"
                onClick={handleSubmit}
                disabled={
                  !canSubmit ||
                  !canSubmitPerm ||
                  hasAnyFieldError ||
                  !requiredFieldsFilled ||
                  !hasLineItems ||
                  anyMutationPending
                }
              >
                {actionLabel(submitMutation.isPending, 'Submit')}
              </Button>
              {canUnapprove ? (
                <Button
                  kind="tertiary"
                  size="md"
                  className="invoice-page__action-btn invoice-page__action-btn--reject"
                  onClick={handleUnapprove}
                  disabled={anyMutationPending}
                >
                  {actionLabel(unapprovePending, 'Unapprove')}
                </Button>
              ) : (
                <Button
                  kind="primary"
                  size="md"
                  className="invoice-page__action-btn invoice-page__action-btn--approve"
                  renderIcon={approvePending ? undefined : Checkmark}
                  onClick={handleApprove}
                  disabled={!canChangeStatus || !canApprovePerm || anyMutationPending}
                >
                  {actionLabel(approvePending, 'Approve')}
                </Button>
              )}
              <Button
                kind="primary"
                size="md"
                className="invoice-page__action-btn invoice-page__action-btn--success"
                onClick={handleDuplicate}
                disabled={!canDuplicate || !canDuplicatePerm || anyMutationPending}
              >
                {actionLabel(duplicateMutation.isPending, 'Duplicate')}
              </Button>
              <Button
                kind="tertiary"
                size="md"
                className="invoice-page__action-btn invoice-page__action-btn--cancel"
                onClick={handleCancel}
                disabled={!canChangeStatus || !canCancelPerm || anyMutationPending}
              >
                {actionLabel(cancelPending, 'Cancel')}
              </Button>
              <Button
                kind="tertiary"
                size="md"
                className="invoice-page__action-btn invoice-page__action-btn--reject"
                onClick={handleReject}
                disabled={!canChangeStatus || !canRejectPerm || anyMutationPending}
              >
                {actionLabel(rejectPending, 'Reject')}
              </Button>
              <Button
                kind="danger"
                size="md"
                className="invoice-page__action-btn invoice-page__action-btn--delete"
                renderIcon={deleteMutation.isPending ? undefined : TrashCan}
                onClick={requestDeleteInvoice}
                disabled={!canDelete || !canDeletePerm || anyMutationPending}
              >
                {actionLabel(deleteMutation.isPending, 'Delete')}
              </Button>
            </div>
          )}
        </Column>
      </Grid>

      <FormModal
        open={editGroupDraft !== null}
        title={
          editGroupDraft
            ? `Edit group — ${editGroupDraft.originalSecondSort} / ${editGroupDraft.originalSpecies}`
            : 'Edit group'
        }
        submitLabel="Save"
        cancelLabel="Cancel"
        submitLoading={updateLineItemMutation.isPending}
        onClose={handleCancelGroupEdit}
        onSubmit={handleSaveGroupEdit}
      >
        {editGroupDraft ? (
          <Grid condensed>
            <Column sm={4} md={8} lg={16} className="invoice-page__field-col">
              <SingleSelect
                id="edit-group-secondary-sort"
                titleText={<RequiredLabel>Secondary Sort Code</RequiredLabel>}
                items={sortCodeItems}
                itemToString={(item) => (item ? `${item.code} - ${item.description}` : '')}
                selectedItem={findByCode(sortCodeItems, editGroupDraft.secondSort)}
                onChange={({ selectedItem }) =>
                  setEditGroupDraft({ ...editGroupDraft, secondSort: selectedItem?.code ?? '' })
                }
                invalid={!!editGroupFieldErrors.secondSort}
                invalidText={editGroupFieldErrors.secondSort}
              />
            </Column>
            <Column sm={4} md={8} lg={16} className="invoice-page__field-col">
              <SingleSelect
                id="edit-group-species"
                titleText={<RequiredLabel>Species</RequiredLabel>}
                items={groupEditFilteredSpecies}
                itemToString={(item) => item?.code ?? ''}
                selectedItem={findByCode(speciesItems, editGroupDraft.species)}
                onChange={({ selectedItem }) =>
                  setEditGroupDraft({ ...editGroupDraft, species: selectedItem?.code ?? '' })
                }
                invalid={!!editGroupFieldErrors.species}
                invalidText={editGroupFieldErrors.species}
              />
            </Column>
          </Grid>
        ) : null}
      </FormModal>

      {/* Shared delete-confirmation modal — drives every delete action. */}
      <FormModal
        open={deleteConfirm !== null}
        title={deleteConfirm?.title}
        submitLabel="Delete"
        cancelLabel="Cancel"
        submitIcon={TrashCan}
        danger
        submitLoading={deleteMutation.isPending || deleteLineItemMutation.isPending}
        onClose={() => setDeleteConfirm(null)}
        onSubmit={() => deleteConfirm?.onConfirm()}
      >
        {deleteConfirm?.message}
      </FormModal>

      {/* Add New Line Item — launched from the table toolbar. All the
      validations from the old inline panel still apply (submit stays
      disabled until the form is valid); the modal closes on a successful
      add and clears the form when cancelled. */}
      <FormModal
        open={addLineItemOpen}
        title="Add New Item"
        submitLabel="Add new item"
        cancelLabel="Cancel"
        submitIcon={Add}
        submitDisabled={!isAddLineItemValid}
        submitLoading={addLineItemMutation.isPending}
        onClose={() => {
          handleClearNewLineItem();
          setAddLineItemOpen(false);
        }}
        onSubmit={handleSaveNewLineItem}
      >
        <Grid condensed>
          <Column sm={4} md={4} lg={8} className="invoice-page__field-col">
            <SingleSelect
              id="new-line-secondary-sort"
              titleText={<RequiredLabel>Secondary Sort Code</RequiredLabel>}
              items={sortCodeItems}
              itemToString={(item) => (item ? `${item.code} - ${item.description}` : '')}
              selectedItem={newLineSecondarySort}
              onChange={({ selectedItem }) => setNewLineSecondarySort(selectedItem ?? null)}
              size="md"
              invalid={!!displayNewLineErrors.secondarySort}
              invalidText={displayNewLineErrors.secondarySort}
            />
          </Column>
          <Column sm={4} md={4} lg={8} className="invoice-page__field-col">
            <SingleSelect
              id="new-line-species"
              titleText={<RequiredLabel>Species</RequiredLabel>}
              items={filteredSpeciesItems}
              itemToString={(item) => item?.code ?? ''}
              selectedItem={newLineSpecies}
              onChange={({ selectedItem }) => setNewLineSpecies(selectedItem ?? null)}
              size="md"
              invalid={!!displayNewLineErrors.species}
              invalidText={displayNewLineErrors.species}
            />
          </Column>
          <Column sm={4} md={4} lg={8} className="invoice-page__field-col">
            <TextInput
              id="new-line-client-sort"
              labelText="Client Secondary Sort Code"
              value={newLineClientSort}
              onChange={(e) => setNewLineClientSort(e.target.value)}
              size="md"
            />
          </Column>
          <Column sm={4} md={4} lg={8} className="invoice-page__field-col">
            <TextInput
              id="new-line-pieces"
              labelText={<RequiredLabel>#Pieces</RequiredLabel>}
              value={newLinePieces}
              onChange={(e) => setNewLinePieces(e.target.value)}
              size="md"
              invalid={!!displayNewLineErrors.pieces}
              invalidText={displayNewLineErrors.pieces}
            />
          </Column>
          <Column sm={4} md={4} lg={8} className="invoice-page__field-col">
            <SingleSelect
              id="new-line-grade"
              titleText={<RequiredLabel>Grade</RequiredLabel>}
              items={filteredGradeItems}
              itemToString={(item) => item?.code ?? ''}
              selectedItem={newLineGrade}
              onChange={({ selectedItem }) => setNewLineGrade(selectedItem ?? null)}
              size="md"
              invalid={!!displayNewLineErrors.grade}
              invalidText={displayNewLineErrors.grade}
            />
          </Column>
          <Column sm={4} md={4} lg={8} className="invoice-page__field-col">
            <TextInput
              id="new-line-volume"
              labelText="Volume"
              value={newLineVolume}
              onChange={(e) => setNewLineVolume(e.target.value)}
              size="md"
              invalid={!!displayNewLineErrors.volume}
              invalidText={displayNewLineErrors.volume}
            />
          </Column>
          <Column sm={4} md={4} lg={8} className="invoice-page__field-col">
            <TextInput
              id="new-line-price"
              labelText="$Price"
              value={newLinePrice}
              onChange={(e) => setNewLinePrice(e.target.value)}
              size="md"
              invalid={!!displayNewLineErrors.price}
              invalidText={displayNewLineErrors.price}
            />
          </Column>
          <Column sm={4} md={4} lg={8} className="invoice-page__field-col">
            <TextInput id="new-line-amount" labelText="$Amount" value={computedNewLineAmount} readOnly size="md" />
          </Column>
        </Grid>
      </FormModal>
    </div>
  );
}

// --------------------------------------------------------------------
// Skeleton view shown while an existing invoice is being fetched.
// Mirrors the rough block-layout of the real accordion so the
// transition to the loaded form doesn't reflow the page jarringly.
// --------------------------------------------------------------------
function InvoiceFormSkeleton() {
  const fieldBlock = (count: number) => (
    <Grid className="invoice-page__section-grid" condensed>
      {Array.from({ length: count }).map((_, i) => (
        <Column key={i} sm={4} md={8} lg={16} className="invoice-page__field-col">
          <SkeletonText heading width="40%" />
          <SkeletonPlaceholder style={{ width: '100%', height: '2.5rem' }} />
        </Column>
      ))}
    </Grid>
  );

  return (
    <Accordion className="invoice-page__accordion" align="end">
      <AccordionItem title={<SkeletonText width="40%" />} open>
        {fieldBlock(5)}
      </AccordionItem>
      <AccordionItem title={<SkeletonText width="50%" />} open>
        {fieldBlock(6)}
      </AccordionItem>
      <AccordionItem title={<SkeletonText width="45%" />} open>
        {fieldBlock(7)}
      </AccordionItem>
      <AccordionItem title={<SkeletonText width="55%" />} open>
        <SkeletonPlaceholder style={{ width: '100%', height: '12rem', marginTop: '1rem' }} />
      </AccordionItem>
    </Accordion>
  );
}

export default InvoicePage;
