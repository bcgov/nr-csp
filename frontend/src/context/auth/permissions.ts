/**
 * Action-level permission constants for CSP.
 *
 * Action strings are identical to the Java PermissionConstants values.
 * Keep in sync with:
 *   backend/src/main/java/ca/bc/gov/nrs/csp/backend/util/constants/PermissionConstants.java
 */

// ── InvoiceSearch ─────────────────────────────────────────────────────────────
export const INVOICE_SEARCH = 'InvoiceSearch';
export const INVOICE_SEARCH_SEARCH = 'InvoiceSearch/Search';

// ── LogSourceCode ─────────────────────────────────────────────────────────────
export const LOG_SOURCE_CODE = 'LogSourceCode';

// ── esfSubmit (ADMIN only) ────────────────────────────────────────────────────
export const ESF_SUBMIT = 'esfSubmit';

// ── inbox ─────────────────────────────────────────────────────────────────────
export const INBOX = 'inbox';
export const INBOX_BACK = 'inbox/Back';
export const INBOX_CLEAR = 'inbox/Clear';
export const INBOX_SEARCH = 'inbox/Search';

// ── invoiceDetails ────────────────────────────────────────────────────────────
export const INVOICE_DETAILS = 'invoiceDetails';
export const INVOICE_DETAILS_APPROVE = 'invoiceDetails/Approve';
export const INVOICE_DETAILS_BACK = 'invoiceDetails/Back';
export const INVOICE_DETAILS_CSV = 'invoiceDetails/CSV';
export const INVOICE_DETAILS_CANCEL = 'invoiceDetails/Cancel';
export const INVOICE_DETAILS_DELETE = 'invoiceDetails/Delete';
export const INVOICE_DETAILS_DUPLICATE = 'invoiceDetails/Duplicate';
export const INVOICE_DETAILS_NEW_LINE_ITEM = 'invoiceDetails/New Line Item';
export const INVOICE_DETAILS_PDF = 'invoiceDetails/PDF';
export const INVOICE_DETAILS_PRINTS = 'invoiceDetails/Prints';
export const INVOICE_DETAILS_REJECT = 'invoiceDetails/Reject';
export const INVOICE_DETAILS_SAVE = 'invoiceDetails/Save';
export const INVOICE_DETAILS_SAVE_INVOICE = 'invoiceDetails/Save Invoice';
export const INVOICE_DETAILS_SUBMIT = 'invoiceDetails/Submit';
export const INVOICE_DETAILS_UNAPPROVE = 'invoiceDetails/Unapprove';
export const INVOICE_DETAILS_UPDATE_GROUP = 'invoiceDetails/Update Group';
export const INVOICE_DETAILS_XLS = 'invoiceDetails/XLS';

// ── modelFlatPriceConv ────────────────────────────────────────────────────────
export const MODEL_FLAT_PRICE_CONV = 'modelFlatPriceConv';
export const MODEL_FLAT_PRICE_CONV_ADD_NEW_ROW = 'modelFlatPriceConv/Add New Row';
export const MODEL_FLAT_PRICE_CONV_BACK = 'modelFlatPriceConv/Back';
export const MODEL_FLAT_PRICE_CONV_CSV = 'modelFlatPriceConv/CSV';
export const MODEL_FLAT_PRICE_CONV_CANCEL = 'modelFlatPriceConv/Cancel';
export const MODEL_FLAT_PRICE_CONV_CLEAR = 'modelFlatPriceConv/Clear';
export const MODEL_FLAT_PRICE_CONV_CLEAR_ALL_CREATE_NEW = 'modelFlatPriceConv/Clear All/Create New';
export const MODEL_FLAT_PRICE_CONV_CLEAR_FILTERS = 'modelFlatPriceConv/Clear Filters';
export const MODEL_FLAT_PRICE_CONV_CLEAR_RESULTS = 'modelFlatPriceConv/Clear Results';
export const MODEL_FLAT_PRICE_CONV_COPY_DATA = 'modelFlatPriceConv/Copy Data';
export const MODEL_FLAT_PRICE_CONV_DELETE = 'modelFlatPriceConv/Delete';
export const MODEL_FLAT_PRICE_CONV_EDIT = 'modelFlatPriceConv/Edit';
export const MODEL_FLAT_PRICE_CONV_PDF = 'modelFlatPriceConv/PDF';
export const MODEL_FLAT_PRICE_CONV_PRINT = 'modelFlatPriceConv/Print';
export const MODEL_FLAT_PRICE_CONV_SAVE = 'modelFlatPriceConv/Save';
export const MODEL_FLAT_PRICE_CONV_SAVE_NEW_ROW = 'modelFlatPriceConv/Save New Row';
export const MODEL_FLAT_PRICE_CONV_SEARCH = 'modelFlatPriceConv/Search';
export const MODEL_FLAT_PRICE_CONV_XLS = 'modelFlatPriceConv/XLS';

// ── modelMPSConv ──────────────────────────────────────────────────────────────
export const MODEL_MPS_CONV = 'modelMPSConv';
export const MODEL_MPS_CONV_ADD_NEW_ROW = 'modelMPSConv/Add New Row';
export const MODEL_MPS_CONV_BACK = 'modelMPSConv/Back';
export const MODEL_MPS_CONV_CSV = 'modelMPSConv/CSV';
export const MODEL_MPS_CONV_CANCEL = 'modelMPSConv/Cancel';
export const MODEL_MPS_CONV_CLEAR_ALL_CREATE_NEW = 'modelMPSConv/Clear All/Create New';
export const MODEL_MPS_CONV_COPY_DATA = 'modelMPSConv/Copy Data';
export const MODEL_MPS_CONV_DELETE = 'modelMPSConv/Delete';
export const MODEL_MPS_CONV_EDIT = 'modelMPSConv/Edit';
export const MODEL_MPS_CONV_PDF = 'modelMPSConv/PDF';
export const MODEL_MPS_CONV_PRINT = 'modelMPSConv/Print';
export const MODEL_MPS_CONV_SAVE = 'modelMPSConv/Save';
export const MODEL_MPS_CONV_SAVE_NEW_ROW = 'modelMPSConv/Save New Row';
export const MODEL_MPS_CONV_SEARCH = 'modelMPSConv/Search';
export const MODEL_MPS_CONV_XLS = 'modelMPSConv/XLS';

// ── prodFlatPriceConv ─────────────────────────────────────────────────────────
export const PROD_FLAT_PRICE_CONV = 'prodFlatPriceConv';
export const PROD_FLAT_PRICE_CONV_ADD_NEW_ROW = 'prodFlatPriceConv/Add New Row';
export const PROD_FLAT_PRICE_CONV_BACK = 'prodFlatPriceConv/Back';
export const PROD_FLAT_PRICE_CONV_CSV = 'prodFlatPriceConv/CSV';
export const PROD_FLAT_PRICE_CONV_CANCEL = 'prodFlatPriceConv/Cancel';
export const PROD_FLAT_PRICE_CONV_CLEAR = 'prodFlatPriceConv/Clear';
export const PROD_FLAT_PRICE_CONV_CLEAR_FILTERS = 'prodFlatPriceConv/Clear Filters';
export const PROD_FLAT_PRICE_CONV_CLEAR_RESULTS = 'prodFlatPriceConv/Clear Results';
export const PROD_FLAT_PRICE_CONV_DELETE = 'prodFlatPriceConv/Delete';
export const PROD_FLAT_PRICE_CONV_EDIT = 'prodFlatPriceConv/Edit';
export const PROD_FLAT_PRICE_CONV_PDF = 'prodFlatPriceConv/PDF';
export const PROD_FLAT_PRICE_CONV_PRINT = 'prodFlatPriceConv/Print';
export const PROD_FLAT_PRICE_CONV_SAVE = 'prodFlatPriceConv/Save';
export const PROD_FLAT_PRICE_CONV_SAVE_NEW_ROW = 'prodFlatPriceConv/Save New Row';
export const PROD_FLAT_PRICE_CONV_SEARCH = 'prodFlatPriceConv/Search';
export const PROD_FLAT_PRICE_CONV_XLS = 'prodFlatPriceConv/XLS';

// ── prodMPSConv ───────────────────────────────────────────────────────────────
export const PROD_MPS_CONV = 'prodMPSConv';
export const PROD_MPS_CONV_ADD_NEW_ROW = 'prodMPSConv/Add New Row';
export const PROD_MPS_CONV_BACK = 'prodMPSConv/Back';
export const PROD_MPS_CONV_CSV = 'prodMPSConv/CSV';
export const PROD_MPS_CONV_CANCEL = 'prodMPSConv/Cancel';
export const PROD_MPS_CONV_DELETE = 'prodMPSConv/Delete';
export const PROD_MPS_CONV_EDIT = 'prodMPSConv/Edit';
export const PROD_MPS_CONV_PDF = 'prodMPSConv/PDF';
export const PROD_MPS_CONV_PRINT = 'prodMPSConv/Print';
export const PROD_MPS_CONV_SAVE = 'prodMPSConv/Save';
export const PROD_MPS_CONV_SAVE_NEW_ROW = 'prodMPSConv/Save New Row';
export const PROD_MPS_CONV_SEARCH = 'prodMPSConv/Search';
export const PROD_MPS_CONV_XLS = 'prodMPSConv/XLS';

// ── prodSortCode ──────────────────────────────────────────────────────────────
export const PROD_SORT_CODE = 'prodSortCode';
export const PROD_SORT_CODE_ADD_NEW_ROW = 'prodSortCode/Add New Row';
export const PROD_SORT_CODE_BACK = 'prodSortCode/Back';
export const PROD_SORT_CODE_CSV = 'prodSortCode/CSV';
export const PROD_SORT_CODE_CANCEL = 'prodSortCode/Cancel';
export const PROD_SORT_CODE_DELETE = 'prodSortCode/Delete';
export const PROD_SORT_CODE_PDF = 'prodSortCode/PDF';
export const PROD_SORT_CODE_PRINT = 'prodSortCode/Print';
export const PROD_SORT_CODE_SAVE = 'prodSortCode/Save';
export const PROD_SORT_CODE_SAVE_NEW_ROW = 'prodSortCode/Save New Row';
export const PROD_SORT_CODE_SEARCH = 'prodSortCode/Search';
export const PROD_SORT_CODE_XLS = 'prodSortCode/XLS';

// ── r06 ───────────────────────────────────────────────────────────────────────
export const R06 = 'r06';
export const R06_ADD = 'r06/Add';
export const R06_BACK = 'r06/Back';
export const R06_CSV = 'r06/CSV';
export const R06_CLEAR = 'r06/Clear';
export const R06_GENERATE_REPORT = 'r06/Generate Report';
export const R06_PDF = 'r06/PDF';
export const R06_XLS = 'r06/XLS';
export const R06_REPORT = 'r06/r06Report';

// ── r07 ───────────────────────────────────────────────────────────────────────
export const R07 = 'r07';
export const R07_BACK = 'r07/Back';
export const R07_CSV = 'r07/CSV';
export const R07_CLEAR = 'r07/Clear';
export const R07_GENERATE_REPORT = 'r07/Generate Report';
export const R07_PDF = 'r07/PDF';
export const R07_XLS = 'r07/XLS';
export const R07_REPORT = 'r07/r07Report';

// ── r08 ───────────────────────────────────────────────────────────────────────
export const R08 = 'r08';
export const R08_BACK = 'r08/Back';
export const R08_CSV = 'r08/CSV';
export const R08_CLEAR = 'r08/Clear';
export const R08_GENERATE_REPORT = 'r08/Generate Report';
export const R08_PDF = 'r08/PDF';
export const R08_XLS = 'r08/XLS';
export const R08_REPORT = 'r08/r08Report';

// ── r10 ───────────────────────────────────────────────────────────────────────
export const R10 = 'r10';
export const R10_BACK = 'r10/Back';
export const R10_CSV = 'r10/CSV';
export const R10_CLEAR = 'r10/Clear';
export const R10_GENERATE_REPORT = 'r10/Generate Report';
export const R10_PDF = 'r10/PDF';
export const R10_XLS = 'r10/XLS';
export const R10_REPORT = 'r10/r10Report';

// ── r11 ───────────────────────────────────────────────────────────────────────
export const R11 = 'r11';
export const R11_BACK = 'r11/Back';
export const R11_CSV = 'r11/CSV';
export const R11_CLEAR = 'r11/Clear';
export const R11_GENERATE_REPORT = 'r11/Generate Report';
export const R11_PDF = 'r11/PDF';
export const R11_XLS = 'r11/XLS';
export const R11_REPORT = 'r11/r11Report';

// ── r12 ───────────────────────────────────────────────────────────────────────
export const R12 = 'r12';
export const R12_BACK = 'r12/Back';
export const R12_CSV = 'r12/CSV';
export const R12_CLEAR = 'r12/Clear';
export const R12_GENERATE_REPORT = 'r12/Generate Report';
export const R12_PDF = 'r12/PDF';
export const R12_XLS = 'r12/XLS';
export const R12_REPORT = 'r12/r12Report';

// ── r13 ───────────────────────────────────────────────────────────────────────
export const R13 = 'r13';
export const R13_BACK = 'r13/Back';
export const R13_CSV = 'r13/CSV';
export const R13_CLEAR = 'r13/Clear';
export const R13_GENERATE_REPORT = 'r13/Generate Report';
export const R13_PDF = 'r13/PDF';
export const R13_XLS = 'r13/XLS';
export const R13_REPORT = 'r13/r13Report';

// ── r14 ───────────────────────────────────────────────────────────────────────
export const R14 = 'r14';
export const R14_BACK = 'r14/Back';
export const R14_CSV = 'r14/CSV';
export const R14_CLEAR = 'r14/Clear';
export const R14_GENERATE_REPORT = 'r14/Generate Report';
export const R14_PDF = 'r14/PDF';
export const R14_SEARCH = 'r14/Search';
export const R14_XLS = 'r14/XLS';
export const R14_REPORT = 'r14/r14Report';

// ── reports ───────────────────────────────────────────────────────────────────
export const REPORTS = 'reports';

// ── search ────────────────────────────────────────────────────────────────────
export const SEARCH = 'search';
export const SEARCH_BACK = 'search/Back';
export const SEARCH_CLEAR = 'search/Clear';
export const SEARCH_DETAILS = 'search/Details';
export const SEARCH_SEARCH = 'search/Search';

// ── tableMaintenance (ADMIN only) ─────────────────────────────────────────────
export const TABLE_MAINTENANCE = 'tableMaintenance';

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
]);

/** The three CSP role constants. Must match backend {@code Roles.java}. */
export const ROLES = ['ADMIN', 'APPROVE', 'VIEW'] as const;
export type Role = (typeof ROLES)[number]; // 'ADMIN' | 'APPROVE' | 'VIEW'

/** Maps a role constant to its permitted action strings. */
export const ROLE_PERMISSIONS: Record<Role, Set<string>> = {
  ADMIN: ADMIN_ACTIONS,
  APPROVE: APPROVE_ACTIONS,
  VIEW: VIEW_ACTIONS,
};
