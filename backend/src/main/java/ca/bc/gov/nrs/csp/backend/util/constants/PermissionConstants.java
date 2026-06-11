package ca.bc.gov.nrs.csp.backend.util.constants;

import java.util.Map;
import java.util.Set;

/**
 * Action-level permission constants for CSP.
 *
 * <p>Action strings use the exact names from the FAM permission matrix:
 * <ul>
 *   <li>Page actions (top-level): {@code "invoiceDetails"}, {@code "inbox"}, etc.</li>
 *   <li>Sub-actions (slash-separated): {@code "invoiceDetails/Approve"}, etc.</li>
 * </ul>
 *
 * <p>Keep in sync with the frontend {@code permissions.ts} file.
 */
public final class PermissionConstants {

    private PermissionConstants() {}

    // ── InvoiceSearch ─────────────────────────────────────────────────────────
    public static final String INVOICE_SEARCH        = "InvoiceSearch";
    public static final String INVOICE_SEARCH_SEARCH = "InvoiceSearch/Search";

    // ── LogSourceCode ─────────────────────────────────────────────────────────
    public static final String LOG_SOURCE_CODE       = "LogSourceCode";

    // ── esfSubmit (ADMIN only) ────────────────────────────────────────────────
    public static final String ESF_SUBMIT            = "esfSubmit";

    // ── inbox ─────────────────────────────────────────────────────────────────
    public static final String INBOX                 = "inbox";
    public static final String INBOX_BACK            = "inbox/Back";
    public static final String INBOX_CLEAR           = "inbox/Clear";
    public static final String INBOX_SEARCH          = "inbox/Search";

    // ── invoiceDetails ────────────────────────────────────────────────────────
    public static final String INVOICE_DETAILS                = "invoiceDetails";
    public static final String INVOICE_DETAILS_APPROVE        = "invoiceDetails/Approve";
    public static final String INVOICE_DETAILS_BACK           = "invoiceDetails/Back";
    public static final String INVOICE_DETAILS_CSV            = "invoiceDetails/CSV";
    public static final String INVOICE_DETAILS_CANCEL         = "invoiceDetails/Cancel";
    public static final String INVOICE_DETAILS_DELETE         = "invoiceDetails/Delete";
    public static final String INVOICE_DETAILS_DUPLICATE      = "invoiceDetails/Duplicate";
    public static final String INVOICE_DETAILS_NEW_LINE_ITEM  = "invoiceDetails/New Line Item";
    public static final String INVOICE_DETAILS_PDF            = "invoiceDetails/PDF";
    public static final String INVOICE_DETAILS_PRINTS         = "invoiceDetails/Prints";
    public static final String INVOICE_DETAILS_REJECT         = "invoiceDetails/Reject";
    public static final String INVOICE_DETAILS_SAVE           = "invoiceDetails/Save";
    public static final String INVOICE_DETAILS_SAVE_INVOICE   = "invoiceDetails/Save Invoice";
    public static final String INVOICE_DETAILS_SUBMIT         = "invoiceDetails/Submit";
    public static final String INVOICE_DETAILS_UNAPPROVE      = "invoiceDetails/Unapprove";
    public static final String INVOICE_DETAILS_UPDATE_GROUP   = "invoiceDetails/Update Group";
    public static final String INVOICE_DETAILS_XLS            = "invoiceDetails/XLS";

    // ── modelFlatPriceConv ────────────────────────────────────────────────────
    public static final String MODEL_FLAT_PRICE_CONV                      = "modelFlatPriceConv";
    public static final String MODEL_FLAT_PRICE_CONV_ADD_NEW_ROW          = "modelFlatPriceConv/Add New Row";
    public static final String MODEL_FLAT_PRICE_CONV_BACK                 = "modelFlatPriceConv/Back";
    public static final String MODEL_FLAT_PRICE_CONV_CSV                  = "modelFlatPriceConv/CSV";
    public static final String MODEL_FLAT_PRICE_CONV_CANCEL               = "modelFlatPriceConv/Cancel";
    public static final String MODEL_FLAT_PRICE_CONV_CLEAR                = "modelFlatPriceConv/Clear";
    public static final String MODEL_FLAT_PRICE_CONV_CLEAR_ALL_CREATE_NEW = "modelFlatPriceConv/Clear All/Create New";
    public static final String MODEL_FLAT_PRICE_CONV_CLEAR_FILTERS        = "modelFlatPriceConv/Clear Filters";
    public static final String MODEL_FLAT_PRICE_CONV_CLEAR_RESULTS        = "modelFlatPriceConv/Clear Results";
    public static final String MODEL_FLAT_PRICE_CONV_COPY_DATA            = "modelFlatPriceConv/Copy Data";
    public static final String MODEL_FLAT_PRICE_CONV_DELETE               = "modelFlatPriceConv/Delete";
    public static final String MODEL_FLAT_PRICE_CONV_EDIT                 = "modelFlatPriceConv/Edit";
    public static final String MODEL_FLAT_PRICE_CONV_PDF                  = "modelFlatPriceConv/PDF";
    public static final String MODEL_FLAT_PRICE_CONV_PRINT                = "modelFlatPriceConv/Print";
    public static final String MODEL_FLAT_PRICE_CONV_SAVE                 = "modelFlatPriceConv/Save";
    public static final String MODEL_FLAT_PRICE_CONV_SAVE_NEW_ROW         = "modelFlatPriceConv/Save New Row";
    public static final String MODEL_FLAT_PRICE_CONV_SEARCH               = "modelFlatPriceConv/Search";
    public static final String MODEL_FLAT_PRICE_CONV_XLS                  = "modelFlatPriceConv/XLS";

    // ── modelMPSConv ──────────────────────────────────────────────────────────
    public static final String MODEL_MPS_CONV                      = "modelMPSConv";
    public static final String MODEL_MPS_CONV_ADD_NEW_ROW          = "modelMPSConv/Add New Row";
    public static final String MODEL_MPS_CONV_BACK                 = "modelMPSConv/Back";
    public static final String MODEL_MPS_CONV_CSV                  = "modelMPSConv/CSV";
    public static final String MODEL_MPS_CONV_CANCEL               = "modelMPSConv/Cancel";
    public static final String MODEL_MPS_CONV_CLEAR_ALL_CREATE_NEW = "modelMPSConv/Clear All/Create New";
    public static final String MODEL_MPS_CONV_COPY_DATA            = "modelMPSConv/Copy Data";
    public static final String MODEL_MPS_CONV_DELETE               = "modelMPSConv/Delete";
    public static final String MODEL_MPS_CONV_EDIT                 = "modelMPSConv/Edit";
    public static final String MODEL_MPS_CONV_PDF                  = "modelMPSConv/PDF";
    public static final String MODEL_MPS_CONV_PRINT                = "modelMPSConv/Print";
    public static final String MODEL_MPS_CONV_SAVE                 = "modelMPSConv/Save";
    public static final String MODEL_MPS_CONV_SAVE_NEW_ROW         = "modelMPSConv/Save New Row";
    public static final String MODEL_MPS_CONV_SEARCH               = "modelMPSConv/Search";
    public static final String MODEL_MPS_CONV_XLS                  = "modelMPSConv/XLS";

    // ── prodFlatPriceConv ─────────────────────────────────────────────────────
    public static final String PROD_FLAT_PRICE_CONV               = "prodFlatPriceConv";
    public static final String PROD_FLAT_PRICE_CONV_ADD_NEW_ROW   = "prodFlatPriceConv/Add New Row";
    public static final String PROD_FLAT_PRICE_CONV_BACK          = "prodFlatPriceConv/Back";
    public static final String PROD_FLAT_PRICE_CONV_CSV           = "prodFlatPriceConv/CSV";
    public static final String PROD_FLAT_PRICE_CONV_CANCEL        = "prodFlatPriceConv/Cancel";
    public static final String PROD_FLAT_PRICE_CONV_CLEAR         = "prodFlatPriceConv/Clear";
    public static final String PROD_FLAT_PRICE_CONV_CLEAR_FILTERS = "prodFlatPriceConv/Clear Filters";
    public static final String PROD_FLAT_PRICE_CONV_CLEAR_RESULTS = "prodFlatPriceConv/Clear Results";
    public static final String PROD_FLAT_PRICE_CONV_DELETE        = "prodFlatPriceConv/Delete";
    public static final String PROD_FLAT_PRICE_CONV_EDIT          = "prodFlatPriceConv/Edit";
    public static final String PROD_FLAT_PRICE_CONV_PDF           = "prodFlatPriceConv/PDF";
    public static final String PROD_FLAT_PRICE_CONV_PRINT         = "prodFlatPriceConv/Print";
    public static final String PROD_FLAT_PRICE_CONV_SAVE          = "prodFlatPriceConv/Save";
    public static final String PROD_FLAT_PRICE_CONV_SAVE_NEW_ROW  = "prodFlatPriceConv/Save New Row";
    public static final String PROD_FLAT_PRICE_CONV_SEARCH        = "prodFlatPriceConv/Search";
    public static final String PROD_FLAT_PRICE_CONV_XLS           = "prodFlatPriceConv/XLS";

    // ── prodMPSConv ───────────────────────────────────────────────────────────
    public static final String PROD_MPS_CONV              = "prodMPSConv";
    public static final String PROD_MPS_CONV_ADD_NEW_ROW  = "prodMPSConv/Add New Row";
    public static final String PROD_MPS_CONV_BACK         = "prodMPSConv/Back";
    public static final String PROD_MPS_CONV_CSV          = "prodMPSConv/CSV";
    public static final String PROD_MPS_CONV_CANCEL       = "prodMPSConv/Cancel";
    public static final String PROD_MPS_CONV_DELETE       = "prodMPSConv/Delete";
    public static final String PROD_MPS_CONV_EDIT         = "prodMPSConv/Edit";
    public static final String PROD_MPS_CONV_PDF          = "prodMPSConv/PDF";
    public static final String PROD_MPS_CONV_PRINT        = "prodMPSConv/Print";
    public static final String PROD_MPS_CONV_SAVE         = "prodMPSConv/Save";
    public static final String PROD_MPS_CONV_SAVE_NEW_ROW = "prodMPSConv/Save New Row";
    public static final String PROD_MPS_CONV_SEARCH       = "prodMPSConv/Search";
    public static final String PROD_MPS_CONV_XLS          = "prodMPSConv/XLS";

    // ── prodSortCode ──────────────────────────────────────────────────────────
    public static final String PROD_SORT_CODE              = "prodSortCode";
    public static final String PROD_SORT_CODE_ADD_NEW_ROW  = "prodSortCode/Add New Row";
    public static final String PROD_SORT_CODE_BACK         = "prodSortCode/Back";
    public static final String PROD_SORT_CODE_CSV          = "prodSortCode/CSV";
    public static final String PROD_SORT_CODE_CANCEL       = "prodSortCode/Cancel";
    public static final String PROD_SORT_CODE_DELETE       = "prodSortCode/Delete";
    public static final String PROD_SORT_CODE_PDF          = "prodSortCode/PDF";
    public static final String PROD_SORT_CODE_PRINT        = "prodSortCode/Print";
    public static final String PROD_SORT_CODE_SAVE         = "prodSortCode/Save";
    public static final String PROD_SORT_CODE_SAVE_NEW_ROW = "prodSortCode/Save New Row";
    public static final String PROD_SORT_CODE_SEARCH       = "prodSortCode/Search";
    public static final String PROD_SORT_CODE_XLS          = "prodSortCode/XLS";

    // ── r06 ───────────────────────────────────────────────────────────────────
    public static final String R06                = "r06";
    public static final String R06_ADD            = "r06/Add";
    public static final String R06_BACK           = "r06/Back";
    public static final String R06_CSV            = "r06/CSV";
    public static final String R06_CLEAR          = "r06/Clear";
    public static final String R06_GENERATE_REPORT = "r06/Generate Report";
    public static final String R06_PDF            = "r06/PDF";
    public static final String R06_XLS            = "r06/XLS";
    public static final String R06_REPORT         = "r06/r06Report";

    // ── r07 ───────────────────────────────────────────────────────────────────
    public static final String R07                 = "r07";
    public static final String R07_BACK            = "r07/Back";
    public static final String R07_CSV             = "r07/CSV";
    public static final String R07_CLEAR           = "r07/Clear";
    public static final String R07_GENERATE_REPORT = "r07/Generate Report";
    public static final String R07_PDF             = "r07/PDF";
    public static final String R07_XLS             = "r07/XLS";
    public static final String R07_REPORT          = "r07/r07Report";

    // ── r08 ───────────────────────────────────────────────────────────────────
    public static final String R08                 = "r08";
    public static final String R08_BACK            = "r08/Back";
    public static final String R08_CSV             = "r08/CSV";
    public static final String R08_CLEAR           = "r08/Clear";
    public static final String R08_GENERATE_REPORT = "r08/Generate Report";
    public static final String R08_PDF             = "r08/PDF";
    public static final String R08_XLS             = "r08/XLS";
    public static final String R08_REPORT          = "r08/r08Report";

    // ── r10 ───────────────────────────────────────────────────────────────────
    public static final String R10                 = "r10";
    public static final String R10_BACK            = "r10/Back";
    public static final String R10_CSV             = "r10/CSV";
    public static final String R10_CLEAR           = "r10/Clear";
    public static final String R10_GENERATE_REPORT = "r10/Generate Report";
    public static final String R10_PDF             = "r10/PDF";
    public static final String R10_XLS             = "r10/XLS";
    public static final String R10_REPORT          = "r10/r10Report";

    // ── r11 ───────────────────────────────────────────────────────────────────
    public static final String R11                 = "r11";
    public static final String R11_BACK            = "r11/Back";
    public static final String R11_CSV             = "r11/CSV";
    public static final String R11_CLEAR           = "r11/Clear";
    public static final String R11_GENERATE_REPORT = "r11/Generate Report";
    public static final String R11_PDF             = "r11/PDF";
    public static final String R11_XLS             = "r11/XLS";
    public static final String R11_REPORT          = "r11/r11Report";

    // ── r12 ───────────────────────────────────────────────────────────────────
    public static final String R12                 = "r12";
    public static final String R12_BACK            = "r12/Back";
    public static final String R12_CSV             = "r12/CSV";
    public static final String R12_CLEAR           = "r12/Clear";
    public static final String R12_GENERATE_REPORT = "r12/Generate Report";
    public static final String R12_PDF             = "r12/PDF";
    public static final String R12_XLS             = "r12/XLS";
    public static final String R12_REPORT          = "r12/r12Report";

    // ── r13 ───────────────────────────────────────────────────────────────────
    public static final String R13                 = "r13";
    public static final String R13_BACK            = "r13/Back";
    public static final String R13_CSV             = "r13/CSV";
    public static final String R13_CLEAR           = "r13/Clear";
    public static final String R13_GENERATE_REPORT = "r13/Generate Report";
    public static final String R13_PDF             = "r13/PDF";
    public static final String R13_XLS             = "r13/XLS";
    public static final String R13_REPORT          = "r13/r13Report";

    // ── r14 ───────────────────────────────────────────────────────────────────
    public static final String R14                 = "r14";
    public static final String R14_BACK            = "r14/Back";
    public static final String R14_CSV             = "r14/CSV";
    public static final String R14_CLEAR           = "r14/Clear";
    public static final String R14_GENERATE_REPORT = "r14/Generate Report";
    public static final String R14_PDF             = "r14/PDF";
    public static final String R14_SEARCH          = "r14/Search";
    public static final String R14_XLS             = "r14/XLS";
    public static final String R14_REPORT          = "r14/r14Report";

    // ── reports ───────────────────────────────────────────────────────────────
    public static final String REPORTS             = "reports";

    // ── search ────────────────────────────────────────────────────────────────
    public static final String SEARCH              = "search";
    public static final String SEARCH_BACK         = "search/Back";
    public static final String SEARCH_CLEAR        = "search/Clear";
    public static final String SEARCH_DETAILS      = "search/Details";
    public static final String SEARCH_SEARCH       = "search/Search";

    // ── tableMaintenance (ADMIN only) ─────────────────────────────────────────
    public static final String TABLE_MAINTENANCE   = "tableMaintenance";

    // ── Role permission sets ──────────────────────────────────────────────────

    public static final Set<String> ADMIN_ACTIONS = Set.of(
            INVOICE_SEARCH, INVOICE_SEARCH_SEARCH,
            LOG_SOURCE_CODE,
            ESF_SUBMIT,
            INBOX, INBOX_BACK, INBOX_CLEAR, INBOX_SEARCH,
            INVOICE_DETAILS, INVOICE_DETAILS_APPROVE, INVOICE_DETAILS_BACK,
            INVOICE_DETAILS_CSV, INVOICE_DETAILS_CANCEL, INVOICE_DETAILS_DELETE,
            INVOICE_DETAILS_DUPLICATE, INVOICE_DETAILS_NEW_LINE_ITEM, INVOICE_DETAILS_PDF,
            INVOICE_DETAILS_PRINTS, INVOICE_DETAILS_REJECT, INVOICE_DETAILS_SAVE,
            INVOICE_DETAILS_SAVE_INVOICE, INVOICE_DETAILS_SUBMIT, INVOICE_DETAILS_UNAPPROVE,
            INVOICE_DETAILS_UPDATE_GROUP, INVOICE_DETAILS_XLS,
            MODEL_FLAT_PRICE_CONV, MODEL_FLAT_PRICE_CONV_ADD_NEW_ROW, MODEL_FLAT_PRICE_CONV_BACK,
            MODEL_FLAT_PRICE_CONV_CSV, MODEL_FLAT_PRICE_CONV_CANCEL, MODEL_FLAT_PRICE_CONV_CLEAR,
            MODEL_FLAT_PRICE_CONV_CLEAR_ALL_CREATE_NEW, MODEL_FLAT_PRICE_CONV_CLEAR_FILTERS,
            MODEL_FLAT_PRICE_CONV_CLEAR_RESULTS, MODEL_FLAT_PRICE_CONV_COPY_DATA,
            MODEL_FLAT_PRICE_CONV_DELETE, MODEL_FLAT_PRICE_CONV_EDIT, MODEL_FLAT_PRICE_CONV_PDF,
            MODEL_FLAT_PRICE_CONV_PRINT, MODEL_FLAT_PRICE_CONV_SAVE,
            MODEL_FLAT_PRICE_CONV_SAVE_NEW_ROW, MODEL_FLAT_PRICE_CONV_SEARCH, MODEL_FLAT_PRICE_CONV_XLS,
            MODEL_MPS_CONV, MODEL_MPS_CONV_ADD_NEW_ROW, MODEL_MPS_CONV_BACK, MODEL_MPS_CONV_CSV,
            MODEL_MPS_CONV_CANCEL, MODEL_MPS_CONV_CLEAR_ALL_CREATE_NEW, MODEL_MPS_CONV_COPY_DATA,
            MODEL_MPS_CONV_DELETE, MODEL_MPS_CONV_EDIT, MODEL_MPS_CONV_PDF, MODEL_MPS_CONV_PRINT,
            MODEL_MPS_CONV_SAVE, MODEL_MPS_CONV_SAVE_NEW_ROW, MODEL_MPS_CONV_SEARCH, MODEL_MPS_CONV_XLS,
            PROD_FLAT_PRICE_CONV, PROD_FLAT_PRICE_CONV_ADD_NEW_ROW, PROD_FLAT_PRICE_CONV_BACK,
            PROD_FLAT_PRICE_CONV_CSV, PROD_FLAT_PRICE_CONV_CANCEL, PROD_FLAT_PRICE_CONV_CLEAR,
            PROD_FLAT_PRICE_CONV_CLEAR_FILTERS, PROD_FLAT_PRICE_CONV_CLEAR_RESULTS,
            PROD_FLAT_PRICE_CONV_DELETE, PROD_FLAT_PRICE_CONV_EDIT, PROD_FLAT_PRICE_CONV_PDF,
            PROD_FLAT_PRICE_CONV_PRINT, PROD_FLAT_PRICE_CONV_SAVE,
            PROD_FLAT_PRICE_CONV_SAVE_NEW_ROW, PROD_FLAT_PRICE_CONV_SEARCH, PROD_FLAT_PRICE_CONV_XLS,
            PROD_MPS_CONV, PROD_MPS_CONV_ADD_NEW_ROW, PROD_MPS_CONV_BACK, PROD_MPS_CONV_CSV,
            PROD_MPS_CONV_CANCEL, PROD_MPS_CONV_DELETE, PROD_MPS_CONV_EDIT, PROD_MPS_CONV_PDF,
            PROD_MPS_CONV_PRINT, PROD_MPS_CONV_SAVE, PROD_MPS_CONV_SAVE_NEW_ROW,
            PROD_MPS_CONV_SEARCH, PROD_MPS_CONV_XLS,
            PROD_SORT_CODE, PROD_SORT_CODE_ADD_NEW_ROW, PROD_SORT_CODE_BACK, PROD_SORT_CODE_CSV,
            PROD_SORT_CODE_CANCEL, PROD_SORT_CODE_DELETE, PROD_SORT_CODE_PDF, PROD_SORT_CODE_PRINT,
            PROD_SORT_CODE_SAVE, PROD_SORT_CODE_SAVE_NEW_ROW, PROD_SORT_CODE_SEARCH, PROD_SORT_CODE_XLS,
            R06, R06_ADD, R06_BACK, R06_CSV, R06_CLEAR, R06_GENERATE_REPORT, R06_PDF, R06_XLS, R06_REPORT,
            R07, R07_BACK, R07_CSV, R07_CLEAR, R07_GENERATE_REPORT, R07_PDF, R07_XLS, R07_REPORT,
            R08, R08_BACK, R08_CSV, R08_CLEAR, R08_GENERATE_REPORT, R08_PDF, R08_XLS, R08_REPORT,
            R10, R10_BACK, R10_CSV, R10_CLEAR, R10_GENERATE_REPORT, R10_PDF, R10_XLS, R10_REPORT,
            R11, R11_BACK, R11_CSV, R11_CLEAR, R11_GENERATE_REPORT, R11_PDF, R11_XLS, R11_REPORT,
            R12, R12_BACK, R12_CSV, R12_CLEAR, R12_GENERATE_REPORT, R12_PDF, R12_XLS, R12_REPORT,
            R13, R13_BACK, R13_CSV, R13_CLEAR, R13_GENERATE_REPORT, R13_PDF, R13_XLS, R13_REPORT,
            R14, R14_BACK, R14_CSV, R14_CLEAR, R14_GENERATE_REPORT, R14_PDF, R14_SEARCH, R14_XLS, R14_REPORT,
            REPORTS,
            SEARCH, SEARCH_BACK, SEARCH_CLEAR, SEARCH_DETAILS, SEARCH_SEARCH,
            TABLE_MAINTENANCE
    );

    /**
     * APPROVE has all ADMIN actions except:
     * - ESF_SUBMIT
     * - TABLE_MAINTENANCE
     * - R06_ADD, R06_CLEAR
     * - R07_CLEAR, R08_CLEAR, R10_CLEAR, R11_CLEAR, R12_CLEAR (R13_CLEAR IS intentionally included)
     * - R14_CLEAR, R14_SEARCH
     * - All conversion table write actions (Add New Row, Edit, Save, Delete, Copy Data, Print, etc.)
     * - prodSortCode/Print and prodMPSConv/Print (Print is ADMIN-only for these two pages; PDF is available)
     */
    public static final Set<String> APPROVE_ACTIONS = Set.of(
            INVOICE_SEARCH, INVOICE_SEARCH_SEARCH,
            LOG_SOURCE_CODE,
            INBOX, INBOX_BACK, INBOX_CLEAR, INBOX_SEARCH,
            INVOICE_DETAILS, INVOICE_DETAILS_APPROVE, INVOICE_DETAILS_BACK,
            INVOICE_DETAILS_CSV, INVOICE_DETAILS_CANCEL, INVOICE_DETAILS_DELETE,
            INVOICE_DETAILS_DUPLICATE, INVOICE_DETAILS_NEW_LINE_ITEM, INVOICE_DETAILS_PDF,
            INVOICE_DETAILS_PRINTS, INVOICE_DETAILS_REJECT, INVOICE_DETAILS_SAVE,
            INVOICE_DETAILS_SAVE_INVOICE, INVOICE_DETAILS_SUBMIT, INVOICE_DETAILS_UNAPPROVE,
            INVOICE_DETAILS_UPDATE_GROUP, INVOICE_DETAILS_XLS,
            MODEL_FLAT_PRICE_CONV, MODEL_FLAT_PRICE_CONV_BACK, MODEL_FLAT_PRICE_CONV_CSV,
            MODEL_FLAT_PRICE_CONV_CANCEL, MODEL_FLAT_PRICE_CONV_CLEAR,
            MODEL_FLAT_PRICE_CONV_CLEAR_FILTERS, MODEL_FLAT_PRICE_CONV_CLEAR_RESULTS,
            MODEL_FLAT_PRICE_CONV_PDF, MODEL_FLAT_PRICE_CONV_SEARCH, MODEL_FLAT_PRICE_CONV_XLS,
            MODEL_MPS_CONV, MODEL_MPS_CONV_BACK, MODEL_MPS_CONV_CSV, MODEL_MPS_CONV_CANCEL,
            MODEL_MPS_CONV_PDF, MODEL_MPS_CONV_SEARCH, MODEL_MPS_CONV_XLS,
            PROD_FLAT_PRICE_CONV, PROD_FLAT_PRICE_CONV_BACK, PROD_FLAT_PRICE_CONV_CSV,
            PROD_FLAT_PRICE_CONV_CANCEL, PROD_FLAT_PRICE_CONV_CLEAR,
            PROD_FLAT_PRICE_CONV_CLEAR_FILTERS, PROD_FLAT_PRICE_CONV_CLEAR_RESULTS,
            PROD_FLAT_PRICE_CONV_PDF, PROD_FLAT_PRICE_CONV_SEARCH, PROD_FLAT_PRICE_CONV_XLS,
            PROD_MPS_CONV, PROD_MPS_CONV_BACK, PROD_MPS_CONV_CSV, PROD_MPS_CONV_CANCEL,
            PROD_MPS_CONV_PDF, PROD_MPS_CONV_SEARCH, PROD_MPS_CONV_XLS,
            PROD_SORT_CODE, PROD_SORT_CODE_BACK, PROD_SORT_CODE_CSV, PROD_SORT_CODE_CANCEL,
            PROD_SORT_CODE_PDF, PROD_SORT_CODE_SEARCH, PROD_SORT_CODE_XLS,
            R06, R06_BACK, R06_CSV, R06_GENERATE_REPORT, R06_PDF, R06_XLS, R06_REPORT,
            R07, R07_BACK, R07_CSV, R07_GENERATE_REPORT, R07_PDF, R07_XLS, R07_REPORT,
            R08, R08_BACK, R08_CSV, R08_GENERATE_REPORT, R08_PDF, R08_XLS, R08_REPORT,
            R10, R10_BACK, R10_CSV, R10_GENERATE_REPORT, R10_PDF, R10_XLS, R10_REPORT,
            R11, R11_BACK, R11_CSV, R11_GENERATE_REPORT, R11_PDF, R11_XLS, R11_REPORT,
            R12, R12_BACK, R12_CSV, R12_GENERATE_REPORT, R12_PDF, R12_XLS, R12_REPORT,
            R13, R13_BACK, R13_CSV, R13_CLEAR, R13_GENERATE_REPORT, R13_PDF, R13_XLS, R13_REPORT,
            R14, R14_BACK, R14_CSV, R14_GENERATE_REPORT, R14_PDF, R14_XLS, R14_REPORT,
            REPORTS,
            SEARCH, SEARCH_BACK, SEARCH_CLEAR, SEARCH_DETAILS, SEARCH_SEARCH
    );

    /**
     * VIEW is read-only: same as APPROVE except no invoice write actions
     * (no Approve/Reject/Save/Submit/Cancel/Delete/Duplicate/New Line Item/Unapprove/Update Group).
     * - prodSortCode/Print and prodMPSConv/Print (Print is ADMIN-only for these two pages; PDF is available)
     */
    public static final Set<String> VIEW_ACTIONS = Set.of(
            INVOICE_SEARCH, INVOICE_SEARCH_SEARCH,
            LOG_SOURCE_CODE,
            INBOX, INBOX_BACK, INBOX_CLEAR, INBOX_SEARCH,
            INVOICE_DETAILS, INVOICE_DETAILS_BACK, INVOICE_DETAILS_CSV,
            INVOICE_DETAILS_PDF, INVOICE_DETAILS_PRINTS, INVOICE_DETAILS_XLS,
            MODEL_FLAT_PRICE_CONV, MODEL_FLAT_PRICE_CONV_BACK, MODEL_FLAT_PRICE_CONV_CSV,
            MODEL_FLAT_PRICE_CONV_CANCEL, MODEL_FLAT_PRICE_CONV_CLEAR,
            MODEL_FLAT_PRICE_CONV_CLEAR_FILTERS, MODEL_FLAT_PRICE_CONV_CLEAR_RESULTS,
            MODEL_FLAT_PRICE_CONV_PDF, MODEL_FLAT_PRICE_CONV_SEARCH, MODEL_FLAT_PRICE_CONV_XLS,
            MODEL_MPS_CONV, MODEL_MPS_CONV_BACK, MODEL_MPS_CONV_CSV, MODEL_MPS_CONV_CANCEL,
            MODEL_MPS_CONV_PDF, MODEL_MPS_CONV_SEARCH, MODEL_MPS_CONV_XLS,
            PROD_FLAT_PRICE_CONV, PROD_FLAT_PRICE_CONV_BACK, PROD_FLAT_PRICE_CONV_CSV,
            PROD_FLAT_PRICE_CONV_CANCEL, PROD_FLAT_PRICE_CONV_CLEAR,
            PROD_FLAT_PRICE_CONV_CLEAR_FILTERS, PROD_FLAT_PRICE_CONV_CLEAR_RESULTS,
            PROD_FLAT_PRICE_CONV_PDF, PROD_FLAT_PRICE_CONV_SEARCH, PROD_FLAT_PRICE_CONV_XLS,
            PROD_MPS_CONV, PROD_MPS_CONV_BACK, PROD_MPS_CONV_CSV, PROD_MPS_CONV_CANCEL,
            PROD_MPS_CONV_PDF, PROD_MPS_CONV_SEARCH, PROD_MPS_CONV_XLS,
            PROD_SORT_CODE, PROD_SORT_CODE_BACK, PROD_SORT_CODE_CSV, PROD_SORT_CODE_CANCEL,
            PROD_SORT_CODE_PDF, PROD_SORT_CODE_SEARCH, PROD_SORT_CODE_XLS,
            R06, R06_BACK, R06_CSV, R06_GENERATE_REPORT, R06_PDF, R06_XLS, R06_REPORT,
            R07, R07_BACK, R07_CSV, R07_GENERATE_REPORT, R07_PDF, R07_XLS, R07_REPORT,
            R08, R08_BACK, R08_CSV, R08_GENERATE_REPORT, R08_PDF, R08_XLS, R08_REPORT,
            R10, R10_BACK, R10_CSV, R10_GENERATE_REPORT, R10_PDF, R10_XLS, R10_REPORT,
            R11, R11_BACK, R11_CSV, R11_GENERATE_REPORT, R11_PDF, R11_XLS, R11_REPORT,
            R12, R12_BACK, R12_CSV, R12_GENERATE_REPORT, R12_PDF, R12_XLS, R12_REPORT,
            R13, R13_BACK, R13_CSV, R13_CLEAR, R13_GENERATE_REPORT, R13_PDF, R13_XLS, R13_REPORT,
            R14, R14_BACK, R14_CSV, R14_GENERATE_REPORT, R14_PDF, R14_XLS, R14_REPORT,
            REPORTS,
            SEARCH, SEARCH_BACK, SEARCH_CLEAR, SEARCH_DETAILS, SEARCH_SEARCH
    );

    /** Maps a role constant to its permitted action strings. */
    public static final Map<String, Set<String>> ROLE_PERMISSIONS = Map.of(
            Roles.ADMIN,   ADMIN_ACTIONS,
            Roles.APPROVE, APPROVE_ACTIONS,
            Roles.VIEW,    VIEW_ACTIONS
    );
}
