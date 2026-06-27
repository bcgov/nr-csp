package ca.bc.gov.nrs.csp.backend.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;

@Configuration
public class DataSourceConfig {

  @Bean
  @Primary
  public DataSource dataSource(
    @Value("${spring.datasource.url}") String url,
    @Value("${spring.datasource.username}") String username,
    @Value("${spring.datasource.password}") String password,
    @Value("${spring.datasource.driver-class-name:oracle.jdbc.OracleDriver}") String driverClass) {
    HikariDataSource ds = DataSourceBuilder.create()
      .type(HikariDataSource.class)
      .url(url)
      .username(username)
      .password(password)
      .driverClassName(driverClass)
      .build();
    // Pass wallet password as a connection property (not a -D JVM flag) so it stays
    // off the process table and out of JMX/thread dumps.
    String walletPassword = System.getenv("KEYSTORE_SECRET");
    if (walletPassword != null && !walletPassword.isEmpty()) {
      ds.addDataSourceProperty("oracle.net.wallet_password", walletPassword);
    }
    return new ValidatingDataSource(ds);
  }
}
