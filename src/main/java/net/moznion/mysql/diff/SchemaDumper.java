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
import java.util.ArrayList;
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

  private final MySqlConnectionInfo localMySqlConnectionInfo;
  private final String mysqldumpPath;

  public SchemaDumper(MySqlConnectionInfo localMySqlConnectionInfo, String mysqldumpPath) {
    if (localMySqlConnectionInfo == null) {
      throw new IllegalArgumentException("mysqlConnectionInfo must not be null");
    }

    if (mysqldumpPath == null) {
      throw new IllegalArgumentException("mysqldumpPath must not be null");
    }

    this.localMySqlConnectionInfo = localMySqlConnectionInfo;
    this.mysqldumpPath = mysqldumpPath;
  }

  public SchemaDumper(MySqlConnectionInfo localMySqlConnectionInfo) {
    this(localMySqlConnectionInfo, "mysqldump");
  }

  public SchemaDumper(String mysqldumpPath) {
    this(MySqlConnectionInfo.builder().build(), mysqldumpPath);
  }

  public SchemaDumper() {
    this(MySqlConnectionInfo.builder().build());
  }

  public String dump(String sql) throws SQLException, IOException, InterruptedException {
    String tempDbName = new StringBuilder()
        .append("tmp_")
        .append(UUID.randomUUID().toString().replaceAll("-", ""))
        .toString();

    String mysqlUrl = localMySqlConnectionInfo.getJdbcUrl();
    String mysqlUser = localMySqlConnectionInfo.getUser();
    String mysqlPass = localMySqlConnectionInfo.getPass();
    try (Connection connection = DriverManager.getConnection(mysqlUrl, mysqlUser, mysqlPass)) {
      try (Statement stmt = connection.createStatement()) {
        stmt.executeUpdate("CREATE DATABASE " + tempDbName);
      }

      try (Statement stmt = connection.createStatement()) {
        stmt.execute(new StringBuilder()
            .append("USE ")
            .append(tempDbName)
            .append("; ")
            .append(sql)
            .toString());
      }

      return fetchSchemaViaMysqldump(tempDbName);
    } catch (Exception e) {
      throw e;
    } finally {
      try (Connection connection = DriverManager.getConnection(mysqlUrl, mysqlUser, mysqlPass)) {
        try (Statement stmt = connection.createStatement()) {
          stmt.executeUpdate("DROP DATABASE " + tempDbName);
        }
      }
    }
  }

  public String dump(File sqlFile, Charset charset)
      throws IOException, SQLException, InterruptedException {
    String sqlString =
        new String(Files.readAllBytes(Paths.get(sqlFile.getAbsolutePath())), charset);
    return dump(sqlString);
  }

  public String dump(File sqlFile) throws IOException, SQLException, InterruptedException {
    return dump(sqlFile, StandardCharsets.UTF_8);
  }

  public String dumpFromLocalDb(String dbName)
      throws IOException, InterruptedException, SQLException {
    return fetchSchemaViaMysqldump(dbName);
  }

  public String dumpFromRemoteDb(String dbName, MySqlConnectionInfo mysqlConnectionInfo)
      throws IOException, InterruptedException, SQLException {
    String schema = fetchSchemaViaMysqldump(dbName, mysqlConnectionInfo);
    return dump(schema);
  }

  private String fetchSchemaViaMysqldump(String dbName, MySqlConnectionInfo mysqlConnectionInfo)
      throws IOException, InterruptedException {
    String schema;
    List<String> mysqldumpCommand = new ArrayList<>(Arrays.asList(
        mysqldumpPath,
        "--no-data=true",
        dbName));

    String mysqlUser = mysqlConnectionInfo.getUser();
    if (!mysqlUser.isEmpty()) {
      mysqldumpCommand.add(new StringBuilder().append("-u").append(mysqlUser).toString());
    }

    String mysqlHost = mysqlConnectionInfo.getHost();
    if (!mysqlHost.isEmpty()) {
      mysqldumpCommand.add(new StringBuilder().append("-h").append(mysqlHost).toString());
    }

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

  private String fetchSchemaViaMysqldump(String dbName)
      throws IOException, InterruptedException, SQLException {
    return fetchSchemaViaMysqldump(dbName, localMySqlConnectionInfo);
  }
}
