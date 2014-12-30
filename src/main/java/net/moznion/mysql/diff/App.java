package net.moznion.mysql.diff;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.kohsuke.args4j.spi.StringArrayOptionHandler;

import net.moznion.mysql.diff.model.Table;

public class App {
  @Option(name = "-v", aliases = "--version", usage = "print version")
  private boolean showVersion;

  @Option(name = "-h", aliases = "--help", usage = "print usage message and exit")
  private boolean showUsage;

  @Argument(index = 0, metaVar = "arguments...", handler = StringArrayOptionHandler.class)
  private String[] arguments;

  public static void main(String[] args) throws IOException, SQLException, InterruptedException {
    App app = new App();

    CmdLineParser parser = new CmdLineParser(app);
    try {
      parser.parseArgument(args);
    } catch (CmdLineException e) {
      e.printStackTrace();
      return;
    }

    if (app.showVersion) {
      System.out.println(App.class.getPackage().getImplementationVersion());
      return;
    }

    if (app.showUsage) {
      System.out.println(getUsageMessage());
      return;
    }

    List<String> coreArgs = Arrays.asList(Optional.ofNullable(app.arguments).orElse(new String[0]));
    int numOfArgs = coreArgs.size();
    if (numOfArgs != 2) {
      if (numOfArgs < 2) {
        System.err.println("[ERROR] Too few command line arguments");
      } else {
        System.err.println("[ERROR] Too many command line arguments");
      }
      System.err.println();
      System.err.println(getUsageMessage());
      System.exit(1);
    }

    List<List<Table>> parsed = new ArrayList<>();
    for (String arg : coreArgs) {
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

  private static String getUsageMessage() {
    return "[Usage]\n" +
        "    java -jar <old_database> <new_database>\n" +
        "[Examples]\n" +
        "* Take diff between createtable1.sql and createtable2.sql " +
        "(both of them are SQL file which are on your machine)\n" +
        "    java -jar createtable1.sql createtable2.sql\n" +
        "* Take diff between dbname1 and dbname2 " +
        "(both of databases on the local MySQL)\n" +
        "    java -jar dbname1 dbname2\n" +
        "* Take diff between dbname1 and dbname2 " +
        "(both of databases on remote MySQL)\n" +
        "    java -jar '-uroot -hlocalhost dbname1' '-uroot -hlocalhost dbname2'" +
        "\n" +
        "[Options]\n" +
        "    -h, --help:    Show usage\n" +
        "    -v, --version: Show version";
  }
}
