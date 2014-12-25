package net.moznion.mysql.diff.model;

import java.util.List;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

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
    tableName = builder.tableName;
    primaryKeys = builder.primaryKeys;
    uniqueKeys = builder.uniqueKeys;
    keys = builder.keys;
    columns = builder.columns;
    content = builder.content;
  }
}
