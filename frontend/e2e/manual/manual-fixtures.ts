// Sample API responses for the user manual screenshots. Every value is fictional: sample people
// ("Jordan Sample", "Pat Example"), example.com addresses, sample companies and client numbers.
// Never paste a record from TEST, PROD or the seeded e2e database into this file.

export type ManualScenario = 'industry' | 'ministry';

export type ManualResponse = {
  status?: number;
  contentType?: string;
  headers?: Record<string, string>;
  body: unknown;
};

/** The invoice the ministry edition opens, and the submission it arrived in. */
export const SAMPLE_INVOICE_ID = 9107;
export const SAMPLE_SUBMISSION_ID = 1042;
/** The forest client the industry edition's Submitter holds. */
export const OWN_CLIENT = '00000001';

const lookup = (entries: [string, string][]) => entries.map(([code, description]) => ({ code, description }));

const LOOKUPS: Record<string, { code: string; description: string }[]> = {
  '/lookup/status': lookup([
    ['DFT', 'Draft'],
    ['PRO', 'Processing'],
    ['APP', 'Approved'],
    ['REJ', 'Rejected'],
    ['CAN', 'Cancelled'],
    ['UNA', 'Unapproved'],
  ]),
  '/lookup/type': lookup([
    ['SAL', 'Sale'],
    ['PUR', 'Purchase'],
    ['ADJ', 'Adjustment'],
    ['CNL', 'Cancellation'],
  ]),
  '/lookup/maturity': lookup([
    ['O', 'Old Growth'],
    ['S', 'Second Growth'],
    ['M', 'Mixed Growth'],
    ['C', 'Cants / Export'],
    ['E', 'Export'],
  ]),
  '/lookup/submission-status': lookup([
    ['LOB', 'Lobby'],
    ['INB', 'Inbox'],
    ['COM', 'Complete'],
    ['REJ', 'Rejected'],
  ]),
  '/lookup/sort-code': lookup([
    ['A', 'Sawlog'],
    ['G', 'Gang sawlog'],
    ['P', 'Pulp'],
    ['S', 'Shingle'],
    ['U', 'Utility'],
  ]),
  '/lookup/species': lookup([
    ['B', 'Balsam'],
    ['C', 'Western redcedar'],
    ['F', 'Douglas-fir'],
    ['H', 'Hemlock'],
    ['Y', 'Yellow cedar'],
  ]),
  '/lookup/grade': lookup([
    ['D', 'Grade D'],
    ['H', 'Grade H'],
    ['I', 'Grade I'],
    ['J', 'Grade J'],
    ['U', 'Grade U'],
    ['X', 'Grade X'],
  ]),
  '/lookup/fob': lookup([
    ['NAN', 'Nanaimo'],
    ['VAN', 'Vancouver'],
    ['CRV', 'Campbell River'],
  ]),
};

const SPECIES_GRADES = ['B', 'C', 'F', 'H', 'Y'].flatMap((species) =>
  ['D', 'H', 'I', 'J', 'U', 'X'].map((grade) => ({ species, grade })),
);

const CLIENTS = [
  {
    clientNumber: OWN_CLIENT,
    clientName: 'EXAMPLE FORESTRY LTD.',
    clientLocnCode: '00',
    clientLocnName: 'HEAD OFFICE',
    city: 'NANAIMO',
    province: 'BC',
  },
  {
    clientNumber: '00000002',
    clientName: 'SAMPLE TIMBER CO.',
    clientLocnCode: '00',
    clientLocnName: 'HEAD OFFICE',
    city: 'CAMPBELL RIVER',
    province: 'BC',
  },
  {
    clientNumber: '00000003',
    clientName: 'COASTAL EXAMPLE MILLS LTD.',
    clientLocnCode: '00',
    clientLocnName: 'MILL',
    city: 'PORT ALBERNI',
    province: 'BC',
  },
];

const page = <T>(content: T[], size = 100) => ({
  content,
  totalElements: content.length,
  totalPages: 1,
  size,
  number: 0,
});

const SEARCH_RESULTS = [
  ['APP', 'EX-24101', '2026-10-06', 'Sale', OWN_CLIENT, 'EXAMPLE FORESTRY LTD.', 'Second Growth', 'Electronic'],
  ['APP', 'EX-24102', '2026-10-06', 'Sale', OWN_CLIENT, 'EXAMPLE FORESTRY LTD.', 'Second Growth', 'Electronic'],
  ['REJ', 'ST-5530', '2026-10-14', 'Purchase', '00000002', 'SAMPLE TIMBER CO.', 'Old Growth', 'Manual'],
  ['APP', 'CEM-0817', '2026-10-20', 'Sale', '00000003', 'COASTAL EXAMPLE MILLS LTD.', 'Mixed Growth', 'Electronic'],
  ['CAN', 'EX-24095', '2026-10-21', 'Sale', OWN_CLIENT, 'EXAMPLE FORESTRY LTD.', 'Second Growth', 'Electronic'],
  ['DFT', 'ST-5541', '2026-10-28', 'Purchase', '00000002', 'SAMPLE TIMBER CO.', 'Old Growth', 'Manual'],
  ['PRO', 'EX-24107', '2026-11-04', 'Sale', OWN_CLIENT, 'EXAMPLE FORESTRY LTD.', 'Second Growth', 'Electronic'],
  ['PRO', 'EX-24108', '2026-11-04', 'Adjustment', OWN_CLIENT, 'EXAMPLE FORESTRY LTD.', 'Second Growth', 'Electronic'],
  ['UNA', 'CEM-0822', '2026-11-12', 'Sale', '00000003', 'COASTAL EXAMPLE MILLS LTD.', 'Mixed Growth', 'Electronic'],
]
  .reverse()
  .map(([status, invoiceNumber, invoiceDate, type, clientNumber, clientName, maturity, submissionType], index) => ({
    coastalLogSaleId: invoiceNumber === 'EX-24107' ? SAMPLE_INVOICE_ID : 9200 + index,
    cspSubmissionId: SAMPLE_SUBMISSION_ID,
    invoiceStatus: LOOKUPS['/lookup/status'].find((s) => s.code === status)!.description,
    invoiceNumber,
    invoiceDate,
    type,
    clientNumber,
    clientName,
    maturity,
    submissionType,
  }));

const INBOX_ROWS = [
  [1042, '2026-11-18', 'Inbox', 'Electronic', 6, 2, 0, 3, 1],
  [null, '2026-11-17', 'Inbox', 'Manual', 1, 0, 0, 1, 0],
  [1039, '2026-11-10', 'Inbox', 'Electronic', 4, 3, 0, 1, 0],
  [1036, '2026-11-03', 'Complete', 'Electronic', 12, 11, 0, 0, 1],
  [null, '2026-10-29', 'Rejected', 'Manual', 1, 0, 1, 0, 0],
  [1031, '2026-10-21', 'Complete', 'Electronic', 8, 8, 0, 0, 0],
].map(
  ([id, submissionDate, submissionStatus, submissionType, total, approved, rejected, processing, cancelled], i) => ({
    coastalLogSaleId: 9300 + i,
    submissionId: id === null ? null : String(id),
    submissionDate,
    submissionStatus,
    submissionType,
    invTotal: total,
    invApproved: approved,
    invRejected: rejected,
    invProcessing: processing,
    invCancelled: cancelled,
  }),
);

const line = (
  lineItemID: number,
  secondSort: string,
  species: string,
  grade: string,
  numOfPieces: number,
  volume: number,
  price: number,
) => ({
  lineItemID,
  invoiceID: SAMPLE_INVOICE_ID,
  secondSort,
  clientSecondarySort: null,
  species,
  speciesDescription: LOOKUPS['/lookup/species'].find((s) => s.code === species)!.description,
  grade,
  numOfPieces,
  price,
  volume,
  convertedPrice: null,
  amount: Math.round(volume * price * 100) / 100,
});

const SAMPLE_LINES = [
  line(1, 'A', 'H', 'H', 182, 412.6, 96.5),
  line(2, 'A', 'H', 'I', 240, 388.2, 88.0),
  line(3, 'A', 'F', 'H', 96, 251.4, 128.75),
  line(4, 'G', 'C', 'J', 64, 140.9, 210.0),
  line(5, 'P', 'H', 'U', 310, 205.3, 48.0),
];

const sum = (values: number[]) => Math.round(values.reduce((total, value) => total + value, 0) * 1000) / 1000;

const SAMPLE_INVOICE = {
  invID: SAMPLE_INVOICE_ID,
  submissionId: SAMPLE_SUBMISSION_ID,
  submissionNumber: SAMPLE_SUBMISSION_ID,
  invNumber: 'EX-24107',
  invoiceDate: '2026-11-04',
  invStatus: 'PRO',
  invType: 'SAL',
  maturity: 'S',
  fobCode: 'NAN',
  primarySortCode: 'A',
  clientPrimarySortCode: null,
  totalAmt: sum(SAMPLE_LINES.map((l) => l.amount)),
  totalPieces: sum(SAMPLE_LINES.map((l) => l.numOfPieces)),
  totalVol: sum(SAMPLE_LINES.map((l) => l.volume)),
  submitterClientNum: OWN_CLIENT,
  submitterLocation: '00',
  submittedBy: 'Seller',
  clientNumber: OWN_CLIENT,
  clientLocation: '00',
  otherClientNum: '00000003',
  otherClientLocation: '00',
  otherClientName: 'COASTAL EXAMPLE MILLS LTD.',
  otherClientCity: 'PORT ALBERNI',
  otherClientProvState: 'BC',
  boomNumbers: ['BM-1182'],
  timberMarks: ['EX123'],
  weightSlips: [],
  replaceInvNum: null,
  adjustInvNum: null,
  reviewComments: null,
  submitComments: 'Second boom delivered under the November contract.',
  entryUserID: 'JSAMPLE',
  version: 'manual-1',
  lineItems: SAMPLE_LINES,
  warnings: [],
  errors: [],
};

const FLAT_PRICE_ROWS = [
  ['S', 'H', 'H', 'A', 100, '2026-01-01'],
  ['S', 'H', 'I', 'A', 92, '2026-01-01'],
  ['S', 'H', 'J', 'A', 81, '2026-01-01'],
  ['S', 'H', 'U', 'A', 55, '2026-01-01'],
  ['S', 'H', 'X', 'A', 40, '2026-01-01'],
].map(([maturity, species, grade, sortCode, flatPriceConversion, effectiveDate], i) => ({
  id: 700 + i,
  modellingCode: 'P',
  maturity,
  species,
  grade,
  sortCode,
  flatPriceConversion,
  effectiveDate,
  expiryDate: null,
  revisionCount: 1,
  entryUserid: 'PEXAMPLE',
  entryTimestamp: '2026-01-05',
  updateUserid: 'PEXAMPLE',
  updateTimestamp: '2026-01-05',
}));

const SORT_CODES = LOOKUPS['/lookup/sort-code'].map(({ code, description }) => ({
  sortCode: code,
  description,
  effectiveDate: '2000-01-01',
  expiryDate: '9999-12-31',
  updateTimestamp: '2026-01-05',
}));

/** The file name the capture uploads; the parse fixture describes its contents. */
export const UPLOAD_FILE_NAME = 'november-invoices.xml';

const UPLOAD_INVOICES = [
  ['EX-24107', '2026-11-04', 'SAL', 4, 1398.4, 140540.95, 892],
  ['EX-24108', '2026-11-04', 'ADJ', 1, 12.5, 1206.25, 0],
  ['EX-24109', '2026-11-06', 'SAL', 2, 520.75, 61238.5, 301],
].map(([invoiceNumber, invoiceDate, invoiceType, , totalVolume, totalAmount, totalPieces], position) => ({
  // The parse response numbers invoices and their lines from 1.
  index: position + 1,
  invoiceNumber: String(invoiceNumber),
  invoiceDate: String(invoiceDate),
  invoiceType: String(invoiceType),
  sellerClientNumber: OWN_CLIENT,
  buyerClientNumber: '00000003',
  maturity: 'S',
  locationFOB: 'NAN',
  totalAmount: Number(totalAmount),
  totalVolume: Number(totalVolume),
  totalPieces: Number(totalPieces),
  replacesInvoiceNumbers: null,
  adjustsInvoiceNumbers: invoiceType === 'ADJ' ? 'EX-24101' : null,
  sellerClientLocnCode: '00',
  buyerClientLocnCode: '00',
  otherPartyName: null,
  otherPartyCity: null,
  otherPartyProvState: null,
  primarySortCode: 'A',
  clientPrimarySortCode: null,
  boomNumbers: 'BM-1182',
  timberMarks: 'EX123',
  weighSlipNumbers: null,
  submitterNotes: null,
}));

const UPLOAD_LINES = SAMPLE_LINES.slice(0, 4)
  .map((l) => ({ ...l, invoiceIndex: 0 }))
  .concat([{ ...line(6, 'A', 'H', 'H', 0, 12.5, 96.5), invoiceIndex: 1 }])
  .concat(
    [line(7, 'A', 'H', 'H', 180, 320.25, 118.0), line(8, 'A', 'F', 'I', 121, 200.5, 116.95)].map((l) => ({
      ...l,
      invoiceIndex: 2,
    })),
  )
  .map((l, i, all) => ({
    invoiceIndex: l.invoiceIndex + 1,
    lineIndex: all.slice(0, i).filter((other) => other.invoiceIndex === l.invoiceIndex).length + 1,
    invoiceNumber: UPLOAD_INVOICES[l.invoiceIndex].invoiceNumber,
    species: l.species,
    grade: l.grade,
    secondarySortCode: l.secondSort,
    clientSecondarySortCode: null,
    numberOfPieces: l.numOfPieces,
    volume: l.volume,
    price: l.price,
  }));

const PARSED_UPLOAD = {
  valid: true,
  code: 'OK',
  message: 'Submission parsed.',
  errors: [],
  submission: {
    email: 'jordan.sample@example.com',
    telephone: '250-555-0100',
    monthComplete: 'N',
    sellerSubmission: 'Y',
    submissionClientNumber: OWN_CLIENT,
    submissionClientLocnCode: '00',
    invoices: UPLOAD_INVOICES,
    lineItems: UPLOAD_LINES,
  },
};

const UPLOAD_CHECK = {
  valid: true,
  code: 'OK',
  message: 'Submission is valid.',
  acceptedInvoices: UPLOAD_INVOICES.map((invoice) => invoice.invoiceNumber),
  rejectedInvoices: [],
  errors: [
    {
      messageKey: 'invoice.type.not.saleorpurchase.warning',
      args: ['ADJ'],
      type: 'WARNING',
      message: 'invoice #2 (EX-24108): The Invoice Type is ADJ.',
    },
  ],
};

const HISTORY_ROWS = [
  [SAMPLE_SUBMISSION_ID, '2026-11-18', 'JSAMPLE', 'Inbox', 6, 0],
  [1039, '2026-11-10', 'JSAMPLE', 'Inbox', 4, 0],
  [1036, '2026-11-03', 'JSAMPLE', 'Complete', 12, 1],
  [1031, '2026-10-21', 'PEXAMPLE', 'Complete', 8, 0],
  [1027, '2026-10-07', 'JSAMPLE', 'Rejected', 3, 3],
].map(([id, submissionDate, submittedBy, submissionStatus, invoiceCount, commentedInvoiceCount]) => ({
  cspSubmissionId: Number(id) + 5000,
  submissionId: String(id),
  submissionDate,
  submittedBy,
  clientNumber: OWN_CLIENT,
  clientName: 'EXAMPLE FORESTRY LTD.',
  submissionStatus,
  invoiceCount,
  commentedInvoiceCount,
}));

const HISTORY_COMMENTS = [
  {
    invoiceNumber: 'EX-23950',
    status: 'Rejected',
    comment: 'Timber mark EX999 is not registered to this client. Correct it and resubmit.',
  },
  { invoiceNumber: 'EX-23951', status: 'Rejected', comment: 'Duplicate of invoice EX-23950.' },
  {
    invoiceNumber: 'EX-23952',
    status: 'Rejected',
    comment: 'Volume does not match the scale slip; resubmit with the scaled volume.',
  },
];

const SUBMISSION_DECISIONS: Record<string, string> = {
  'EX-24107': 'Processing',
  'EX-24108': 'Processing',
  'EX-24109': 'Approved',
};

const detailInvoiceId = (invoiceNumber: string) =>
  invoiceNumber === 'EX-24107'
    ? SAMPLE_INVOICE_ID
    : 9400 + UPLOAD_INVOICES.find((invoice) => invoice.invoiceNumber === invoiceNumber)!.index;

const SUBMISSION_DETAIL = {
  cspSubmissionId: SAMPLE_SUBMISSION_ID + 5000,
  submissionId: String(SAMPLE_SUBMISSION_ID),
  submissionDate: '2026-11-18',
  submittedBy: 'JSAMPLE',
  submissionStatus: 'Inbox',
  clientNumber: OWN_CLIENT,
  clientName: 'EXAMPLE FORESTRY LTD.',
  clientLocnCode: '00',
  email: 'jordan.sample@example.com',
  telephone: '250-555-0100',
  monthComplete: 'N',
  sellerSubmission: 'Y',
  adminComment: null,
  invoices: UPLOAD_INVOICES.map((invoice) => ({
    coastalLogSaleId: detailInvoiceId(invoice.invoiceNumber),
    invoiceNumber: invoice.invoiceNumber,
    invoiceDate: invoice.invoiceDate,
    type: LOOKUPS['/lookup/type'].find((t) => t.code === invoice.invoiceType)!.description,
    status: SUBMISSION_DECISIONS[invoice.invoiceNumber],
    sellerClient: invoice.sellerClientNumber,
    buyerClient: invoice.buyerClientNumber,
    maturity: 'Second Growth',
    fobLocation: invoice.locationFOB,
    totalAmount: invoice.totalAmount,
    totalVolume: invoice.totalVolume,
    totalPieces: invoice.totalPieces,
    replacesInvoiceNumbers: invoice.replacesInvoiceNumbers,
    adjustsInvoiceNumbers: invoice.adjustsInvoiceNumbers,
    sellerClientLocnCode: '00',
    buyerClientLocnCode: '00',
    otherPartyName: null,
    otherPartyCity: null,
    otherPartyProvState: null,
    primarySortCode: 'A',
    clientPrimarySortCode: null,
    boomNumbers: invoice.boomNumbers,
    timberMarks: invoice.timberMarks,
    weighSlips: null,
    submitterNotes: null,
    staffComment: invoice.invoiceNumber === 'EX-24109' ? 'Approved. Prices confirmed against the contract.' : null,
  })),
  lineItems: UPLOAD_LINES.map((l) => ({
    coastalLogSaleId: detailInvoiceId(l.invoiceNumber),
    invoiceNumber: l.invoiceNumber,
    species: l.species,
    grade: l.grade,
    sortCode: l.secondarySortCode,
    clientSortCode: null,
    pieces: l.numberOfPieces,
    volume: l.volume,
    price: l.price,
  })),
};

const clientsMatching = (params: URLSearchParams) => {
  const name = params.get('name')?.toUpperCase();
  const number = params.get('number')?.padStart(8, '0');
  return CLIENTS.filter((client) =>
    name ? client.clientName.includes(name) : number ? client.clientNumber === number : true,
  );
};

/** The response for one API call, or undefined when the capture has no fixture for it. */
export const resolveManualApi = (
  scenario: ManualScenario,
  method: string,
  apiPath: string,
  params: URLSearchParams,
  body: string | null,
): ManualResponse | undefined => {
  void scenario;
  void body;
  if (method === 'POST' && apiPath === '/submissions/parse') return { body: PARSED_UPLOAD };
  if (method === 'POST' && apiPath === '/submissions/validate/business') return { body: UPLOAD_CHECK };
  if (method !== 'GET') return undefined;
  if (LOOKUPS[apiPath]) return { body: LOOKUPS[apiPath] };
  if (apiPath === '/health') return { body: { status: 'UP' } };
  if (apiPath === '/lookup/species-grade-combinations') return { body: SPECIES_GRADES };
  if (apiPath.startsWith('/lookup/grade-by-species/')) return { body: LOOKUPS['/lookup/grade'] };
  if (apiPath === '/clients') return { body: clientsMatching(params) };
  if (apiPath === '/search') return { body: page(SEARCH_RESULTS) };
  if (apiPath === '/inbox') return { body: page(INBOX_ROWS) };
  if (apiPath === `/invoices/${SAMPLE_INVOICE_ID}`) return { body: SAMPLE_INVOICE };
  if (apiPath === '/flat-price-conversions') return { body: FLAT_PRICE_ROWS };
  if (apiPath === '/sort-codes') return { body: page(SORT_CODES) };
  if (apiPath === '/submission-history') return { body: page(HISTORY_ROWS, 10) };
  if (apiPath === `/submission-history/${SAMPLE_SUBMISSION_ID}`) return { body: SUBMISSION_DETAIL };
  if (/^\/submission-history\/\d+\/invoices$/.test(apiPath)) return { body: HISTORY_COMMENTS };
  return undefined;
};
