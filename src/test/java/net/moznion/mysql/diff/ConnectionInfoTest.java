package net.moznion.mysql.diff;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.net.URISyntaxException;

public class ConnectionInfoTest {
  @Test
  public void shouldConnectSuccessfullyThroughFullyUrl() {
    try {
      new SchemaDumper(
          MySqlConnectionInfo
              .builder()
              .url(
                  "jdbc:log4j:mysql://localhost:3306/something_table?"
                      + "cacheServerConfiguration=true&useLocalSessionState=true")
              .build());
      assertTrue(true);
    } catch (Exception e) {
      assertTrue(false);
    }
  }

  @Test
  public void shouldConnectSuccessfullyThroughUrlWithoutPort() {
    try {
      new SchemaDumper(
          MySqlConnectionInfo
              .builder()
              .url(
                  "jdbc:log4j:mysql://localhost/something_table?"
                      + "cacheServerConfiguration=true&useLocalSessionState=true")
              .build());
      assertTrue(true);
    } catch (Exception e) {
      assertTrue(false);
    }
  }

  @Test
  public void shouldConnectSuccessfullyThroughUrlWithoutOptions() {
    try {
      new SchemaDumper(MySqlConnectionInfo.builder()
          .url("jdbc:log4j:mysql://localhost/something_table")
          .build());
      assertTrue(true);
    } catch (Exception e) {
      assertTrue(false);
    }
  }

  @Test
  public void shouldConnectSuccessfullyThroughUrlWithoutConnectionPrefix() {
    try {
      new SchemaDumper(
          MySqlConnectionInfo
              .builder()
              .url("localhost/something_table")
              .build());
      assertTrue(false);
    } catch (URISyntaxException e) {
      assertTrue(true);
    } catch (Exception e) {
      assertTrue(false);
    }
  }
}
