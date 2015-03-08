package net.moznion.mysql.diff;

import lombok.Getter;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Getter
class MySqlConnectionUri {
  private String host;
  private int port;
  private List<String> queries;

  private static final int DEFAULT_PORT = 3306;
  private static final Pattern URL_PATTERN = Pattern.compile("^([^/]*:)[^:/]+:");

  public MySqlConnectionUri(String url) throws URISyntaxException {
    Matcher urlMatcher = URL_PATTERN.matcher(url);
    if (!urlMatcher.find()) {
      throw new URISyntaxException(url, "It doesn't contain connection protocol schema of jdbc");
    }

    String cleanUrl = url.substring(urlMatcher.group(1).length());
    URI uri = new URI(cleanUrl);

    host = uri.getHost();

    port = uri.getPort();
    if (port < 0) {
      port = DEFAULT_PORT;
    }

    queries = parseQueryString(uri.getQuery());
  }

  private List<String> parseQueryString(String queryString) {
    ArrayList<String> queries = new ArrayList<>();

    if (queryString != null) {
      Arrays.asList(queryString.split("&")).forEach(property -> {
        queries.add(property);
      });
    }

    return queries;
  }
}
