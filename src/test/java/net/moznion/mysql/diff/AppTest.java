package net.moznion.mysql.diff;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.nio.charset.Charset;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.UUID;

import org.junit.Test;

public class AppTest {
  private static final String SQL_FOR_TEST = "CREATE TABLE `sample` (\n" +
      "  `id` int(10) NOT NULL AUTO_INCREMENT,\n" +
      "  PRIMARY KEY (`id`)\n" +
      ") ENGINE=InnoDB DEFAULT CHARSET=utf8;\n";

  @Test
  public void shouldShowVersion() throws IOException, SQLException, InterruptedException {
    // For short option
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    System.setOut(new PrintStream(baos));

    String argsForShortOpt[] = {"-v"};
    App.main(argsForShortOpt);

    String versionString = baos.toString();
    System.setOut(new PrintStream(new FileOutputStream(FileDescriptor.out)));
    assertEquals("Missing Version\n", versionString);

    // For long option
    baos = new ByteArrayOutputStream();
    System.setOut(new PrintStream(baos));

    String argsForLongOpt[] = {"--version"};
    App.main(argsForLongOpt);

    versionString = baos.toString();
    System.setOut(new PrintStream(new FileOutputStream(FileDescriptor.out)));
    assertEquals("Missing Version\n", versionString);
  }

  @Test
  public void shouldShowUsageByOption() throws IOException, SQLException, InterruptedException {
    String expectedUsageString = "[Usage]\n" +
        "    java -jar <old_database> <new_database>\n" +
        "[Examples]\n" +
        "* Take diff between createtable1.sql and createtable2.sql " +
        "(both of them are SQL file which are on your machine)\n" +
        "    java -jar createtable1.sql createtable2.sql\n" +
        "* Take diff between dbname1 and dbname2 " +
        "(both of databases on the local MySQL)\n" +
        "    java -jar dbname1 dbname2\n" +
        "* Take diff between dbname1 and dbname2 " +
        "(both of databases on remote MySQL)\n" +
        "    java -jar '-uroot -hlocalhost dbname1' '-uroot -hlocalhost dbname2'" +
        "\n" +
        "[Options]\n" +
        "    -h, --help:    Show usage\n" +
        "    -v, --version: Show version\n";

    // For short option
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    System.setOut(new PrintStream(baos));

    String argsForShortOpt[] = {"-h"};
    App.main(argsForShortOpt);

    String usageString = baos.toString();

    System.setOut(new PrintStream(new FileOutputStream(FileDescriptor.out)));
    assertEquals(expectedUsageString, usageString);

    // For long option
    baos = new ByteArrayOutputStream();
    System.setOut(new PrintStream(baos));

    String argsForLongOpt[] = {"--help"};
    App.main(argsForLongOpt);

    usageString = baos.toString();

    System.setOut(new PrintStream(new FileOutputStream(FileDescriptor.out)));
    assertEquals(expectedUsageString, usageString);
  }

  @Test
  public void shouldTakeDiffBetweenFiles() throws IOException, SQLException, InterruptedException {
    File sqlFile1 = File.createTempFile("tempsql1", ".sql");
    File sqlFile2 = File.createTempFile("tempsql2", ".sql");

    for (File sqlFile : Arrays.asList(sqlFile1, sqlFile2)) {
      try (BufferedWriter bufferedWriter = new BufferedWriter
          (new OutputStreamWriter(new FileOutputStream(sqlFile), Charset.forName("UTF-8")))) {
        bufferedWriter.write(SQL_FOR_TEST);
      }
    }

    String args[] = {sqlFile1.getAbsolutePath(), sqlFile2.getAbsolutePath()};
    App.main(args);

    assertTrue(true);

    sqlFile1.delete();
    sqlFile2.delete();
  }

  @Test
  @edu.umd.cs.findbugs.annotations.SuppressFBWarnings(
      value = "SQL_NONCONSTANT_STRING_PASSED_TO_EXECUTE")
  public void shouldTakeDiffBetweenLocalDB() throws IOException, SQLException, InterruptedException {
    MySQLConnectionInfo connInfo = MySQLConnectionInfo.builder().build();

    String tempDBName1 = new StringBuilder()
        .append("tmp_")
        .append(UUID.randomUUID().toString().replaceAll("-", ""))
        .toString();
    String tempDBName2 = new StringBuilder()
        .append("tmp_")
        .append(UUID.randomUUID().toString().replaceAll("-", ""))
        .toString();

    String mysqlURL = connInfo.getJdbcURL();
    String user = connInfo.getUser();
    String pass = connInfo.getPass();

    try (Connection connection = DriverManager.getConnection(mysqlURL, user, pass)) {
      for (String dbName : Arrays.asList(tempDBName1, tempDBName2)) {
        try (Statement stmt = connection.createStatement()) {
          stmt.executeUpdate("CREATE DATABASE " + dbName);
        }
        try (Statement stmt = connection.createStatement()) {
          stmt.executeUpdate("USE " + dbName + "; " + SQL_FOR_TEST);
        }
      }

      String args[] = {tempDBName1, tempDBName2};
      App.main(args);
    } catch (Exception e) {
      assertTrue(false);
      throw e;
    } finally {
      try (Connection connectionToTeardown = DriverManager.getConnection(mysqlURL, user, pass)) {
        for (String dbName : Arrays.asList(tempDBName1, tempDBName2)) {
          try (Statement stmt = connectionToTeardown.createStatement()) {
            stmt.executeUpdate("DROP DATABASE " + dbName);
          }
        }
      }
    }

    assertTrue(true);
  }

  @Test
  @edu.umd.cs.findbugs.annotations.SuppressFBWarnings(
      value = "SQL_NONCONSTANT_STRING_PASSED_TO_EXECUTE")
  public void shouldTakeDiffBetweenRemoteDB() throws IOException, SQLException,
      InterruptedException {
    MySQLConnectionInfo connInfo = MySQLConnectionInfo.builder().build();

    final String tempDBName1 = new StringBuilder()
        .append("tmp_")
        .append(UUID.randomUUID().toString().replaceAll("-", ""))
        .toString();
    final String tempDBName2 = new StringBuilder()
        .append("tmp_")
        .append(UUID.randomUUID().toString().replaceAll("-", ""))
        .toString();

    String mysqlURL = connInfo.getJdbcURL();
    String user = connInfo.getUser();
    String pass = connInfo.getPass();

    try (Connection connection = DriverManager.getConnection(mysqlURL, user, pass)) {
      for (String dbName : Arrays.asList(tempDBName1, tempDBName2)) {
        try (Statement stmt = connection.createStatement()) {
          stmt.executeUpdate("CREATE DATABASE " + dbName);
        }
        try (Statement stmt = connection.createStatement()) {
          stmt.executeUpdate("USE " + dbName + "; " + SQL_FOR_TEST);
        }
      }

      String args[] = {
          "'-u root -h localhost " + tempDBName1 + "'",
          "'-u root -h localhost " + tempDBName2 + "'"
      };
      App.main(args);
    } catch (Exception e) {
      throw e;
    } finally {
      try (Connection connectionToTeardown = DriverManager.getConnection(mysqlURL, user, pass)) {
        for (String dbName : Arrays.asList(tempDBName1, tempDBName2)) {
          try (Statement stmt = connectionToTeardown.createStatement()) {
            stmt.executeUpdate("DROP DATABASE " + dbName);
          }
        }
      }
    }

    assertTrue(true);
  }
}
