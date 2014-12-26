package net.moznion.mysql.diff;

import net.moznion.mysql.diff.model.Column;
import net.moznion.mysql.diff.model.OrdinaryKey;
import net.moznion.mysql.diff.model.Table;
import net.moznion.mysql.diff.model.UniqueKey;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SchemaParser {
  private final static Pattern TABLA_BLOCK_PATTERN = Pattern.compile(
      "CREATE TABLE .*? ENGINE[^;]*", Pattern.MULTILINE | Pattern.DOTALL);

  private final static Pattern TABLE_NAME_PATTERN = Pattern.compile("`(.*?)`");

  private final static Pattern PRIMARY_KEY_PATTERN = Pattern.compile(
      "^\\s*PRIMARY KEY\\s+\\((.*)\\)");

  private final static Pattern UNIQUE_KEY_PATTERN = Pattern.compile(
      "^\\s*UNIQUE KEY\\s+`(.*)`\\s+\\((.*)\\)");

  private final static Pattern ORDINARY_KEY_PATTERN = Pattern.compile(
      "^\\s*KEY\\s+`(.*)`\\s+\\((.*)\\)");

  private final static Pattern COLUMN_PATTERN = Pattern.compile(
      "^\\s*`(.*?)`\\s+(.+?)[\n,]?$");

  public static List<Table> parse(String schema) {
    Matcher blockMatcher = TABLA_BLOCK_PATTERN.matcher(schema);

    List<Table> tables = new ArrayList<>();

    while (blockMatcher.find()) {
      String content = blockMatcher.group();

      Matcher tableNameMatcher = TABLE_NAME_PATTERN.matcher(content);
      if (!tableNameMatcher.find()) {
        continue;
      }
      String tableName = tableNameMatcher.group(1);

      List<String> primaryKeys = new ArrayList<>();
      List<UniqueKey> uniqueKeys = new ArrayList<>();
      List<OrdinaryKey> keys = new ArrayList<>();
      List<Column> columns = new ArrayList<>();

      for (String line : content.split("\r?\n")) {
        if (line.matches("^CREATE") || line.matches("^\\)")) {
          continue;
        }

        Matcher primaryKeyMatcher = PRIMARY_KEY_PATTERN.matcher(line);
        if (primaryKeyMatcher.find()) {
          primaryKeys.add(primaryKeyMatcher.group(1));
          continue;
        }

        Matcher uniqueKeyMatcher = UNIQUE_KEY_PATTERN.matcher(line);
        if (uniqueKeyMatcher.find()) {
          uniqueKeys.add(new UniqueKey(uniqueKeyMatcher.group(1), uniqueKeyMatcher.group(2)));
          continue;
        }

        Matcher ordinaryKeyMatcher = ORDINARY_KEY_PATTERN.matcher(line);
        if (ordinaryKeyMatcher.find()) {
          keys.add(new OrdinaryKey(ordinaryKeyMatcher.group(1), ordinaryKeyMatcher.group(2)));
          continue;
        }

        Matcher columnMatcher = COLUMN_PATTERN.matcher(line);
        if (columnMatcher.find()) {
          columns.add(new Column(columnMatcher.group(1), columnMatcher.group(2)));
          continue;
        }

        // Match nothing if reach here
      }

      tables.add(Table.builder()
          .tableName(tableName)
          .primaryKeys(primaryKeys)
          .keys(keys)
          .columns(columns)
          .content(content)
          .build());
    }

    return tables;
  }
}
