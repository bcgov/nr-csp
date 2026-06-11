package ca.bc.gov.nrs.csp.backend.util.constants;

public final class ConstantsCode {

    private ConstantsCode() {}

    public static final String INVENTRYSTATUS_NEW = "NEW";
    public static final String INVENTRYSTATUS_UNAPPROVED = "UNA";
    public static final String INVENTRYSTATUS_DRAFT = "DFT";
    public static final String INVENTRYSTATUS_APPROVED = "APP";
    public static final String INVENTRYSTATUS_REJECTED = "REJ";
    public static final String INVENTRYSTATUS_PROCESSING = "PRO";
    public static final String INVENTRYSTATUS_CANCELLED = "CAN";

    public static final String SUBMSTATUS_COMPLETE = "COM";
    public static final String SUBMSTATUS_INBOX = "INB";
    public static final String SUBMSTATUS_LOBBY = "LOB";
    public static final String SUBMSTATUS_REJECTED = "REJ";

    public static final String LOGSOURCECODE_TIMERMARK = "MARK";
    public static final String LOGSOURCECODE_BOOMNUMBER = "BOOM";
    public static final String LOGSOURCECODE_WEIGHSLIP = "WEIGH";

    public static final String INVOICE_SUBMITTEDBY_BUYER = "Buyer";
    public static final String INVOICE_SUBMITTEDBY_SELLER = "Seller";

    public static final String INVTYPE_PURCHASE = "PUR";
    public static final String INVTYPE_SALE = "SAL";
    public static final String INVTYPE_ADJUST = "ADJ";

    public static final String INVRELATETYPE_REPLACE = "REP";
    public static final String INVRELATETYPE_ADJUST = "ADJ";

    public static final String SORTCODE_BOOMSTICK = "B";

    public static final int MAXOFCSVFORREPLACEINVOICENUM = 5;
    public static final int MAXOFCSVFORADJUSTINVOICENUM = 5;
    public static final int MAXOFCSVFORBOOMNUMBERS = 10;
    public static final int MAXOFCSVFORTIMBERMARKS = 10;
    public static final int MAXOFCSVFORWEIGHSLIPS = 10;
    public static final int MAXTOKENLENGTHFORBOOMNUMBERS = 20;
    public static final int MAXTOKENLENGTHFORTIMBERMARKS = 6;
    public static final int MAXTOKENLENGTHFORWEIGHSLIPS = 100;

    public static final double TOTALAMOUNT_MAXPERMITTEDVARIANCE = 5.00;
    public static final double TOTALVOLUME_MAXPERMITTEDVARIANCE = 5.00;
    public static final int TOTALPIECES_MAXPERMITTEDVARIANCE = 0;
}
