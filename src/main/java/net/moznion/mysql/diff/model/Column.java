package net.moznion.mysql.diff.model;

import lombok.Getter;

@Getter
public class Column {
  private final String name;
  private final String definition;

  public Column(String name, String definition) {
    this.name = name;
    this.definition = definition;
  }
}
