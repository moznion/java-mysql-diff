package net.moznion.mysql.diff;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.moznion.mysql.diff.model.Table;

public class App {
  public static void main(String[] args) throws IOException, SQLException, InterruptedException {
    // TODO use log4j
    if (args[0].equals("-version")) {
      System.out.println(App.class.getPackage().getImplementationVersion());
      return;
    }

    if (args.length != 2) {
      // TODO show usage
      throw new IllegalArgumentException("Too few command line arguments");
    }

    List<List<Table>> parsed = new ArrayList<>();
    for (String arg : args) {
      String schema;
      SchemaDumper schemaDumper = new SchemaDumper(); // TODO should be more configurable

      File file = new File(arg);
      if (file.exists()) {
        // for file
        schema = schemaDumper.dump(file);
      } else if (arg.contains(" ")) {
        // for remote server
        schema =
            schemaDumper.dumpFromRemoteDB(parseArgForDBName(arg), parseArgForConnectionInfo(arg));
      } else {
        // for local server
        schema = schemaDumper.dumpFromLocalDB(arg);
      }

      parsed.add(SchemaParser.parse(schema));
    }

    String diff = DiffExtractor.extractDiff(parsed.get(0), parsed.get(1));
    System.out.println(diff);
  }

  private static final Pattern patternForHost = Pattern.compile("^-h(.+)$");
  private static final Pattern patternForUser = Pattern.compile("^-u(.+)$");
  private static final Pattern patternForPass = Pattern.compile("^-p(.+)$");

  private static String parseArgForDBName(String info) {
    List<String> flags = Arrays.asList(info.split(" "));

    String dbName = null;
    for (String flag : flags) {
      Matcher matcherForHost = patternForHost.matcher(flag);
      Matcher matcherForUser = patternForUser.matcher(flag);
      Matcher matcherForPass = patternForPass.matcher(flag);

      if (!matcherForHost.matches() &&
          !matcherForUser.matches() &&
          !matcherForPass.matches()) {
        dbName = flag;
      }
    }

    if (dbName == null) {
      throw new IllegalArgumentException("Argument for remote connection must contain DB name");
    }

    return dbName;
  }

  private static MySQLConnectionInfo parseArgForConnectionInfo(String info) {
    List<String> flags = Arrays.asList(info.split(" "));

    MySQLConnectionInfo.Builder builder = MySQLConnectionInfo.builder();

    for (String flag : flags) {
      Matcher matcherForHost = patternForHost.matcher(flag);
      Matcher matcherForUser = patternForUser.matcher(flag);
      Matcher matcherForPass = patternForPass.matcher(flag);

      if (matcherForHost.find()) {
        // for host name
        builder.host(matcherForHost.group(1));
      } else if (matcherForUser.find()) {
        // for user name
        builder.host(matcherForUser.group(1));
      } else if (matcherForPass.find()) {
        // for password
        builder.host(matcherForPass.group(1));
      }
    }

    return builder.build();
  }
}
