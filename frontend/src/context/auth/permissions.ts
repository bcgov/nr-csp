/**
 * Action-level permission constants for CSP.
 *
 * Action strings are identical to the Java PermissionConstants values.
 * Keep in sync with:
 *   backend/src/main/java/ca/bc/gov/nrs/csp/backend/util/constants/PermissionConstants.java
 */

// ── InvoiceSearch ─────────────────────────────────────────────────────────────
const INVOICE_SEARCH = 'InvoiceSearch';
const INVOICE_SEARCH_SEARCH = 'InvoiceSearch/Search';

// ── LogSourceCode ─────────────────────────────────────────────────────────────
const LOG_SOURCE_CODE = 'LogSourceCode';

// ── esfSubmit (ADMIN only) ────────────────────────────────────────────────────
export const ESF_SUBMIT = 'esfSubmit';

// ── inbox ─────────────────────────────────────────────────────────────────────
export const INBOX = 'inbox';
const INBOX_BACK = 'inbox/Back';
const INBOX_CLEAR = 'inbox/Clear';
const INBOX_SEARCH = 'inbox/Search';

// ── invoiceDetails ────────────────────────────────────────────────────────────
export const INVOICE_DETAILS = 'invoiceDetails';
export const INVOICE_DETAILS_APPROVE = 'invoiceDetails/Approve';
const INVOICE_DETAILS_BACK = 'invoiceDetails/Back';
const INVOICE_DETAILS_CSV = 'invoiceDetails/CSV';
export const INVOICE_DETAILS_CANCEL = 'invoiceDetails/Cancel';
export const INVOICE_DETAILS_DELETE = 'invoiceDetails/Delete';
export const INVOICE_DETAILS_DUPLICATE = 'invoiceDetails/Duplicate';
const INVOICE_DETAILS_NEW_LINE_ITEM = 'invoiceDetails/New Line Item';
const INVOICE_DETAILS_PDF = 'invoiceDetails/PDF';
const INVOICE_DETAILS_PRINTS = 'invoiceDetails/Prints';
export const INVOICE_DETAILS_REJECT = 'invoiceDetails/Reject';
export const INVOICE_DETAILS_SAVE = 'invoiceDetails/Save';
const INVOICE_DETAILS_SAVE_INVOICE = 'invoiceDetails/Save Invoice';
export const INVOICE_DETAILS_SUBMIT = 'invoiceDetails/Submit';
export const INVOICE_DETAILS_UNAPPROVE = 'invoiceDetails/Unapprove';
const INVOICE_DETAILS_UPDATE_GROUP = 'invoiceDetails/Update Group';
const INVOICE_DETAILS_XLS = 'invoiceDetails/XLS';

// ── modelFlatPriceConv ────────────────────────────────────────────────────────
const MODEL_FLAT_PRICE_CONV = 'modelFlatPriceConv';
const MODEL_FLAT_PRICE_CONV_ADD_NEW_ROW = 'modelFlatPriceConv/Add New Row';
const MODEL_FLAT_PRICE_CONV_BACK = 'modelFlatPriceConv/Back';
const MODEL_FLAT_PRICE_CONV_CSV = 'modelFlatPriceConv/CSV';
const MODEL_FLAT_PRICE_CONV_CANCEL = 'modelFlatPriceConv/Cancel';
const MODEL_FLAT_PRICE_CONV_CLEAR = 'modelFlatPriceConv/Clear';
const MODEL_FLAT_PRICE_CONV_CLEAR_ALL_CREATE_NEW = 'modelFlatPriceConv/Clear All/Create New';
const MODEL_FLAT_PRICE_CONV_CLEAR_FILTERS = 'modelFlatPriceConv/Clear Filters';
const MODEL_FLAT_PRICE_CONV_CLEAR_RESULTS = 'modelFlatPriceConv/Clear Results';
const MODEL_FLAT_PRICE_CONV_COPY_DATA = 'modelFlatPriceConv/Copy Data';
const MODEL_FLAT_PRICE_CONV_DELETE = 'modelFlatPriceConv/Delete';
const MODEL_FLAT_PRICE_CONV_EDIT = 'modelFlatPriceConv/Edit';
const MODEL_FLAT_PRICE_CONV_PDF = 'modelFlatPriceConv/PDF';
const MODEL_FLAT_PRICE_CONV_PRINT = 'modelFlatPriceConv/Print';
const MODEL_FLAT_PRICE_CONV_SAVE = 'modelFlatPriceConv/Save';
const MODEL_FLAT_PRICE_CONV_SAVE_NEW_ROW = 'modelFlatPriceConv/Save New Row';
const MODEL_FLAT_PRICE_CONV_SEARCH = 'modelFlatPriceConv/Search';
const MODEL_FLAT_PRICE_CONV_XLS = 'modelFlatPriceConv/XLS';

// ── modelMPSConv ──────────────────────────────────────────────────────────────
const MODEL_MPS_CONV = 'modelMPSConv';
const MODEL_MPS_CONV_ADD_NEW_ROW = 'modelMPSConv/Add New Row';
const MODEL_MPS_CONV_BACK = 'modelMPSConv/Back';
const MODEL_MPS_CONV_CSV = 'modelMPSConv/CSV';
const MODEL_MPS_CONV_CANCEL = 'modelMPSConv/Cancel';
const MODEL_MPS_CONV_CLEAR_ALL_CREATE_NEW = 'modelMPSConv/Clear All/Create New';
const MODEL_MPS_CONV_COPY_DATA = 'modelMPSConv/Copy Data';
const MODEL_MPS_CONV_DELETE = 'modelMPSConv/Delete';
const MODEL_MPS_CONV_EDIT = 'modelMPSConv/Edit';
const MODEL_MPS_CONV_PDF = 'modelMPSConv/PDF';
const MODEL_MPS_CONV_PRINT = 'modelMPSConv/Print';
const MODEL_MPS_CONV_SAVE = 'modelMPSConv/Save';
const MODEL_MPS_CONV_SAVE_NEW_ROW = 'modelMPSConv/Save New Row';
const MODEL_MPS_CONV_SEARCH = 'modelMPSConv/Search';
const MODEL_MPS_CONV_XLS = 'modelMPSConv/XLS';

// ── prodFlatPriceConv ─────────────────────────────────────────────────────────
export const PROD_FLAT_PRICE_CONV = 'prodFlatPriceConv';
export const PROD_FLAT_PRICE_CONV_ADD_NEW_ROW = 'prodFlatPriceConv/Add New Row';
const PROD_FLAT_PRICE_CONV_BACK = 'prodFlatPriceConv/Back';
const PROD_FLAT_PRICE_CONV_CSV = 'prodFlatPriceConv/CSV';
const PROD_FLAT_PRICE_CONV_CANCEL = 'prodFlatPriceConv/Cancel';
const PROD_FLAT_PRICE_CONV_CLEAR = 'prodFlatPriceConv/Clear';
const PROD_FLAT_PRICE_CONV_CLEAR_FILTERS = 'prodFlatPriceConv/Clear Filters';
const PROD_FLAT_PRICE_CONV_CLEAR_RESULTS = 'prodFlatPriceConv/Clear Results';
export const PROD_FLAT_PRICE_CONV_DELETE = 'prodFlatPriceConv/Delete';
export const PROD_FLAT_PRICE_CONV_EDIT = 'prodFlatPriceConv/Edit';
const PROD_FLAT_PRICE_CONV_PDF = 'prodFlatPriceConv/PDF';
const PROD_FLAT_PRICE_CONV_PRINT = 'prodFlatPriceConv/Print';
const PROD_FLAT_PRICE_CONV_SAVE = 'prodFlatPriceConv/Save';
const PROD_FLAT_PRICE_CONV_SAVE_NEW_ROW = 'prodFlatPriceConv/Save New Row';
const PROD_FLAT_PRICE_CONV_SEARCH = 'prodFlatPriceConv/Search';
const PROD_FLAT_PRICE_CONV_XLS = 'prodFlatPriceConv/XLS';

// ── prodMPSConv ───────────────────────────────────────────────────────────────
const PROD_MPS_CONV = 'prodMPSConv';
const PROD_MPS_CONV_ADD_NEW_ROW = 'prodMPSConv/Add New Row';
const PROD_MPS_CONV_BACK = 'prodMPSConv/Back';
const PROD_MPS_CONV_CSV = 'prodMPSConv/CSV';
const PROD_MPS_CONV_CANCEL = 'prodMPSConv/Cancel';
const PROD_MPS_CONV_DELETE = 'prodMPSConv/Delete';
const PROD_MPS_CONV_EDIT = 'prodMPSConv/Edit';
const PROD_MPS_CONV_PDF = 'prodMPSConv/PDF';
const PROD_MPS_CONV_PRINT = 'prodMPSConv/Print';
const PROD_MPS_CONV_SAVE = 'prodMPSConv/Save';
const PROD_MPS_CONV_SAVE_NEW_ROW = 'prodMPSConv/Save New Row';
const PROD_MPS_CONV_SEARCH = 'prodMPSConv/Search';
const PROD_MPS_CONV_XLS = 'prodMPSConv/XLS';

// ── prodSortCode ──────────────────────────────────────────────────────────────
export const PROD_SORT_CODE = 'prodSortCode';
export const PROD_SORT_CODE_ADD_NEW_ROW = 'prodSortCode/Add New Row';
const PROD_SORT_CODE_BACK = 'prodSortCode/Back';
const PROD_SORT_CODE_CSV = 'prodSortCode/CSV';
const PROD_SORT_CODE_CANCEL = 'prodSortCode/Cancel';
export const PROD_SORT_CODE_DELETE = 'prodSortCode/Delete';
const PROD_SORT_CODE_PDF = 'prodSortCode/PDF';
const PROD_SORT_CODE_PRINT = 'prodSortCode/Print';
const PROD_SORT_CODE_SAVE = 'prodSortCode/Save';
const PROD_SORT_CODE_SAVE_NEW_ROW = 'prodSortCode/Save New Row';
const PROD_SORT_CODE_SEARCH = 'prodSortCode/Search';
const PROD_SORT_CODE_XLS = 'prodSortCode/XLS';

// ── r06 ───────────────────────────────────────────────────────────────────────
export const R06 = 'r06';
const R06_ADD = 'r06/Add';
const R06_BACK = 'r06/Back';
const R06_CSV = 'r06/CSV';
const R06_CLEAR = 'r06/Clear';
const R06_GENERATE_REPORT = 'r06/Generate Report';
const R06_PDF = 'r06/PDF';
const R06_XLS = 'r06/XLS';
const R06_REPORT = 'r06/r06Report';

// ── r07 ───────────────────────────────────────────────────────────────────────
export const R07 = 'r07';
const R07_BACK = 'r07/Back';
const R07_CSV = 'r07/CSV';
const R07_CLEAR = 'r07/Clear';
const R07_GENERATE_REPORT = 'r07/Generate Report';
const R07_PDF = 'r07/PDF';
const R07_XLS = 'r07/XLS';
const R07_REPORT = 'r07/r07Report';

// ── r08 ───────────────────────────────────────────────────────────────────────
export const R08 = 'r08';
const R08_BACK = 'r08/Back';
const R08_CSV = 'r08/CSV';
const R08_CLEAR = 'r08/Clear';
const R08_GENERATE_REPORT = 'r08/Generate Report';
const R08_PDF = 'r08/PDF';
const R08_XLS = 'r08/XLS';
const R08_REPORT = 'r08/r08Report';

// ── r10 ───────────────────────────────────────────────────────────────────────
export const R10 = 'r10';
const R10_BACK = 'r10/Back';
const R10_CSV = 'r10/CSV';
const R10_CLEAR = 'r10/Clear';
const R10_GENERATE_REPORT = 'r10/Generate Report';
const R10_PDF = 'r10/PDF';
const R10_XLS = 'r10/XLS';
const R10_REPORT = 'r10/r10Report';

// ── r11 ───────────────────────────────────────────────────────────────────────
export const R11 = 'r11';
const R11_BACK = 'r11/Back';
const R11_CSV = 'r11/CSV';
const R11_CLEAR = 'r11/Clear';
const R11_GENERATE_REPORT = 'r11/Generate Report';
const R11_PDF = 'r11/PDF';
const R11_XLS = 'r11/XLS';
const R11_REPORT = 'r11/r11Report';

// ── r12 ───────────────────────────────────────────────────────────────────────
export const R12 = 'r12';
const R12_BACK = 'r12/Back';
const R12_CSV = 'r12/CSV';
const R12_CLEAR = 'r12/Clear';
const R12_GENERATE_REPORT = 'r12/Generate Report';
const R12_PDF = 'r12/PDF';
const R12_XLS = 'r12/XLS';
const R12_REPORT = 'r12/r12Report';

// ── r13 ───────────────────────────────────────────────────────────────────────
export const R13 = 'r13';
const R13_BACK = 'r13/Back';
const R13_CSV = 'r13/CSV';
const R13_CLEAR = 'r13/Clear';
const R13_GENERATE_REPORT = 'r13/Generate Report';
const R13_PDF = 'r13/PDF';
const R13_XLS = 'r13/XLS';
const R13_REPORT = 'r13/r13Report';

// ── r14 ───────────────────────────────────────────────────────────────────────
const R14 = 'r14';
const R14_BACK = 'r14/Back';
const R14_CSV = 'r14/CSV';
const R14_CLEAR = 'r14/Clear';
const R14_GENERATE_REPORT = 'r14/Generate Report';
const R14_PDF = 'r14/PDF';
const R14_SEARCH = 'r14/Search';
const R14_XLS = 'r14/XLS';
const R14_REPORT = 'r14/r14Report';

// ── reports ───────────────────────────────────────────────────────────────────
export const REPORTS = 'reports';

// ── search ────────────────────────────────────────────────────────────────────
export const SEARCH = 'search';
const SEARCH_BACK = 'search/Back';
const SEARCH_CLEAR = 'search/Clear';
const SEARCH_DETAILS = 'search/Details';
const SEARCH_SEARCH = 'search/Search';

// ── tableMaintenance (ADMIN only) ─────────────────────────────────────────────
export const TABLE_MAINTENANCE = 'tableMaintenance';

// ── XML submissions (not in the legacy matrix: legacy industry used ESF) ──────
export const SUBMISSION_UPLOAD = 'submissionUpload';
export const SUBMISSION_HISTORY = 'submissionHistory';

// ── Role permission sets ──────────────────────────────────────────────────────

const ADMIN_ACTIONS = new Set([
  INVOICE_SEARCH,
  INVOICE_SEARCH_SEARCH,
  LOG_SOURCE_CODE,
  ESF_SUBMIT,
  INBOX,
  INBOX_BACK,
  INBOX_CLEAR,
  INBOX_SEARCH,
  INVOICE_DETAILS,
  INVOICE_DETAILS_APPROVE,
  INVOICE_DETAILS_BACK,
  INVOICE_DETAILS_CSV,
  INVOICE_DETAILS_CANCEL,
  INVOICE_DETAILS_DELETE,
  INVOICE_DETAILS_DUPLICATE,
  INVOICE_DETAILS_NEW_LINE_ITEM,
  INVOICE_DETAILS_PDF,
  INVOICE_DETAILS_PRINTS,
  INVOICE_DETAILS_REJECT,
  INVOICE_DETAILS_SAVE,
  INVOICE_DETAILS_SAVE_INVOICE,
  INVOICE_DETAILS_SUBMIT,
  INVOICE_DETAILS_UNAPPROVE,
  INVOICE_DETAILS_UPDATE_GROUP,
  INVOICE_DETAILS_XLS,
  MODEL_FLAT_PRICE_CONV,
  MODEL_FLAT_PRICE_CONV_ADD_NEW_ROW,
  MODEL_FLAT_PRICE_CONV_BACK,
  MODEL_FLAT_PRICE_CONV_CSV,
  MODEL_FLAT_PRICE_CONV_CANCEL,
  MODEL_FLAT_PRICE_CONV_CLEAR,
  MODEL_FLAT_PRICE_CONV_CLEAR_ALL_CREATE_NEW,
  MODEL_FLAT_PRICE_CONV_CLEAR_FILTERS,
  MODEL_FLAT_PRICE_CONV_CLEAR_RESULTS,
  MODEL_FLAT_PRICE_CONV_COPY_DATA,
  MODEL_FLAT_PRICE_CONV_DELETE,
  MODEL_FLAT_PRICE_CONV_EDIT,
  MODEL_FLAT_PRICE_CONV_PDF,
  MODEL_FLAT_PRICE_CONV_PRINT,
  MODEL_FLAT_PRICE_CONV_SAVE,
  MODEL_FLAT_PRICE_CONV_SAVE_NEW_ROW,
  MODEL_FLAT_PRICE_CONV_SEARCH,
  MODEL_FLAT_PRICE_CONV_XLS,
  MODEL_MPS_CONV,
  MODEL_MPS_CONV_ADD_NEW_ROW,
  MODEL_MPS_CONV_BACK,
  MODEL_MPS_CONV_CSV,
  MODEL_MPS_CONV_CANCEL,
  MODEL_MPS_CONV_CLEAR_ALL_CREATE_NEW,
  MODEL_MPS_CONV_COPY_DATA,
  MODEL_MPS_CONV_DELETE,
  MODEL_MPS_CONV_EDIT,
  MODEL_MPS_CONV_PDF,
  MODEL_MPS_CONV_PRINT,
  MODEL_MPS_CONV_SAVE,
  MODEL_MPS_CONV_SAVE_NEW_ROW,
  MODEL_MPS_CONV_SEARCH,
  MODEL_MPS_CONV_XLS,
  PROD_FLAT_PRICE_CONV,
  PROD_FLAT_PRICE_CONV_ADD_NEW_ROW,
  PROD_FLAT_PRICE_CONV_BACK,
  PROD_FLAT_PRICE_CONV_CSV,
  PROD_FLAT_PRICE_CONV_CANCEL,
  PROD_FLAT_PRICE_CONV_CLEAR,
  PROD_FLAT_PRICE_CONV_CLEAR_FILTERS,
  PROD_FLAT_PRICE_CONV_CLEAR_RESULTS,
  PROD_FLAT_PRICE_CONV_DELETE,
  PROD_FLAT_PRICE_CONV_EDIT,
  PROD_FLAT_PRICE_CONV_PDF,
  PROD_FLAT_PRICE_CONV_PRINT,
  PROD_FLAT_PRICE_CONV_SAVE,
  PROD_FLAT_PRICE_CONV_SAVE_NEW_ROW,
  PROD_FLAT_PRICE_CONV_SEARCH,
  PROD_FLAT_PRICE_CONV_XLS,
  PROD_MPS_CONV,
  PROD_MPS_CONV_ADD_NEW_ROW,
  PROD_MPS_CONV_BACK,
  PROD_MPS_CONV_CSV,
  PROD_MPS_CONV_CANCEL,
  PROD_MPS_CONV_DELETE,
  PROD_MPS_CONV_EDIT,
  PROD_MPS_CONV_PDF,
  PROD_MPS_CONV_PRINT,
  PROD_MPS_CONV_SAVE,
  PROD_MPS_CONV_SAVE_NEW_ROW,
  PROD_MPS_CONV_SEARCH,
  PROD_MPS_CONV_XLS,
  PROD_SORT_CODE,
  PROD_SORT_CODE_ADD_NEW_ROW,
  PROD_SORT_CODE_BACK,
  PROD_SORT_CODE_CSV,
  PROD_SORT_CODE_CANCEL,
  PROD_SORT_CODE_DELETE,
  PROD_SORT_CODE_PDF,
  PROD_SORT_CODE_PRINT,
  PROD_SORT_CODE_SAVE,
  PROD_SORT_CODE_SAVE_NEW_ROW,
  PROD_SORT_CODE_SEARCH,
  PROD_SORT_CODE_XLS,
  R06,
  R06_ADD,
  R06_BACK,
  R06_CSV,
  R06_CLEAR,
  R06_GENERATE_REPORT,
  R06_PDF,
  R06_XLS,
  R06_REPORT,
  R07,
  R07_BACK,
  R07_CSV,
  R07_CLEAR,
  R07_GENERATE_REPORT,
  R07_PDF,
  R07_XLS,
  R07_REPORT,
  R08,
  R08_BACK,
  R08_CSV,
  R08_CLEAR,
  R08_GENERATE_REPORT,
  R08_PDF,
  R08_XLS,
  R08_REPORT,
  R10,
  R10_BACK,
  R10_CSV,
  R10_CLEAR,
  R10_GENERATE_REPORT,
  R10_PDF,
  R10_XLS,
  R10_REPORT,
  R11,
  R11_BACK,
  R11_CSV,
  R11_CLEAR,
  R11_GENERATE_REPORT,
  R11_PDF,
  R11_XLS,
  R11_REPORT,
  R12,
  R12_BACK,
  R12_CSV,
  R12_CLEAR,
  R12_GENERATE_REPORT,
  R12_PDF,
  R12_XLS,
  R12_REPORT,
  R13,
  R13_BACK,
  R13_CSV,
  R13_CLEAR,
  R13_GENERATE_REPORT,
  R13_PDF,
  R13_XLS,
  R13_REPORT,
  R14,
  R14_BACK,
  R14_CSV,
  R14_CLEAR,
  R14_GENERATE_REPORT,
  R14_PDF,
  R14_SEARCH,
  R14_XLS,
  R14_REPORT,
  REPORTS,
  SEARCH,
  SEARCH_BACK,
  SEARCH_CLEAR,
  SEARCH_DETAILS,
  SEARCH_SEARCH,
  TABLE_MAINTENANCE,
  SUBMISSION_UPLOAD,
  SUBMISSION_HISTORY,
]);

const APPROVE_ACTIONS = new Set([
  INVOICE_SEARCH,
  INVOICE_SEARCH_SEARCH,
  LOG_SOURCE_CODE,
  INBOX,
  INBOX_BACK,
  INBOX_CLEAR,
  INBOX_SEARCH,
  INVOICE_DETAILS,
  INVOICE_DETAILS_APPROVE,
  INVOICE_DETAILS_BACK,
  INVOICE_DETAILS_CSV,
  INVOICE_DETAILS_CANCEL,
  INVOICE_DETAILS_DELETE,
  INVOICE_DETAILS_DUPLICATE,
  INVOICE_DETAILS_NEW_LINE_ITEM,
  INVOICE_DETAILS_PDF,
  INVOICE_DETAILS_PRINTS,
  INVOICE_DETAILS_REJECT,
  INVOICE_DETAILS_SAVE,
  INVOICE_DETAILS_SAVE_INVOICE,
  INVOICE_DETAILS_SUBMIT,
  INVOICE_DETAILS_UNAPPROVE,
  INVOICE_DETAILS_UPDATE_GROUP,
  INVOICE_DETAILS_XLS,
  MODEL_FLAT_PRICE_CONV,
  MODEL_FLAT_PRICE_CONV_BACK,
  MODEL_FLAT_PRICE_CONV_CSV,
  MODEL_FLAT_PRICE_CONV_CANCEL,
  MODEL_FLAT_PRICE_CONV_CLEAR,
  MODEL_FLAT_PRICE_CONV_CLEAR_FILTERS,
  MODEL_FLAT_PRICE_CONV_CLEAR_RESULTS,
  MODEL_FLAT_PRICE_CONV_PDF,
  MODEL_FLAT_PRICE_CONV_SEARCH,
  MODEL_FLAT_PRICE_CONV_XLS,
  MODEL_MPS_CONV,
  MODEL_MPS_CONV_BACK,
  MODEL_MPS_CONV_CSV,
  MODEL_MPS_CONV_CANCEL,
  MODEL_MPS_CONV_PDF,
  MODEL_MPS_CONV_SEARCH,
  MODEL_MPS_CONV_XLS,
  PROD_FLAT_PRICE_CONV,
  PROD_FLAT_PRICE_CONV_BACK,
  PROD_FLAT_PRICE_CONV_CSV,
  PROD_FLAT_PRICE_CONV_CANCEL,
  PROD_FLAT_PRICE_CONV_CLEAR,
  PROD_FLAT_PRICE_CONV_CLEAR_FILTERS,
  PROD_FLAT_PRICE_CONV_CLEAR_RESULTS,
  PROD_FLAT_PRICE_CONV_PDF,
  PROD_FLAT_PRICE_CONV_SEARCH,
  PROD_FLAT_PRICE_CONV_XLS,
  PROD_MPS_CONV,
  PROD_MPS_CONV_BACK,
  PROD_MPS_CONV_CSV,
  PROD_MPS_CONV_CANCEL,
  PROD_MPS_CONV_PDF,
  PROD_MPS_CONV_SEARCH,
  PROD_MPS_CONV_XLS,
  PROD_SORT_CODE,
  PROD_SORT_CODE_BACK,
  PROD_SORT_CODE_CSV,
  PROD_SORT_CODE_CANCEL,
  PROD_SORT_CODE_PDF,
  PROD_SORT_CODE_SEARCH,
  PROD_SORT_CODE_XLS,
  R06,
  R06_BACK,
  R06_CSV,
  R06_GENERATE_REPORT,
  R06_PDF,
  R06_XLS,
  R06_REPORT,
  R07,
  R07_BACK,
  R07_CSV,
  R07_GENERATE_REPORT,
  R07_PDF,
  R07_XLS,
  R07_REPORT,
  R08,
  R08_BACK,
  R08_CSV,
  R08_GENERATE_REPORT,
  R08_PDF,
  R08_XLS,
  R08_REPORT,
  R10,
  R10_BACK,
  R10_CSV,
  R10_GENERATE_REPORT,
  R10_PDF,
  R10_XLS,
  R10_REPORT,
  R11,
  R11_BACK,
  R11_CSV,
  R11_GENERATE_REPORT,
  R11_PDF,
  R11_XLS,
  R11_REPORT,
  R12,
  R12_BACK,
  R12_CSV,
  R12_GENERATE_REPORT,
  R12_PDF,
  R12_XLS,
  R12_REPORT,
  R13,
  R13_BACK,
  R13_CSV,
  R13_CLEAR,
  R13_GENERATE_REPORT,
  R13_PDF,
  R13_XLS,
  R13_REPORT,
  R14,
  R14_BACK,
  R14_CSV,
  R14_GENERATE_REPORT,
  R14_PDF,
  R14_XLS,
  R14_REPORT,
  REPORTS,
  SEARCH,
  SEARCH_BACK,
  SEARCH_CLEAR,
  SEARCH_DETAILS,
  SEARCH_SEARCH,
  SUBMISSION_UPLOAD,
  SUBMISSION_HISTORY,
]);

const VIEW_ACTIONS = new Set([
  INVOICE_SEARCH,
  INVOICE_SEARCH_SEARCH,
  LOG_SOURCE_CODE,
  INBOX,
  INBOX_BACK,
  INBOX_CLEAR,
  INBOX_SEARCH,
  INVOICE_DETAILS,
  INVOICE_DETAILS_BACK,
  INVOICE_DETAILS_CSV,
  INVOICE_DETAILS_PDF,
  INVOICE_DETAILS_PRINTS,
  INVOICE_DETAILS_XLS,
  MODEL_FLAT_PRICE_CONV,
  MODEL_FLAT_PRICE_CONV_BACK,
  MODEL_FLAT_PRICE_CONV_CSV,
  MODEL_FLAT_PRICE_CONV_CANCEL,
  MODEL_FLAT_PRICE_CONV_CLEAR,
  MODEL_FLAT_PRICE_CONV_CLEAR_FILTERS,
  MODEL_FLAT_PRICE_CONV_CLEAR_RESULTS,
  MODEL_FLAT_PRICE_CONV_PDF,
  MODEL_FLAT_PRICE_CONV_SEARCH,
  MODEL_FLAT_PRICE_CONV_XLS,
  MODEL_MPS_CONV,
  MODEL_MPS_CONV_BACK,
  MODEL_MPS_CONV_CSV,
  MODEL_MPS_CONV_CANCEL,
  MODEL_MPS_CONV_PDF,
  MODEL_MPS_CONV_SEARCH,
  MODEL_MPS_CONV_XLS,
  PROD_FLAT_PRICE_CONV,
  PROD_FLAT_PRICE_CONV_BACK,
  PROD_FLAT_PRICE_CONV_CSV,
  PROD_FLAT_PRICE_CONV_CANCEL,
  PROD_FLAT_PRICE_CONV_CLEAR,
  PROD_FLAT_PRICE_CONV_CLEAR_FILTERS,
  PROD_FLAT_PRICE_CONV_CLEAR_RESULTS,
  PROD_FLAT_PRICE_CONV_PDF,
  PROD_FLAT_PRICE_CONV_SEARCH,
  PROD_FLAT_PRICE_CONV_XLS,
  PROD_MPS_CONV,
  PROD_MPS_CONV_BACK,
  PROD_MPS_CONV_CSV,
  PROD_MPS_CONV_CANCEL,
  PROD_MPS_CONV_PDF,
  PROD_MPS_CONV_SEARCH,
  PROD_MPS_CONV_XLS,
  PROD_SORT_CODE,
  PROD_SORT_CODE_BACK,
  PROD_SORT_CODE_CSV,
  PROD_SORT_CODE_CANCEL,
  PROD_SORT_CODE_PDF,
  PROD_SORT_CODE_SEARCH,
  PROD_SORT_CODE_XLS,
  R06,
  R06_BACK,
  R06_CSV,
  R06_GENERATE_REPORT,
  R06_PDF,
  R06_XLS,
  R06_REPORT,
  R07,
  R07_BACK,
  R07_CSV,
  R07_GENERATE_REPORT,
  R07_PDF,
  R07_XLS,
  R07_REPORT,
  R08,
  R08_BACK,
  R08_CSV,
  R08_GENERATE_REPORT,
  R08_PDF,
  R08_XLS,
  R08_REPORT,
  R10,
  R10_BACK,
  R10_CSV,
  R10_GENERATE_REPORT,
  R10_PDF,
  R10_XLS,
  R10_REPORT,
  R11,
  R11_BACK,
  R11_CSV,
  R11_GENERATE_REPORT,
  R11_PDF,
  R11_XLS,
  R11_REPORT,
  R12,
  R12_BACK,
  R12_CSV,
  R12_GENERATE_REPORT,
  R12_PDF,
  R12_XLS,
  R12_REPORT,
  R13,
  R13_BACK,
  R13_CSV,
  R13_CLEAR,
  R13_GENERATE_REPORT,
  R13_PDF,
  R13_XLS,
  R13_REPORT,
  R14,
  R14_BACK,
  R14_CSV,
  R14_GENERATE_REPORT,
  R14_PDF,
  R14_XLS,
  R14_REPORT,
  REPORTS,
  SEARCH,
  SEARCH_BACK,
  SEARCH_CLEAR,
  SEARCH_DETAILS,
  SEARCH_SEARCH,
  SUBMISSION_UPLOAD,
  SUBMISSION_HISTORY,
]);

/**
 * SUBMITTER is the Business BCeID industry role: upload XML submissions and see
 * their history, nothing else, and only for the forest clients it holds.
 */
const SUBMITTER_ACTIONS = new Set([SUBMISSION_UPLOAD, SUBMISSION_HISTORY]);

/** The CSP role constants. Must match backend {@code Roles.java}. */
export const ROLES = ['ADMIN', 'APPROVE', 'VIEW', 'SUBMITTER'] as const;
export type Role = (typeof ROLES)[number]; // 'ADMIN' | 'APPROVE' | 'VIEW' | 'SUBMITTER'

/** The IDIR staff roles; SUBMITTER is the only industry role. */
export const STAFF_ROLES: readonly Role[] = ['ADMIN', 'APPROVE', 'VIEW'];

/** Maps a role constant to its permitted action strings. */
export const ROLE_PERMISSIONS: Record<Role, Set<string>> = {
  ADMIN: ADMIN_ACTIONS,
  APPROVE: APPROVE_ACTIONS,
  VIEW: VIEW_ACTIONS,
  SUBMITTER: SUBMITTER_ACTIONS,
};
