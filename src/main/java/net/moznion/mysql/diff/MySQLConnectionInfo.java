package net.moznion.mysql.diff;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Getter
public class MySqlConnectionInfo {
  private final String host;
  private final String user;
  private final String pass;
  private final String jdbcUrl;

  @Setter
  @Accessors(fluent = true)
  public static class Builder {
    private String host = "localhost";
    private String user = "root";
    private String pass = "";

    public Builder() {}

    public MySqlConnectionInfo build() {
      return new MySqlConnectionInfo(this);
    }
  }

  public static Builder builder() {
    return new Builder();
  }

  private MySqlConnectionInfo(Builder builder) {
    host = builder.host;
    user = builder.user;
    pass = builder.pass;
    jdbcUrl = new StringBuilder()
        .append("jdbc:mysql://")
        .append(builder.host)
        .append("?allowMultiQueries=true")
        .toString();
  }
}
