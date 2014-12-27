package net.moznion.mysql.diff;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Parser for SQL table definition.
 * 
 * @author moznion
 *
 */
public class SchemaDumper {
  private static final String LINE_SEPARATOR = System.lineSeparator();

  private final MySQLConnectionInfo mysqlConnectionInfo;
  private final String mysqldumpPath;

  public SchemaDumper(MySQLConnectionInfo mysqlConnectionInfo, String mysqldumpPath) {
    if (mysqlConnectionInfo == null) {
      throw new IllegalArgumentException("mysqlConnectionInfo must not be null");
    }

    if (mysqldumpPath == null) {
      throw new IllegalArgumentException("mysqldumpPath must not be null");
    }

    this.mysqlConnectionInfo = mysqlConnectionInfo;
    this.mysqldumpPath = mysqldumpPath;
  }

  public SchemaDumper(MySQLConnectionInfo mySQLConnectionInfo) {
    this(mySQLConnectionInfo, "mysqldump");
  }
  
  public SchemaDumper(String mysqldumpPath) {
    this(MySQLConnectionInfo.builder().build(), mysqldumpPath);
  }

  public SchemaDumper() {
    this(MySQLConnectionInfo.builder().build());
  }

  private String fetchSchemaViaMysqldump(String dbName)
      throws IOException, InterruptedException, SQLException {
    String schema;
    List<String> mysqldumpCommand = Arrays.asList(
        mysqldumpPath,
        new StringBuilder().append("-u").append(mysqlConnectionInfo.getUser()).toString(),
        "--no-data=true",
        dbName);

    ProcessBuilder processBuilder = new ProcessBuilder(mysqldumpCommand);

    Process process = processBuilder.start();
    try (InputStream inputStream = process.getInputStream()) {
      StringBuilder stdoutStringBuilder = new StringBuilder();
      try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream))) {
        String line;
        while ((line = bufferedReader.readLine()) != null) {
          stdoutStringBuilder.append(line).append(LINE_SEPARATOR);
        }
      }
      schema = stdoutStringBuilder.toString();
    }

    if (process.waitFor() != 0) {
      throw new RuntimeException(
          new StringBuilder()
              .append("Failed to execute `mysqldump` command (command: ")
              .append(String.join(" ", mysqldumpCommand))
              .append(")")
              .toString());
    }

    return schema;
  }

  public String dump(String sql) throws SQLException, IOException, InterruptedException {
    String tempDBName = new StringBuilder()
        .append("tmp_")
        .append(UUID.randomUUID().toString().replaceAll("-", ""))
        .toString();

    String mysqlURL = mysqlConnectionInfo.getURL();
    String mysqlUser = mysqlConnectionInfo.getUser();
    String mysqlPass = mysqlConnectionInfo.getPass();
    try (Connection connection = DriverManager.getConnection(mysqlURL, mysqlUser, mysqlPass)) {
      try (Statement stmt = connection.createStatement()) {
        stmt.executeUpdate("CREATE DATABASE " + tempDBName);
      }

      try (Connection dbSpecifiedConnection = DriverManager.getConnection(
          new StringBuilder().append(mysqlURL).append("/").append(tempDBName).toString(),
          mysqlUser, mysqlPass)) {
        try (Statement stmt = dbSpecifiedConnection.createStatement()) {
          stmt.executeUpdate(sql);
        }
      }

      return fetchSchemaViaMysqldump(tempDBName);
    } catch (Exception e) {
      throw e;
    } finally {
      try (Connection connection = DriverManager.getConnection(mysqlURL, mysqlUser, mysqlPass)) {
        try (Statement stmt = connection.createStatement()) {
          stmt.executeUpdate("DROP DATABASE " + tempDBName);
        }
      }
    }
  }

  public String dump(File sqlFile, Charset charset) throws IOException, SQLException,
      InterruptedException {
    String sqlString =
        new String(Files.readAllBytes(Paths.get(sqlFile.getAbsolutePath())), charset);
    return dump(sqlString);
  }

  public String dump(File sqlFile) throws IOException, SQLException, InterruptedException {
    return dump(sqlFile, StandardCharsets.UTF_8);
  }

  public String dumpFromLocalDB(String dbName) throws IOException, InterruptedException,
      SQLException {
    return fetchSchemaViaMysqldump(dbName);
  }
}
