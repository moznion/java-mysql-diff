package net.moznion.mysql.diff;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
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
 * Dumper for SQL table definition.
 * 
 * @author moznion
 *
 */
public class SchemaDumper {
  private static final String LINE_SEPARATOR = System.lineSeparator();

  private final MySqlConnectionInfo localMySqlConnectionInfo;
  private final String mysqldumpPath;

  /**
   * Instantiate SchemaDumper.
   * 
   * @param localMySqlConnectionInfo Connection information of MySQL which is on your local
   *        environment.
   * @param mysqldumpPath Path for mysqldump command.
   */
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

  /**
   * Instantiate SchemaDumper.
   * 
   * <p>
   * Path of mysqldump will be used default as "mysqldump".
   * </p>
   * 
   * @param localMySqlConnectionInfo Connection information of MySQL which is on your local
   *        environment.
   */
  public SchemaDumper(MySqlConnectionInfo localMySqlConnectionInfo) {
    this(localMySqlConnectionInfo, "mysqldump");
  }

  /**
   * Instantiate SchemaDumper.
   * 
   * <p>
   * Connection information of local MySQL will be used default as "-h localhost -u root".
   * </p>
   * 
   * @param mysqldumpPath Path for mysqldump command.
   */
  public SchemaDumper(String mysqldumpPath) {
    this(MySqlConnectionInfo.builder().build(), mysqldumpPath);
  }

  /**
   * Instantiate SchemaDumper.
   * 
   * <p>
   * Path of mysqldump will be used default as "mysqldump".<br>
   * Connection information of local MySQL will be used default as "-h localhost -u root".
   * </p>
   */
  public SchemaDumper() {
    this(MySqlConnectionInfo.builder().build());
  }

  /**
   * Dump schema from SQL string.
   * 
   * @param sql SQL string which is a target to dump.
   * @return Result of dumping.
   * @throws SQLException Throw if invalid SQL is given.
   * @throws IOException Throw if mysqldump command is failed.
   * @throws InterruptedException Throw if mysqldump command is failed.
   */
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

  /**
   * Dump schema from SQL file.
   * 
   * @param sqlFile SQL file.
   * @param charset Character set of SQL file.
   * @return Result of dumping.
   * @throws SQLException Throw if invalid SQL is given.
   * @throws IOException Throw if mysqldump command is failed.
   * @throws InterruptedException Throw if mysqldump command is failed.
   */
  public String dump(File sqlFile, Charset charset)
      throws IOException, SQLException, InterruptedException {
    String sqlString =
        new String(Files.readAllBytes(Paths.get(sqlFile.getAbsolutePath())), charset);
    return dump(sqlString);
  }

  /**
   * Dump schema from SQL file which is written by UTF-8.
   * 
   * @param sqlFile SQL file (written by UTF-8).
   * @return Result of dumping.
   * @throws SQLException Throw if invalid SQL is given.
   * @throws IOException Throw if mysqldump command is failed.
   * @throws InterruptedException Throw if mysqldump command is failed.
   */
  public String dump(File sqlFile) throws IOException, SQLException, InterruptedException {
    return dump(sqlFile, StandardCharsets.UTF_8);
  }

  /**
   * Dump schema from DB name which is in local MySQL.
   * 
   * @param dbName DB name which is in local MySQL.
   * @return Result of dumping.
   * @throws SQLException Throw if invalid SQL is given.
   * @throws IOException Throw if mysqldump command is failed.
   * @throws InterruptedException Throw if mysqldump command is failed.
   */
  public String dumpFromLocalDb(String dbName)
      throws IOException, InterruptedException, SQLException {
    return fetchSchemaViaMysqldump(dbName);
  }

  /**
   * Dump schema from DB name which is in remote MySQL.
   * 
   * @param dbName DB name which is in remote MySQL.
   * @param mysqlConnectionInfo Connection information of remote MySQL.
   * @return Result of dumping.
   * @throws SQLException Throw if invalid SQL is given.
   * @throws IOException Throw if mysqldump command is failed.
   * @throws InterruptedException Throw if mysqldump command is failed.
   */
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
