package net.moznion.mysql.diff.model;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Getter
public class Table {
  private final String tableName;
  private final List<String> primaryKeys;
  private final List<UniqueKey> uniqueKeys;
  private final List<OrdinaryKey> keys;
  private final List<Column> columns;
  private final String content;

  @Setter
  @Accessors(fluent = true)
  public static class Builder {
    private String tableName;
    private List<String> primaryKeys;
    private List<UniqueKey> uniqueKeys;
    private List<OrdinaryKey> keys;
    private List<Column> columns;
    private String content;

    public Builder() {}

    public Table build() {
      return new Table(this);
    }
  }

  public static Builder builder() {
    return new Builder();
  }

  private Table(Builder builder) {
    tableName = Optional.ofNullable(builder.tableName)
        .orElseThrow(() -> new IllegalArgumentException("Missing table name"));
    primaryKeys = Optional.ofNullable(builder.primaryKeys).orElse(new ArrayList<>());
    uniqueKeys = Optional.ofNullable(builder.uniqueKeys).orElse(new ArrayList<>());
    keys = Optional.ofNullable(builder.keys).orElse(new ArrayList<>());
    columns = Optional.ofNullable(builder.columns).orElse(new ArrayList<>());
    content = Optional.ofNullable(builder.content).orElse("");
  }
}
