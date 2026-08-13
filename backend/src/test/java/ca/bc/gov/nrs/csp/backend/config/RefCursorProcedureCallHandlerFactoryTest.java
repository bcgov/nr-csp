package ca.bc.gov.nrs.csp.backend.config;

import net.sf.jasperreports.engine.query.ProcedureCallHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RefCursorProcedureCallHandlerFactoryTest {

    @Mock
    CallableStatement statement;

    RefCursorProcedureCallHandlerFactory factory;
    ProcedureCallHandler handler;

    @BeforeEach
    void setUp() {
        factory = new RefCursorProcedureCallHandlerFactory();
        handler = factory.createProcedureCallHandler();
    }

    @Nested
    @DisplayName("isHandling()")
    class IsHandling {

        @Test
        void shouldMatch_plainCallSyntax() throws SQLException {
            assertThat(handler.isHandling(null, "{call CSP_SP_RPT_06 (?, ?, ?)}")).isTrue();
        }

        @Test
        void shouldMatch_callSyntaxAcrossMultipleLines() throws SQLException {
            assertThat(handler.isHandling(null, "{call CSP_SP_RPT_07 (?, ?,\n ?, ?)}")).isTrue();
        }

        @Test
        void shouldMatch_functionCallReturnValueSyntax() throws SQLException {
            assertThat(handler.isHandling(null, "{? = call SOME_FUNCTION(?)}")).isTrue();
        }

        @Test
        void shouldNotMatch_plainSelectQuery() throws SQLException {
            assertThat(handler.isHandling(null, "select * from invoice where invoice_id = $P{ID}")).isFalse();
        }

        @Test
        void shouldNotMatch_nullQueryString() throws SQLException {
            assertThat(handler.isHandling(null, null)).isFalse();
        }
    }

    @Nested
    @DisplayName("init()")
    class Init {

        @Test
        void shouldRegisterRefCursorAtPositionOne() throws SQLException {
            handler.init(statement);

            verify(statement).registerOutParameter(1, Types.REF_CURSOR);
        }

        @Test
        void shouldWrapSqlException_asUnchecked() throws SQLException {
            willThrow(new SQLException("boom")).given(statement).registerOutParameter(1, Types.REF_CURSOR);

            assertThatThrownBy(() -> handler.init(statement))
                    .isInstanceOf(IllegalStateException.class)
                    .hasCauseInstanceOf(SQLException.class);
        }
    }

    @Nested
    @DisplayName("setParameterValue()")
    class SetParameterValue {

        @Test
        void shouldReturnTrue_forResultSetType() throws SQLException {
            assertThat(handler.setParameterValue(1, ResultSet.class, null)).isTrue();
        }

        @Test
        void shouldReturnFalse_forOtherTypes_soDefaultBindingStillApplies() throws SQLException {
            assertThat(handler.setParameterValue(2, String.class, "20200101")).isFalse();
        }
    }

    @Nested
    @DisplayName("execute()")
    class Execute {

        @Test
        void shouldExecuteStatementAndReturnRegisteredCursor() throws SQLException {
            ResultSet cursorResult = mock(ResultSet.class);
            given(statement.getObject(1)).willReturn(cursorResult);
            handler.init(statement);

            ResultSet result = handler.execute();

            verify(statement).execute();
            assertThat(result).isSameAs(cursorResult);
        }
    }
}
