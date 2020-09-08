package net.moznion.mysql.diff;

import com.mysql.cj.jdbc.exceptions.CommunicationsException;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

@RunWith(Enclosed.class)
public class SchemaDumperTest {
  private static final String SQL_FOR_TEST = "CREATE TABLE `sample` (\n"
      + "  `id` int(10) NOT NULL AUTO_INCREMENT,\n"
      + "  PRIMARY KEY (`id`)\n"
      + ") ENGINE=InnoDB DEFAULT CHARSET=utf8;\n";

  public static class ForConstructors {
    @Test
    public void shouldInstantiateAsDefault()
        throws SQLException, IOException, InterruptedException {
      SchemaDumper schemaDumper = new SchemaDumper();
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
    public void shouldInstantiateByMysqldumpPath() throws SQLException, IOException,
        InterruptedException {
      SchemaDumper schemaDumper = new SchemaDumper("mysqldump");
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
    public void shouldInstantiateByLocalMySqlConnectionInfo() throws SQLException, IOException,
        InterruptedException {
      SchemaDumper schemaDumper = new SchemaDumper(MySqlConnectionInfo.builder().build());
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
    public void shouldInstantiateByAllArgs()
        throws SQLException, IOException, InterruptedException {
      SchemaDumper schemaDumper =
          new SchemaDumper(MySqlConnectionInfo.builder().build(), "mysqldump");
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
    public void shouldRaiseIllegalArgumentExceptionByNullConnectionInfo() {
      try {
        new SchemaDumper(null, "mysqldump");
      } catch (IllegalArgumentException e) {
        assertTrue(true);
        return;
      } catch (Exception e) {
        assertTrue(false);
      }
      assertTrue(false);
    }

    @Test
    public void shouldRaiseIllegalArgumentExceptionByNullMysqldumpPath() {
      try {
        new SchemaDumper(MySqlConnectionInfo.builder().build(), null);
      } catch (IllegalArgumentException e) {
        assertTrue(true);
        return;
      } catch (Exception e) {
        assertTrue(false);
      }
      assertTrue(false);
    }
  }

  public static class ForDumpMethods {
    private SchemaDumper schemaDumper = new SchemaDumper();

    @Test
    public void shouldDumpBySqlString() throws SQLException, IOException, InterruptedException {
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
    public void shouldDumpBySqlFileWithDefaultCharset()
        throws IOException, SQLException, InterruptedException {
      File sqlFile = File.createTempFile("tempsql", ".sql");

      try (BufferedWriter bufferedWriter =
          new BufferedWriter(new OutputStreamWriter(new FileOutputStream(sqlFile),
              Charset.forName("UTF-8")))) {
        bufferedWriter.write(SQL_FOR_TEST);
      }

      try {
        schemaDumper.dump(sqlFile);
        assertTrue(true);
      } catch (CommunicationsException e) {
        assumeTrue("MySQL maybe not launched", false);
      } catch (Exception e) {
        assertTrue(false);
      } finally {
        sqlFile.delete();
      }
    }

    @Test
    public void shouldDumpBySqlFileWithSpecifiedCharset()
        throws IOException, SQLException, InterruptedException {
      File sqlFile = File.createTempFile("tempsql", ".sql");

      try (BufferedWriter bufferedWriter =
          new BufferedWriter(new OutputStreamWriter(new FileOutputStream(sqlFile),
              Charset.forName("EUC-JP")))) {
        bufferedWriter.write(SQL_FOR_TEST);
      }

      try {
        schemaDumper.dump(sqlFile, Charset.forName("EUC-JP"));
        assertTrue(true);
      } catch (CommunicationsException e) {
        assumeTrue("MySQL maybe not launched", false);
      } catch (Exception e) {
        assertTrue(false);
      } finally {
        sqlFile.delete();
      }

    }

    @Test
    public void shouldDumpFromLocalMySql()
        throws SQLException, IOException, InterruptedException {
      MySqlConnectionInfo connInfo = MySqlConnectionInfo.builder().build();

      String tempDbName = new StringBuilder()
          .append("tmp_")
          .append(UUID.randomUUID().toString().replaceAll("-", ""))
          .toString();
      String mysqlUrl = connInfo.getJdbcUrl();
      String user = connInfo.getUser();
      String pass = connInfo.getPass();

      try (Connection connection = DriverManager.getConnection(mysqlUrl, user, pass)) {
        try (Statement stmt = connection.createStatement()) {
          stmt.executeUpdate("CREATE DATABASE " + tempDbName);
        }
        try (Statement stmt = connection.createStatement()) {
          stmt.executeUpdate("USE " + tempDbName + "; " + SQL_FOR_TEST);
        }

        schemaDumper.dumpFromLocalDb(tempDbName);
      } catch (Exception e) {
        throw e;
      } finally {
        try (Connection connectionToTeardown = DriverManager.getConnection(mysqlUrl, user, pass)) {
          try (Statement stmt = connectionToTeardown.createStatement()) {
            stmt.executeUpdate("DROP DATABASE " + tempDbName);
          }
        } catch (CommunicationsException e) {
          assumeTrue("MySQL maybe not launched", false);
        }
      }
      assertTrue(true);
    }

    @Test
    public void shouldDumpFromRemoteMySql() throws SQLException, IOException, InterruptedException {
      MySqlConnectionInfo connInfo = MySqlConnectionInfo.builder().build();

      String tempDbName = new StringBuilder()
          .append("tmp_")
          .append(UUID.randomUUID().toString().replaceAll("-", ""))
          .toString();
      String mysqlUrl = connInfo.getJdbcUrl();
      String user = connInfo.getUser();
      String pass = connInfo.getPass();

      try (Connection connection = DriverManager.getConnection(mysqlUrl, user, pass)) {
        try (Statement stmt = connection.createStatement()) {
          stmt.executeUpdate("CREATE DATABASE " + tempDbName);
        }
        try (Statement stmt = connection.createStatement()) {
          stmt.executeUpdate("USE " + tempDbName + "; " + SQL_FOR_TEST);
        }
        schemaDumper.dumpFromRemoteDb(tempDbName, connInfo);
      } catch (Exception e) {
        throw e;
      } finally {
        try (Connection connectionToTeardown = DriverManager.getConnection(mysqlUrl, user, pass)) {
          try (Statement stmt = connectionToTeardown.createStatement()) {
            stmt.executeUpdate("DROP DATABASE " + tempDbName);
          }
        } catch (CommunicationsException e) {
          assumeTrue("MySQL maybe not launched", false);
        }
      }
      assertTrue(true);
    }
  }
}
