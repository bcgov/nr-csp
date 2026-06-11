package ca.bc.gov.nrs.csp.backend.repository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.CallableStatement;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.Types;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RepositoryUtilsTest {

    @Mock CallableStatement cs;
    @Mock ResultSet rs;

    // ---------------------------------------------------------------
    // setLongOrNull
    // ---------------------------------------------------------------

    @Test
    void setLongOrNull_setsValue_whenPresent() throws Exception {
        RepositoryUtils.setLongOrNull(cs, 1, 42L);
        verify(cs).setLong(1, 42L);
    }

    @Test
    void setLongOrNull_setsNull_whenAbsent() throws Exception {
        RepositoryUtils.setLongOrNull(cs, 1, null);
        verify(cs).setNull(1, Types.NUMERIC);
    }

    // ---------------------------------------------------------------
    // setStringOrNull
    // ---------------------------------------------------------------

    @Test
    void setStringOrNull_setsValue_whenPresent() throws Exception {
        RepositoryUtils.setStringOrNull(cs, 2, "hello");
        verify(cs).setString(2, "hello");
    }

    @Test
    void setStringOrNull_setsNull_whenNull() throws Exception {
        RepositoryUtils.setStringOrNull(cs, 2, null);
        verify(cs).setNull(2, Types.VARCHAR);
    }

    @Test
    void setStringOrNull_setsNull_whenBlank() throws Exception {
        RepositoryUtils.setStringOrNull(cs, 2, "   ");
        verify(cs).setNull(2, Types.VARCHAR);
    }

    // ---------------------------------------------------------------
    // setLocalDateOrNull
    // ---------------------------------------------------------------

    @Test
    void setLocalDateOrNull_setsValue_whenPresent() throws Exception {
        LocalDate date = LocalDate.of(2024, 1, 15);
        RepositoryUtils.setLocalDateOrNull(cs, 3, date);
        verify(cs).setDate(3, Date.valueOf(date));
    }

    @Test
    void setLocalDateOrNull_setsNull_whenAbsent() throws Exception {
        RepositoryUtils.setLocalDateOrNull(cs, 3, null);
        verify(cs).setNull(3, Types.DATE);
    }

    // ---------------------------------------------------------------
    // getLongNullable
    // ---------------------------------------------------------------

    @Test
    void getLongNullable_returnsValue_whenPresent() throws Exception {
        when(rs.getLong("id")).thenReturn(99L);
        when(rs.wasNull()).thenReturn(false);
        assertThat(RepositoryUtils.getLongNullable(rs, "id")).isEqualTo(99L);
    }

    @Test
    void getLongNullable_returnsNull_whenDbNull() throws Exception {
        when(rs.getLong("id")).thenReturn(0L);
        when(rs.wasNull()).thenReturn(true);
        assertThat(RepositoryUtils.getLongNullable(rs, "id")).isNull();
    }

    // ---------------------------------------------------------------
    // getLocalDateNullable
    // ---------------------------------------------------------------

    @Test
    void getLocalDateNullable_returnsDate_whenPresent() throws Exception {
        LocalDate expected = LocalDate.of(2025, 6, 1);
        when(rs.getDate("dt")).thenReturn(Date.valueOf(expected));
        assertThat(RepositoryUtils.getLocalDateNullable(rs, "dt")).isEqualTo(expected);
    }

    @Test
    void getLocalDateNullable_returnsNull_whenDbNull() throws Exception {
        when(rs.getDate("dt")).thenReturn(null);
        assertThat(RepositoryUtils.getLocalDateNullable(rs, "dt")).isNull();
    }

    // ---------------------------------------------------------------
    // getYNFlag
    // ---------------------------------------------------------------

    @Test
    void getYNFlag_returnsTrue_forY() throws Exception {
        when(rs.getString("active")).thenReturn("Y");
        assertThat(RepositoryUtils.getYNFlag(rs, "active")).isTrue();
    }

    @Test
    void getYNFlag_returnsTrue_forLowercaseY() throws Exception {
        when(rs.getString("active")).thenReturn("y");
        assertThat(RepositoryUtils.getYNFlag(rs, "active")).isTrue();
    }

    @Test
    void getYNFlag_returnsFalse_forN() throws Exception {
        when(rs.getString("active")).thenReturn("N");
        assertThat(RepositoryUtils.getYNFlag(rs, "active")).isFalse();
    }

    @Test
    void getYNFlag_returnsFalse_forNull() throws Exception {
        when(rs.getString("active")).thenReturn(null);
        assertThat(RepositoryUtils.getYNFlag(rs, "active")).isFalse();
    }
}
