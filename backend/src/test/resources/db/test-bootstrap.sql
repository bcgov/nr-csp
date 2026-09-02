-- Test schema bootstrap for the Oracle Testcontainers integration tests.
--
-- Executed once per container by OracleTestContainer.bootstrap(), connected as
-- SYSTEM (the gvenzl image sets the SYS/SYSTEM password to the container
-- password). Statements are separated by a line containing only "/" — never by
-- semicolons — so PL/SQL bodies (which contain semicolons) need no special
-- casing. Full-line "--" comments are stripped before execution.
--
-- ${APP_USER} is replaced at runtime with the container's application user
-- (the account the Spring datasource connects as).
--
-- What this builds:
--  * A THE schema mirroring the subset of the production schema the backend
--    touches at startup (ReferenceDataWarmupService lookups, client search)
--    plus one seed-data table per report under test.
--  * Stub CSP_SP_RPT_* ref-cursor procedures with production-matching
--    signatures (parameter order comes from the <query> in each
--    src/main/resources/reports/*.jrxml). The stubs only filter on the
--    parameters the ITs exercise (date ranges, seller, month/year, submission
--    number, log-sale type); the legacy procs' status/maturity list filtering
--    is intentionally not replicated.
--  * Grants and synonyms so ${APP_USER} can read THE.* tables and call the
--    procedures unqualified, exactly as the app does against the real DB.

CREATE USER the IDENTIFIED BY the_test_pw DEFAULT TABLESPACE users QUOTA UNLIMITED ON users
/

-- ────────────────────────────────────────────────────────────────────────────
-- Reference tables read by ReferenceDataWarmupService at startup
-- (columns match the SQL in LookupRepository). Rows whose lookup query filters
-- on expiry_date > SYSDATE without a NULL guard (submission status, modelling)
-- must carry a real far-future expiry date.
-- ────────────────────────────────────────────────────────────────────────────

CREATE TABLE the.log_sale_type_code (
    log_sale_type_code VARCHAR2(1) PRIMARY KEY,
    description        VARCHAR2(200),
    effective_date     DATE,
    expiry_date        DATE
)
/
INSERT INTO the.log_sale_type_code VALUES ('O', 'Old Growth',    DATE '2000-01-01', DATE '9999-12-31')
/
INSERT INTO the.log_sale_type_code VALUES ('S', 'Second Growth', DATE '2000-01-01', DATE '9999-12-31')
/
INSERT INTO the.log_sale_type_code VALUES ('M', 'Mixed',         DATE '2000-01-01', DATE '9999-12-31')
/

CREATE TABLE the.csp_invoice_type_code (
    csp_invoice_type_code VARCHAR2(3) PRIMARY KEY,
    description           VARCHAR2(200),
    effective_date        DATE,
    expiry_date           DATE
)
/
INSERT INTO the.csp_invoice_type_code VALUES ('SAL', 'Sale',        DATE '2000-01-01', DATE '9999-12-31')
/
INSERT INTO the.csp_invoice_type_code VALUES ('PUR', 'Purchase',    DATE '2000-01-01', DATE '9999-12-31')
/
INSERT INTO the.csp_invoice_type_code VALUES ('ADJ', 'Adjustment',  DATE '2000-01-01', DATE '9999-12-31')
/
INSERT INTO the.csp_invoice_type_code VALUES ('CAN', 'Cancellation', DATE '2000-01-01', DATE '9999-12-31')
/

CREATE TABLE the.log_sale_entry_status_code (
    log_sale_entry_status_code VARCHAR2(3) PRIMARY KEY,
    description                VARCHAR2(200),
    effective_date             DATE,
    expiry_date                DATE
)
/
INSERT INTO the.log_sale_entry_status_code VALUES ('APP', 'Approved', DATE '2000-01-01', DATE '9999-12-31')
/
INSERT INTO the.log_sale_entry_status_code VALUES ('DFT', 'Draft',    DATE '2000-01-01', DATE '9999-12-31')
/

CREATE TABLE the.csp_submission_status_code (
    csp_submission_status_code VARCHAR2(3) PRIMARY KEY,
    description                VARCHAR2(200),
    effective_date             DATE,
    expiry_date                DATE
)
/
INSERT INTO the.csp_submission_status_code VALUES ('COM', 'Complete', DATE '2000-01-01', DATE '9999-12-31')
/
INSERT INTO the.csp_submission_status_code VALUES ('INB', 'In Batch', DATE '2000-01-01', DATE '9999-12-31')
/

CREATE TABLE the.log_sale_sort_code (
    log_sale_sort_code VARCHAR2(3) PRIMARY KEY,
    description        VARCHAR2(200),
    effective_date     DATE,
    expiry_date        DATE
)
/
INSERT INTO the.log_sale_sort_code VALUES ('01', 'Sort 01', DATE '2000-01-01', DATE '9999-12-31')
/

CREATE TABLE the.log_sale_species_code (
    log_sale_species_code VARCHAR2(3) PRIMARY KEY,
    description           VARCHAR2(200),
    effective_date        DATE,
    expiry_date           DATE
)
/
INSERT INTO the.log_sale_species_code VALUES ('HEM', 'Hemlock', DATE '2000-01-01', DATE '9999-12-31')
/
INSERT INTO the.log_sale_species_code VALUES ('BAL', 'Balsam',  DATE '2000-01-01', DATE '9999-12-31')
/

CREATE TABLE the.log_sale_grade_code (
    log_sale_grade_code VARCHAR2(1) PRIMARY KEY,
    description         VARCHAR2(200),
    effective_date      DATE,
    expiry_date         DATE
)
/
INSERT INTO the.log_sale_grade_code VALUES ('H', 'H Grade', DATE '2000-01-01', DATE '9999-12-31')
/
INSERT INTO the.log_sale_grade_code VALUES ('J', 'J Grade', DATE '2000-01-01', DATE '9999-12-31')
/

CREATE TABLE the.csp_species_grade_xref (
    log_sale_species_code VARCHAR2(3),
    log_sale_grade_code   VARCHAR2(1),
    effective_date        DATE,
    expiry_date           DATE
)
/
INSERT INTO the.csp_species_grade_xref VALUES ('HEM', 'H', DATE '2000-01-01', DATE '9999-12-31')
/
INSERT INTO the.csp_species_grade_xref VALUES ('BAL', 'J', DATE '2000-01-01', DATE '9999-12-31')
/

CREATE TABLE the.csp_modelling_code (
    csp_modelling_code VARCHAR2(3) PRIMARY KEY,
    description        VARCHAR2(200),
    effective_date     DATE,
    expiry_date        DATE
)
/
INSERT INTO the.csp_modelling_code VALUES ('Y', 'Modelled', DATE '2000-01-01', DATE '9999-12-31')
/

CREATE TABLE the.log_sale_fob_location_code (
    log_sale_fob_location_code VARCHAR2(3) PRIMARY KEY,
    description                VARCHAR2(200),
    effective_date             DATE,
    expiry_date                DATE
)
/
INSERT INTO the.log_sale_fob_location_code VALUES ('V', 'Vancouver', DATE '2000-01-01', DATE '9999-12-31')
/

CREATE TABLE the.log_sale_species_group_list (
    log_sale_species_group_code VARCHAR2(3),
    log_sale_species_code       VARCHAR2(3)
)
/
INSERT INTO the.log_sale_species_group_list VALUES ('HB', 'HEM')
/

-- ────────────────────────────────────────────────────────────────────────────
-- Client tables used by the report validators (SearchService.findClientsByNumber)
-- ────────────────────────────────────────────────────────────────────────────

CREATE TABLE the.v_client_public (
    client_number VARCHAR2(8) PRIMARY KEY,
    client_name   VARCHAR2(100)
)
/
INSERT INTO the.v_client_public VALUES ('00001001', 'ACME TIMBER SALES LTD.')
/
INSERT INTO the.v_client_public VALUES ('00002002', 'PACIFIC LOG BUYERS INC.')
/

CREATE TABLE the.client_location (
    client_number    VARCHAR2(8),
    client_locn_code VARCHAR2(2),
    client_locn_name VARCHAR2(100),
    city             VARCHAR2(60),
    province         VARCHAR2(2)
)
/
INSERT INTO the.client_location VALUES ('00001001', '00', 'HEAD OFFICE', 'VICTORIA', 'BC')
/
INSERT INTO the.client_location VALUES ('00002002', '00', 'HEAD OFFICE', 'NANAIMO', 'BC')
/

-- ────────────────────────────────────────────────────────────────────────────
-- Report seed tables. Column names match the <field name="..."> declarations
-- in the corresponding JRXML templates exactly (the ref cursor is mapped to
-- report fields by column name); extra *_client_number filter columns beyond
-- the template's field list are ignored by JasperReports.
-- All seed rows sit in January 2024 so tests can pin fixed date ranges.
-- ────────────────────────────────────────────────────────────────────────────

CREATE TABLE the.csp_rpt07_data (
    maturity                   VARCHAR2(1),
    maturity_desc              VARCHAR2(200),
    submitter_id               VARCHAR2(30),
    client_invoice_date        DATE,
    client_invoice_no          VARCHAR2(30),
    csp_invoice_type_code      VARCHAR2(3),
    invoice_type_desc          VARCHAR2(200),
    participant_client_no      VARCHAR2(8),
    replaces_adjusts           VARCHAR2(30),
    submission_yyyymm          VARCHAR2(6),
    submission_status_desc     VARCHAR2(200),
    submission_id              VARCHAR2(30),
    approval_yyyymm            VARCHAR2(6),
    original_volume            NUMBER,
    new_volume                 NUMBER,
    original_amount            NUMBER,
    new_amount                 NUMBER,
    log_sale_entry_status_code VARCHAR2(3),
    invoice_status_desc        VARCHAR2(200),
    coastal_log_sale_id        VARCHAR2(30),
    revision_count             VARCHAR2(10),
    update_timestamp           DATE,
    seller_client_number       VARCHAR2(8),
    buyer_client_number        VARCHAR2(8)
)
/
INSERT INTO the.csp_rpt07_data VALUES (
    'O', 'Old Growth', 'SUBMITTER1', DATE '2024-01-15', 'INV-7001', 'SAL', 'Sale',
    '00002002', NULL, '202401', 'Complete', '9001', '202401',
    100.5, 100.5, 5025.00, 5025.00, 'APP', 'Approved', 'CLS-7001', '1',
    DATE '2024-01-16', '00001001', '00002002')
/

CREATE TABLE the.csp_rpt08_data (
    csp_invoice_type_code          VARCHAR2(3),
    seller_client_number           VARCHAR2(8),
    buyer_client_number            VARCHAR2(8),
    client_invoice_no              VARCHAR2(30),
    client_invoice_date            DATE,
    client_total_invoice_pieces    NUMBER,
    client_total_invoice_volume    NUMBER,
    client_total_invoice_amt       NUMBER,
    invoice_type_desc              VARCHAR2(200),
    log_sale_type_code             VARCHAR2(1),
    approval_date                  VARCHAR2(10),
    submission_id                  VARCHAR2(30),
    submission_month               VARCHAR2(6),
    submission_client_pieces_total NUMBER,
    submission_client_volume_total NUMBER,
    submission_client_amt_total    NUMBER
)
/
INSERT INTO the.csp_rpt08_data VALUES (
    'SAL', '00001001', '00002002', 'INV-8001', DATE '2024-01-15',
    250, 480.75, 24037.50, 'Sale', 'O', '2024-01-20', '9001', '202401',
    250, 480.75, 24037.50)
/

CREATE TABLE the.csp_rpt10_data (
    log_sale_species_code VARCHAR2(3),
    log_sale_grade_code   VARCHAR2(1),
    log_sale_sort_code    VARCHAR2(3),
    log_sale_type_code    VARCHAR2(1),
    seller_client_number  VARCHAR2(8),
    buyer_client_number   VARCHAR2(8),
    client_invoice_no     VARCHAR2(30),
    invoice_date          DATE,
    pieces                NUMBER,
    volume                NUMBER,
    volume_percentage     NUMBER,
    amount                NUMBER,
    amv                   NUMBER,
    maturity_desc         VARCHAR2(200),
    species_desc          VARCHAR2(200),
    grade_desc            VARCHAR2(200),
    sortcode_desc         VARCHAR2(200),
    totalvol_species      NUMBER,
    speciesvol_percent    NUMBER,
    totalvol_sort         NUMBER,
    sortvol_percent       NUMBER,
    grandtotalvol         NUMBER
)
/
INSERT INTO the.csp_rpt10_data VALUES (
    'HEM', 'H', '01', 'O', '00001001', '00002002', 'INV-1001', DATE '2024-01-15',
    120, 300.25, 62.5, 15012.50, 50.00, 'Old Growth', 'Hemlock', 'H Grade', 'Sort 01',
    300.25, 62.5, 480.50, 62.5, 480.50)
/
INSERT INTO the.csp_rpt10_data VALUES (
    'BAL', 'J', '01', 'O', '00001001', '00002002', 'INV-1001', DATE '2024-01-15',
    80, 180.25, 37.5, 7210.00, 40.00, 'Old Growth', 'Balsam', 'J Grade', 'Sort 01',
    180.25, 37.5, 480.50, 37.5, 480.50)
/

CREATE TABLE the.csp_rpt12_data (
    log_sale_type_code         VARCHAR2(1),
    client_invoice_date        DATE,
    log_sale_sort_code         VARCHAR2(3),
    log_sale_species_code      VARCHAR2(3),
    log_sale_grade_code        VARCHAR2(1),
    pieces                     NUMBER,
    volume                     NUMBER,
    avg_price                  NUMBER,
    amount                     NUMBER,
    coastal_log_sale_id        VARCHAR2(30),
    coastal_log_sale_detail_id NUMBER(18),
    maturity                   VARCHAR2(1),
    sord_code_desc             VARCHAR2(200),
    species_desc               VARCHAR2(200),
    gread_desc                 VARCHAR2(200)
)
/
INSERT INTO the.csp_rpt12_data VALUES (
    'O', DATE '2024-01-15', '01', 'HEM', 'H', 120, 300.25, 50.00, 15012.50,
    'CLS-1201', 120101, 'O', 'Sort 01', 'Hemlock', 'H Grade')
/
INSERT INTO the.csp_rpt12_data VALUES (
    'O', DATE '2024-01-20', '01', 'BAL', 'J', 80, 180.25, 40.00, 7210.00,
    'CLS-1202', 120102, 'O', 'Sort 01', 'Balsam', 'J Grade')
/

-- ────────────────────────────────────────────────────────────────────────────
-- Stub report procedures. Parameter order matches the {call ...} in each
-- JRXML template; the first parameter is always the REF CURSOR OUT param that
-- RefCursorProcedureCallHandlerFactory binds. Date strings arrive as YYYYMMDD.
-- ────────────────────────────────────────────────────────────────────────────

CREATE OR REPLACE PROCEDURE the.csp_sp_rpt_07 (
    p_cursor            OUT SYS_REFCURSOR,
    p_month             IN  VARCHAR2,
    p_year              IN  VARCHAR2,
    p_invoice_date_from IN  VARCHAR2,
    p_invoice_date_to   IN  VARCHAR2,
    p_seller_number     IN  VARCHAR2,
    p_seller_locn_code  IN  VARCHAR2,
    p_buyer_number      IN  VARCHAR2,
    p_buyer_locn_code   IN  VARCHAR2,
    p_invoice_type      IN  VARCHAR2,
    p_invoice_status    IN  VARCHAR2,
    p_submission_status IN  VARCHAR2,
    p_submission_number IN  VARCHAR2,
    p_submission_yyyymm IN  VARCHAR2,
    p_maturity          IN  VARCHAR2,
    p_show_repl_adjusts IN  VARCHAR2
) AS
BEGIN
    OPEN p_cursor FOR
        SELECT t.*
        FROM the.csp_rpt07_data t
        WHERE (p_invoice_date_from IS NULL OR t.client_invoice_date >= TO_DATE(p_invoice_date_from, 'YYYYMMDD'))
          AND (p_invoice_date_to   IS NULL OR t.client_invoice_date <  TO_DATE(p_invoice_date_to, 'YYYYMMDD') + 1)
          AND (p_seller_number     IS NULL OR t.seller_client_number = p_seller_number)
          AND (p_buyer_number      IS NULL OR t.buyer_client_number  = p_buyer_number)
          AND (p_submission_number IS NULL OR t.submission_id        = p_submission_number)
        ORDER BY t.client_invoice_no;
END;
/

CREATE OR REPLACE PROCEDURE the.csp_sp_rpt_08 (
    p_cursor            OUT SYS_REFCURSOR,
    p_invoice_date_from IN  VARCHAR2,
    p_invoice_date_to   IN  VARCHAR2,
    p_invoice_status    IN  VARCHAR2,
    p_seller_number     IN  VARCHAR2,
    p_seller_locn_code  IN  VARCHAR2,
    p_buyer_number      IN  VARCHAR2,
    p_buyer_locn_code   IN  VARCHAR2,
    p_maturity          IN  VARCHAR2,
    p_invoice_type      IN  VARCHAR2,
    p_month             IN  VARCHAR2,
    p_year              IN  VARCHAR2,
    p_submission_number IN  VARCHAR2
) AS
BEGIN
    OPEN p_cursor FOR
        SELECT t.*
        FROM the.csp_rpt08_data t
        WHERE (p_invoice_date_from IS NULL OR t.client_invoice_date >= TO_DATE(p_invoice_date_from, 'YYYYMMDD'))
          AND (p_invoice_date_to   IS NULL OR t.client_invoice_date <  TO_DATE(p_invoice_date_to, 'YYYYMMDD') + 1)
          AND (p_seller_number     IS NULL OR t.seller_client_number = p_seller_number)
          AND (p_buyer_number      IS NULL OR t.buyer_client_number  = p_buyer_number)
          AND (p_submission_number IS NULL OR t.submission_id        = p_submission_number)
        ORDER BY t.client_invoice_no;
END;
/

CREATE OR REPLACE PROCEDURE the.csp_sp_rpt_10 (
    p_cursor            OUT SYS_REFCURSOR,
    p_invoice_date_from IN  VARCHAR2,
    p_invoice_date_to   IN  VARCHAR2,
    p_seller_number     IN  VARCHAR2,
    p_seller_locn_code  IN  VARCHAR2,
    p_buyer_number      IN  VARCHAR2,
    p_buyer_locn_code   IN  VARCHAR2,
    p_maturity          IN  VARCHAR2,
    p_invoice_type_code IN  VARCHAR2
) AS
BEGIN
    OPEN p_cursor FOR
        SELECT t.*
        FROM the.csp_rpt10_data t
        WHERE (p_invoice_date_from IS NULL OR t.invoice_date >= TO_DATE(p_invoice_date_from, 'YYYYMMDD'))
          AND (p_invoice_date_to   IS NULL OR t.invoice_date <  TO_DATE(p_invoice_date_to, 'YYYYMMDD') + 1)
          AND (p_seller_number     IS NULL OR t.seller_client_number = p_seller_number)
          AND (p_buyer_number      IS NULL OR t.buyer_client_number  = p_buyer_number)
        ORDER BY t.log_sale_species_code;
END;
/

CREATE OR REPLACE PROCEDURE the.csp_sp_rpt_12 (
    p_cursor             OUT SYS_REFCURSOR,
    p_month              IN  VARCHAR2,
    p_year               IN  VARCHAR2,
    p_log_sale_type_code IN  VARCHAR2,
    p_invoice_date_from  IN  VARCHAR2,
    p_invoice_date_to    IN  VARCHAR2
) AS
BEGIN
    OPEN p_cursor FOR
        SELECT t.*
        FROM the.csp_rpt12_data t
        WHERE (p_year               IS NULL OR TO_CHAR(t.client_invoice_date, 'YYYY') = p_year)
          AND (p_month              IS NULL OR TO_CHAR(t.client_invoice_date, 'MM')   = p_month)
          AND (p_log_sale_type_code IS NULL OR t.log_sale_type_code = p_log_sale_type_code)
          AND (p_invoice_date_from  IS NULL OR t.client_invoice_date >= TO_DATE(p_invoice_date_from, 'YYYYMMDD'))
          AND (p_invoice_date_to    IS NULL OR t.client_invoice_date <  TO_DATE(p_invoice_date_to, 'YYYYMMDD') + 1)
        ORDER BY t.client_invoice_date;
END;
/

-- ────────────────────────────────────────────────────────────────────────────
-- Grants + synonyms so the application user sees the same object names it
-- does in production: THE.* tables via qualified SELECT, CSP_SP_RPT_* procs
-- unqualified.
-- ────────────────────────────────────────────────────────────────────────────

GRANT SELECT ON the.log_sale_type_code TO ${APP_USER}
/
GRANT SELECT ON the.csp_invoice_type_code TO ${APP_USER}
/
GRANT SELECT ON the.log_sale_entry_status_code TO ${APP_USER}
/
GRANT SELECT ON the.csp_submission_status_code TO ${APP_USER}
/
GRANT SELECT ON the.log_sale_sort_code TO ${APP_USER}
/
GRANT SELECT ON the.log_sale_species_code TO ${APP_USER}
/
GRANT SELECT ON the.log_sale_grade_code TO ${APP_USER}
/
GRANT SELECT ON the.csp_species_grade_xref TO ${APP_USER}
/
GRANT SELECT ON the.csp_modelling_code TO ${APP_USER}
/
GRANT SELECT ON the.log_sale_fob_location_code TO ${APP_USER}
/
GRANT SELECT ON the.log_sale_species_group_list TO ${APP_USER}
/
GRANT SELECT ON the.v_client_public TO ${APP_USER}
/
GRANT SELECT ON the.client_location TO ${APP_USER}
/
GRANT EXECUTE ON the.csp_sp_rpt_07 TO ${APP_USER}
/
GRANT EXECUTE ON the.csp_sp_rpt_08 TO ${APP_USER}
/
GRANT EXECUTE ON the.csp_sp_rpt_10 TO ${APP_USER}
/
GRANT EXECUTE ON the.csp_sp_rpt_12 TO ${APP_USER}
/
CREATE SYNONYM ${APP_USER}.csp_sp_rpt_07 FOR the.csp_sp_rpt_07
/
CREATE SYNONYM ${APP_USER}.csp_sp_rpt_08 FOR the.csp_sp_rpt_08
/
CREATE SYNONYM ${APP_USER}.csp_sp_rpt_10 FOR the.csp_sp_rpt_10
/
CREATE SYNONYM ${APP_USER}.csp_sp_rpt_12 FOR the.csp_sp_rpt_12
/
COMMIT
/
