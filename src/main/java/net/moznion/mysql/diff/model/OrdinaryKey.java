package net.moznion.mysql.diff.model;

import lombok.Getter;

@Getter
public class OrdinaryKey {
  private final String name;
  private final String column;

  public OrdinaryKey(String name, String column) {
    this.name = name;
    this.column = column;
  }
}
