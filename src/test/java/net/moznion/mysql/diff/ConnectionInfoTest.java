package net.moznion.mysql.diff;

import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

import com.mysql.jdbc.CommunicationsException;

import org.junit.Test;

public class ConnectionInfoTest {
  private static final String SQL_FOR_TEST = "CREATE TABLE `sample` (\n"
      + "  `id` int(10) NOT NULL AUTO_INCREMENT,\n"
      + "  PRIMARY KEY (`id`)\n"
      + ") ENGINE=InnoDB DEFAULT CHARSET=utf8;\n";

  @Test
  public void shouldConnectSuccessfullyThroughFullyUrl() {
    SchemaDumper schemaDumper =
        new SchemaDumper(
            MySqlConnectionInfo
                .builder()
                .url(
                    "jdbc:mysql://localhost:3306/something_table?cacheServerConfiguration=true&useLocalSessionState=true")
                .build());
    try {
      schemaDumper.dump(SQL_FOR_TEST);
      assertTrue(true);
    } catch (CommunicationsException e) {
      assumeTrue("MySQL maybe not launched", false);
    } catch (Exception e) {
      assertTrue(false);
    }
  }

  @Test
  public void shouldConnectSuccessfullyThroughUrlWithoutPort() {
    SchemaDumper schemaDumper =
        new SchemaDumper(
            MySqlConnectionInfo
                .builder()
                .url(
                    "jdbc:mysql://localhost/something_table?cacheServerConfiguration=true&useLocalSessionState=true")
                .build());
    try {
      schemaDumper.dump(SQL_FOR_TEST);
      assertTrue(true);
    } catch (CommunicationsException e) {
      assumeTrue("MySQL maybe not launched", false);
    } catch (Exception e) {
      assertTrue(false);
    }
  }

  @Test
  public void shouldConnectSuccessfullyThroughUrlWithoutOptions() {
    SchemaDumper schemaDumper = new SchemaDumper(MySqlConnectionInfo.builder()
        .url("jdbc:mysql://localhost/something_table")
        .build());
    try {
      schemaDumper.dump(SQL_FOR_TEST);
      assertTrue(true);
    } catch (CommunicationsException e) {
      assumeTrue("MySQL maybe not launched", false);
    } catch (Exception e) {
      assertTrue(false);
    }
  }

  @Test
  public void shouldConnectSuccessfullyThroughUrlWithoutConnectionPrefix() {
    SchemaDumper schemaDumper =
        new SchemaDumper(
            MySqlConnectionInfo
                .builder()
                .url("localhost/something_table")
                .build());
    try {
      schemaDumper.dump(SQL_FOR_TEST);
      assertTrue(true);
    } catch (CommunicationsException e) {
      assumeTrue("MySQL maybe not launched", false);
    } catch (Exception e) {
      assertTrue(false);
    }
  }
}
