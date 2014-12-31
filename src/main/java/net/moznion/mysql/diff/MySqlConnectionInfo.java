package net.moznion.mysql.diff;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

/**
 * Representation of connection information for MySQL.
 * 
 * @author moznion
 *
 */
@Getter
public class MySqlConnectionInfo {
  private final String host;
  private final String user;
  private final String pass;
  private final String jdbcUrl;

  /**
   * Builder class of MySqlConnectionInfo.
   * 
   * <p>
   * This class provides following setters;
   * </p>
   * <ul>
   * <li>host(String hostName) // default value: "localhost"</li>
   * <li>user(String userName) // default value: "root"</li>
   * <li>host(String password) // default value: ""</li>
   * </ul>
   */
  @Setter
  @Accessors(fluent = true)
  public static class Builder {
    private String host = "localhost";
    private String user = "root";
    private String pass = "";

    /**
     * Builds MySqlConnectionInfo.
     * 
     * @return New MySqlConnectionInfo instance.
     */
    public MySqlConnectionInfo build() {
      return new MySqlConnectionInfo(this);
    }
  }

  /**
   * Dispenses a new builder of MySqlConnectionInfo.
   * 
   * @return Builder of MySqlConnectionInfo.
   */
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
