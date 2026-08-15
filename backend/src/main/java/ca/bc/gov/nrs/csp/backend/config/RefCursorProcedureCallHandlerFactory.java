package ca.bc.gov.nrs.csp.backend.config;

import net.sf.jasperreports.engine.query.ProcedureCallHandler;
import net.sf.jasperreports.engine.query.ProcedureCallHandlerFactory;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.regex.Pattern;

/**
 * Registers the Oracle REF CURSOR binding that JasperReports 7.0.4's own
 * {@code net.sf.jasperreports.engine.query.OracleProcedureCallHandlerFactory} claims to provide
 * but doesn't: that class reflectively loads {@code com.jaspersoft.jasperreports.plsql.OracleProcedureCallHandler},
 * a class that does not exist in the open-source {@code jasperreports} artifact (nor anywhere on
 * Maven Central) — confirmed by decompiling the 7.0.4 jar. When that reflective load fails with
 * {@code ClassNotFoundException}, the factory silently swallows it and returns {@code null} — no
 * log line, no exception. With no working handler, {@code JRJdbcQueryExecuter.isProcedureCall()}
 * always returns {@code false} even for genuine {@code {call ...}} queries, so it prepares a plain
 * {@code PreparedStatement} instead of a {@code CallableStatement}, and the
 * {@code REPORT_CURSOR} OUT parameter (declared {@code java.sql.ResultSet}, with no IN value)
 * falls through to a generic binding path that does {@code statement.setNull(index, Types.JAVA_OBJECT)}
 * — which Oracle's driver rejects with {@code ORA-17004: Invalid column type: 2000}.
 *
 * <p>This factory replaces the broken built-in one via the
 * {@code net.sf.jasperreports.jdbc.procedure.call.handler.factory} property (see
 * {@code resources/jasperreports.properties}). Every stored-procedure report in this codebase
 * follows the convention (confirmed against every jrxml's {@code <query language="plsql">}) that
 * the {@code REPORT_CURSOR} OUT parameter is always declared first — this handler relies on that
 * convention rather than inspecting parameter metadata.</p>
 */
public class RefCursorProcedureCallHandlerFactory implements ProcedureCallHandlerFactory {

    @Override
    public ProcedureCallHandler createProcedureCallHandler() {
        return new RefCursorProcedureCallHandler();
    }

    private static final class RefCursorProcedureCallHandler implements ProcedureCallHandler {

        // Matches JDBC escape syntax for a stored-procedure call: {call ...} or {? = call ...}.
        // This app only ever talks to Oracle, so isHandling() doesn't need to inspect the
        // Connection — but it must still distinguish procedure calls from plain SELECT queries
        // (e.g. R13's language="sql" query), which must NOT be routed through prepareCall().
        //
        // Uses [^}]* rather than .* after "call" so the mandatory whitespace and the
        // "everything else" segment can never both match the same characters — with the
        // original .* there was no character excluded from either side, so the engine could
        // backtrack across every possible split point between them (quadratic on pathological
        // input, flagged by SonarQube as super-linear regex performance). A negated character
        // class matches newlines natively, so DOTALL is no longer needed either.
        private static final Pattern CALL_SYNTAX = Pattern.compile(
                "^\\s*\\{\\s*(\\?\\s*=\\s*)?call\\s[^}]*}\\s*$", Pattern.CASE_INSENSITIVE);

        private CallableStatement statement;

        @Override
        public boolean isHandling(Connection connection, String queryString) {
            return queryString != null && CALL_SYNTAX.matcher(queryString).matches();
        }

        @Override
        public void init(CallableStatement statement) {
            this.statement = statement;
            try {
                statement.registerOutParameter(1, Types.REF_CURSOR);
            } catch (SQLException e) {
                throw new IllegalStateException("Failed to register REF CURSOR OUT parameter", e);
            }
        }

        @Override
        public boolean setParameterValue(int index, Class<?> parameterType, Object value) {
            // The REF CURSOR OUT parameter has no IN value to bind — already registered in
            // init(). Returning true tells JRJdbcQueryExecuter to skip its own generic binding,
            // which would otherwise try to bind a null java.sql.ResultSet and fail.
            return ResultSet.class.equals(parameterType);
        }

        @Override
        public ResultSet execute() throws SQLException {
            statement.execute();
            return (ResultSet) statement.getObject(1);
        }
    }
}
