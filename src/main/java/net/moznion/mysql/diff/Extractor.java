package net.moznion.mysql.diff;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import net.moznion.mysql.diff.model.Column;
import net.moznion.mysql.diff.model.OrdinaryKey;
import net.moznion.mysql.diff.model.Table;
import net.moznion.mysql.diff.model.UniqueKey;

public class Extractor {
  public static String extractDiff(List<Table> oldTables, List<Table> newTables) {
    StringBuilder diffStringBuilder = new StringBuilder();

    List<String> newTableNames = newTables.stream()
        .map(table -> table.getTableName())
        .sorted()
        .collect(Collectors.toList());

    Map<String, Table> oldTableMap = oldTables.stream()
        .collect(Collectors.toMap(Table::getTableName, t -> t));
    Map<String, Table> newTableMap = newTables.stream()
        .collect(Collectors.toMap(Table::getTableName, t -> t));

    for (String tableName : newTableNames) {
      Table newTable = newTableMap.get(tableName);
      if (oldTableMap.containsKey(tableName)) {
        Table oldTable = oldTableMap.get(tableName);
        diffStringBuilder.append(extractTableDiff(tableName, oldTable, newTable));
      } else {
        diffStringBuilder.append(newTable.getContent()).append(";\n\n");
      }
    }

    return diffStringBuilder.toString();
  }

  private static String extractTableDiff(String tableName, Table oldTable, Table newTable) {
    List<String> changes = extractColumnDiff(oldTable, newTable);
    changes.addAll(extractKeyDiff(oldTable, newTable));

    if (changes.isEmpty()) {
      return "";
    }

    return new StringBuilder()
        .append("ALTER TABLE `")
        .append(tableName)
        .append("` ")
        .append(String.join(", ", changes))
        .append(";\n\n")
        .toString();
  }

  private static List<String> extractColumnDiff(Table oldTable, Table newTable) {
    List<Column> oldColumns = oldTable.getColumns();
    List<Column> newColumns = newTable.getColumns();

    Map<String, Column> oldColumnMap = oldColumns.stream()
        .collect(Collectors.toMap(Column::getName, c -> c));
    Map<String, Column> newColumnMap = newColumns.stream()
        .collect(Collectors.toMap(Column::getName, c -> c));

    Map<String, Column> allColumnMap = new HashMap<>();
    allColumnMap.putAll(oldColumnMap);
    allColumnMap.putAll(newColumnMap);

    List<String> changes = new ArrayList<>();
    for (Entry<String, Column> column : allColumnMap.entrySet()) {
      String columnName = column.getKey();

      if (!oldColumnMap.containsKey(columnName)) {
        changes.add(new StringBuilder()
            .append("ADD `")
            .append(columnName)
            .append("` ")
            .append(newColumnMap.get(columnName).getDefinition())
            .toString());
        continue;
      }

      if (!newColumnMap.containsKey(columnName)) {
        changes.add(new StringBuilder()
            .append("DROP `")
            .append(columnName)
            .append("`")
            .toString());
        continue;
      }

      String oldDefinition = oldColumnMap.get(columnName).getDefinition();
      String newDefinition = newColumnMap.get(columnName).getDefinition();
      if (!oldDefinition.equals(newDefinition)) {
        changes.add(new StringBuilder()
            .append("MODIFY `")
            .append(columnName)
            .append("` ")
            .append(newDefinition)
            .toString());
        continue;
      }
    }

    return changes;
  }

  private static List<String> extractKeyDiff(Table oldTable, Table newTable) {
    List<String> changes = new ArrayList<>();

    // For ordinary key
    changes.addAll(extractOrdinaryKeyDiff(oldTable, newTable));

    // For unique key
    changes.addAll(extractUniqueKeyDiff(oldTable, newTable));

    return changes;
  }

  private static List<String> extractOrdinaryKeyDiff(Table oldTable, Table newTable) {
    List<String> changes = new ArrayList<>();

    List<OrdinaryKey> oldKeys = oldTable.getKeys();
    List<OrdinaryKey> newKeys = newTable.getKeys();

    Map<String, Boolean> oldKeysExistanceMap = oldKeys.stream()
        .collect(Collectors.toMap(OrdinaryKey::getColumn, k -> true));
    Map<String, Boolean> newKeysExistanceMap = newKeys.stream()
        .collect(Collectors.toMap(OrdinaryKey::getColumn, k -> true));

    // add key
    for (OrdinaryKey key : newKeys) {
      String column = key.getColumn();
      if (oldKeysExistanceMap.containsKey(column)) {
        continue;
      }

      String name = String.join("_",
          Arrays.stream(column.split(","))
              .map(col -> col.replaceAll("[`()]", ""))
              .collect(Collectors.toList()));

      changes.add(
          new StringBuilder()
              .append("ADD INDEX `")
              .append(name)
              .append("` (")
              .append(column)
              .append(")")
              .toString());
    }

    // drop key
    for (OrdinaryKey key : oldKeys) {
      String column = key.getColumn();
      if (newKeysExistanceMap.containsKey(column)) {
        continue;
      }

      changes.add(
          new StringBuilder()
              .append("DROP INDEX `")
              .append(key.getName())
              .append("`")
              .toString());
    }

    return changes;
  }

  private static List<String> extractUniqueKeyDiff(Table oldTable, Table newTable) {
    List<String> changes = new ArrayList<>();

    List<UniqueKey> oldKeys = oldTable.getUniqueKeys();
    List<UniqueKey> newKeys = newTable.getUniqueKeys();

    Map<String, Boolean> oldKeysExistanceMap = oldKeys.stream()
        .collect(Collectors.toMap(OrdinaryKey::getColumn, k -> true));
    Map<String, Boolean> newKeysExistanceMap = newKeys.stream()
        .collect(Collectors.toMap(OrdinaryKey::getColumn, k -> true));

    // add key
    for (UniqueKey key : newKeys) {
      String column = key.getColumn();
      if (oldKeysExistanceMap.containsKey(column)) {
        continue;
      }

      String name = String.join("_",
          Arrays.asList(column.split(",")).stream()
              .map(col -> col.replaceAll("[`()]", ""))
              .collect(Collectors.toList()));

      changes.add(
          new StringBuilder()
              .append("ADD UNIQUE INDEX `")
              .append(name)
              .append("` (")
              .append(column)
              .append(")")
              .toString());
    }

    // drop key
    for (OrdinaryKey key : oldKeys) {
      String column = key.getColumn();
      if (newKeysExistanceMap.containsKey(column)) {
        continue;
      }

      changes.add(
          new StringBuilder()
              .append("DROP INDEX `")
              .append(key.getName())
              .append("`")
              .toString());
    }

    return changes;
  }
}
