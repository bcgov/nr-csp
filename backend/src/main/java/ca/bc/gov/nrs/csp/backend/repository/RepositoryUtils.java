package ca.bc.gov.nrs.csp.backend.repository;

import java.sql.CallableStatement;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDate;

/**
 * Shared helpers for binding Oracle CallableStatement parameters and reading ResultSet columns.
 * All repositories use these methods to ensure consistent null handling and type mapping.
 */
public final class RepositoryUtils {

    private RepositoryUtils() {}

    // ---------------------------------------------------------------
    // CallableStatement setters
    // ---------------------------------------------------------------

    public static void setLongOrNull(CallableStatement cs, int idx, Long value) throws SQLException {
        if (value == null) {
            cs.setNull(idx, Types.NUMERIC);
        } else {
            cs.setLong(idx, value);
        }
    }

    public static void setStringOrNull(CallableStatement cs, int idx, String value) throws SQLException {
        if (value == null || value.isBlank()) {
            cs.setNull(idx, Types.VARCHAR);
        } else {
            cs.setString(idx, value);
        }
    }

    public static void setLocalDateOrNull(CallableStatement cs, int idx, LocalDate value) throws SQLException {
        if (value == null) {
            cs.setNull(idx, Types.DATE);
        } else {
            cs.setDate(idx, Date.valueOf(value));
        }
    }

    // ---------------------------------------------------------------
    // ResultSet getters
    // ---------------------------------------------------------------

    public static Long getLongNullable(ResultSet rs, String column) throws SQLException {
        long value = rs.getLong(column);
        return rs.wasNull() ? null : value;
    }

    public static LocalDate getLocalDateNullable(ResultSet rs, String column) throws SQLException {
        Date value = rs.getDate(column);
        return value == null ? null : value.toLocalDate();
    }

    /**
     * Reads an Oracle Y/N VARCHAR2(1) column and converts it to a boolean.
     * Returns {@code false} for any value that is not exactly "Y".
     */
    public static boolean getYNFlag(ResultSet rs, String column) throws SQLException {
        String value = rs.getString(column);
        return "Y".equalsIgnoreCase(value);
    }
}
