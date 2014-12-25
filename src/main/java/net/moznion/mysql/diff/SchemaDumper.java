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

import lombok.experimental.Accessors;

/**
 * Parser for SQL table definition.
 * 
 * @author moznion
 *
 */
public class SchemaDumper {
  private static final String LINE_SEPARATOR = System.lineSeparator();

  private final String mysqlURL;
  private final String mysqlUser;
  private final String mysqlPass;

  @Accessors(fluent = true)
  public static class Builder {
    private String mysqlHost = "localhost";
    private String mysqlUser = "root";
    private String mysqqlPass = "";

    public Builder() {}

    public SchemaDumper build() {
      return new SchemaDumper(this);
    }
  }

  public static Builder builder() {
    return new Builder();
  }

  private SchemaDumper(Builder builder) {
    mysqlUser = builder.mysqlUser;
    mysqlPass = builder.mysqqlPass;
    mysqlURL = new StringBuilder()
        .append("jdbc:mysql://")
        .append(builder.mysqlHost)
        .toString();
  }

  public String dump(String sql) throws SQLException, IOException, InterruptedException {
    String tempDBName = new StringBuilder()
        .append("tmp_")
        .append(UUID.randomUUID().toString().replaceAll("-", ""))
        .toString();

    try (Connection connection = DriverManager.getConnection(mysqlURL, mysqlUser, mysqlPass)) {
      try (Statement stmt = connection.createStatement()) {
        stmt.executeUpdate("CREATE DATABASE " + tempDBName);
      }

      List<String> mysqldumpCommand = Arrays.asList(
          "mysqldump",
          new StringBuilder().append("-u").append(mysqlUser).toString(),
          "--no-data=true",
          tempDBName);

      ProcessBuilder processBuilder = new ProcessBuilder(mysqldumpCommand);

      String schema;
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

      int ret = process.waitFor();

      try (Statement stmt = connection.createStatement()) {
        stmt.executeUpdate("DROP DATABASE " + tempDBName);
      }

      if (ret != 0) {
        throw new RuntimeException(
            new StringBuilder()
                .append("Failed to execute `mysqldump` command (command: ")
                .append(String.join(" ", mysqldumpCommand))
                .append(")")
                .toString());
      }

      return schema;
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
}
