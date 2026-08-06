package ca.bc.gov.nrs.csp.backend.config;

import net.sf.jasperreports.engine.query.JRJdbcQueryExecuterFactory;

import java.sql.ResultSet;

/**
 * JasperReports' compile-time design verifier ({@code JRVerifier.verifyQuery}) checks every
 * {@code $P{...}} used inside a query against the query executer factory's
 * {@code supportsQueryParameterType()} allow-list — for {@link JRJdbcQueryExecuterFactory} that
 * list only contains 19 primitive/date/String types and does NOT include {@link ResultSet},
 * so a report declaring a {@code REPORT_CURSOR} parameter of class {@code java.sql.ResultSet}
 * (the standard pattern for an Oracle stored-procedure call returning a REF CURSOR) fails to
 * compile with "Parameter type not supported in query", even though the executer itself
 * ({@code JRJdbcQueryExecuter}, via {@code OracleProcedureCallHandlerFactory}) fully supports
 * binding a REF CURSOR OUT parameter through {@code Connection.prepareCall(...)} at runtime.
 *
 * <p>This factory only relaxes that compile-time check; {@code createQueryExecuter} is
 * inherited unchanged, so runtime query execution is identical to the standard "sql" language.
 * Registered for the "plsql" language in {@code resources/jasperreports.properties} — the
 * R06-R12 report designs (originally authored for JasperReports Server, which bundles its own
 * "plsql" language support as a commercial extension not present in open-source JasperReports)
 * declare {@code <query language="plsql">} for their stored-procedure calls.</p>
 */
public class PlsqlQueryExecuterFactory extends JRJdbcQueryExecuterFactory {

    @Override
    public boolean supportsQueryParameterType(String className) {
        return ResultSet.class.getName().equals(className) || super.supportsQueryParameterType(className);
    }
}
