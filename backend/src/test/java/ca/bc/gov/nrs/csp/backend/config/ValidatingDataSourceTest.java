package ca.bc.gov.nrs.csp.backend.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class ValidatingDataSourceTest {

    private static final String VALIDATION_QUERY = "SELECT 1 FROM DUAL";

    @Mock
    private DataSource target;

    @Mock
    private Connection connection;

    @Mock
    private Statement statement;

    @Test
    void constructor_validationSucceeds_wrapsTargetDataSource() throws SQLException {
        given(target.getConnection()).willReturn(connection);
        given(connection.createStatement()).willReturn(statement);

        ValidatingDataSource dataSource = new ValidatingDataSource(target);

        assertThat(dataSource.getTargetDataSource()).isSameAs(target);
        then(statement).should().execute(VALIDATION_QUERY);
    }

    @Test
    void constructor_validationSucceeds_closesConnectionAndStatement() throws SQLException {
        given(target.getConnection()).willReturn(connection);
        given(connection.createStatement()).willReturn(statement);

        new ValidatingDataSource(target);

        then(statement).should().close();
        then(connection).should().close();
    }

    @Test
    void constructor_getConnectionFails_throwsIllegalStateException() throws SQLException {
        SQLException cause = new SQLException("invalid credentials");
        given(target.getConnection()).willThrow(cause);

        assertThatThrownBy(() -> new ValidatingDataSource(target))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Datasource validation failed. Check SPRING_DATASOURCE_URL/USERNAME/PASSWORD.")
                .hasCause(cause);
    }

    @Test
    void constructor_createStatementFails_throwsIllegalStateExceptionAndClosesConnection() throws SQLException {
        SQLException cause = new SQLException("connection closed");
        given(target.getConnection()).willReturn(connection);
        given(connection.createStatement()).willThrow(cause);

        assertThatThrownBy(() -> new ValidatingDataSource(target))
                .isInstanceOf(IllegalStateException.class)
                .hasCause(cause);
        then(connection).should().close();
    }

    @Test
    void constructor_validationQueryFails_throwsIllegalStateExceptionAndClosesResources() throws SQLException {
        SQLException cause = new SQLException("ORA-00942: table or view does not exist");
        given(target.getConnection()).willReturn(connection);
        given(connection.createStatement()).willReturn(statement);
        given(statement.execute(VALIDATION_QUERY)).willThrow(cause);

        assertThatThrownBy(() -> new ValidatingDataSource(target))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Datasource validation failed")
                .hasCause(cause);
        then(statement).should().close();
        then(connection).should().close();
    }

    // Delegated methods (getConnection variants, unwrap, isWrapperFor, log writer, timeout)
    // are inherited from Spring's DelegatingDataSource, not declared in ValidatingDataSource
    // itself. The checks below assert the delegation behaviour of the wrapper instance.

    @Test
    void getConnection_afterValidation_delegatesToTarget() throws SQLException {
        given(target.getConnection()).willReturn(connection);
        given(connection.createStatement()).willReturn(statement);
        ValidatingDataSource dataSource = new ValidatingDataSource(target);

        assertThat(dataSource.getConnection()).isSameAs(connection);
    }

    @Test
    void getConnectionWithCredentials_delegatesToTarget() throws SQLException {
        given(target.getConnection()).willReturn(connection);
        given(connection.createStatement()).willReturn(statement);
        ValidatingDataSource dataSource = new ValidatingDataSource(target);

        given(target.getConnection("user", "pass")).willReturn(connection);

        assertThat(dataSource.getConnection("user", "pass")).isSameAs(connection);
    }

    @Test
    void unwrapAndIsWrapperFor_forDataSourceInterface_returnSelf() throws SQLException {
        given(target.getConnection()).willReturn(connection);
        given(connection.createStatement()).willReturn(statement);
        ValidatingDataSource dataSource = new ValidatingDataSource(target);

        assertThat(dataSource.unwrap(DataSource.class)).isSameAs(dataSource);
        assertThat(dataSource.isWrapperFor(DataSource.class)).isTrue();
    }
}
