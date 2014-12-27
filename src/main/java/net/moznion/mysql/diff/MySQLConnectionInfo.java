package net.moznion.mysql.diff;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Getter
public class MySQLConnectionInfo {
  private final String host;
  private final String user;
  private final String pass;
  private final String jdbcURL;

  @Setter
  @Accessors(fluent = true)
  public static class Builder {
    private String host = "localhost";
    private String user = "root";
    private String pass = "";

    public Builder() {}

    public MySQLConnectionInfo build() {
      return new MySQLConnectionInfo(this);
    }
  }

  public static Builder builder() {
    return new Builder();
  }

  private MySQLConnectionInfo(Builder builder) {
    host = builder.host;
    user = builder.user;
    pass = builder.pass;
    jdbcURL = new StringBuilder()
        .append("jdbc:mysql://")
        .append(builder.host)
        .toString();
  }
}
