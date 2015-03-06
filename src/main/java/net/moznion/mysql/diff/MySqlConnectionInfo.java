package net.moznion.mysql.diff;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
   * <li>port(int portNumber) // default value: 3306</li>
   * <li>user(String userName) // default value: "root"</li>
   * <li>host(String password) // default value: ""</li>
   * </ul>
   */
  @Accessors(fluent = true)
  public static class Builder {
    @Setter
    private String host = "localhost";
    @Setter
    private int port = 3306;
    @Setter
    private String user = "root";
    @Setter
    private String pass = "";

    private List<String> properties = new ArrayList<>(Arrays.asList("allowMultiQueries=true"));

    /**
     * Add property.
     * 
     * @param property
     */
    public void addProperty(String property) {
      properties.add(property);
    }

    /**
     * Set host name, port number and options by URL of a remote MySQL.
     * 
     * @param url URL of a remote MySQL (e.g.
     *        jdbc:mysql://localhost:8888/something_table?cacheServerConfiguration=true)
     */
    public Builder url(String url) {
      url = url.replaceFirst("^.+://", ""); // remove connection prefix

      List<String> splittedByQuestion = Arrays.asList(url.split("\\?"));
      String origin = splittedByQuestion.get(0);

      List<String> splittedOrigin = Arrays.asList(origin.split("/")[0].split(":"));
      host = splittedOrigin.get(0);
      if (splittedOrigin.size() >= 2) {
        // exists port number
        port = Integer.parseInt(splittedOrigin.get(1), 10);
      }

      if (splittedByQuestion.size() >= 2) {
        // for property
        String serialProperty = splittedByQuestion.get(1);
        Arrays.asList(serialProperty.split("&")).forEach(property -> {
          addProperty(property);
        });
      }

      return this;
    }

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
        .append(":")
        .append(builder.port)
        .append("?")
        .append(String.join("&", builder.properties))
        .toString();
  }
}
