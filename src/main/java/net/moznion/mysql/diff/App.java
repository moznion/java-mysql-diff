package net.moznion.mysql.diff;

import lombok.Getter;

import net.moznion.mysql.diff.model.Table;

import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.kohsuke.args4j.spi.StringArrayOptionHandler;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Class for CLI application.
 * 
 * @author moznion
 *
 */
public class App {
  @Option(name = "-v", aliases = "--version", usage = "print version")
  private boolean showVersion;

  @Option(name = "-h", aliases = "--help", usage = "print usage message and exit")
  private boolean showUsage;

  @Argument(index = 0, metaVar = "arguments...", handler = StringArrayOptionHandler.class)
  private String[] arguments;

  /**
   * Class to parse options for remote DB connection information.
   */
  @Getter
  private class RemoteDbArg {
    @Option(name = "-h", aliases = "--host", metaVar = "host", usage = "specify host")
    private String host;

    @Option(name = "-u", aliases = "--user", metaVar = "user", usage = "specify user")
    private String user;

    @Option(name = "-p", aliases = "--password", metaVar = "pass", usage = "specify password")
    private String pass;

    @Argument(index = 0, metaVar = "dbName")
    private String dbName;
  }

  /**
   * Entry point of CLI application.
   * 
   * <pre>
   * [Usage]
   *     java -jar [old_database] [new_database]
   * [Examples]
   * Take diff between createtable1.sql and createtable2.sql (both of SQL files on your machine)
   *     java -jar createtable1.sql createtable2.sql
   * Take diff between dbname1 and dbname2 (both of databases on the local MySQL)
   *     java -jar dbname1 dbname2
   * Take diff between dbname1 and dbname2 (both of databases on remote MySQL)
   *     java -jar '-uroot -hlocalhost dbname1' '-uroot -hlocalhost dbname2'
   * [Options]
   *     -h, --help:    Show usage
   *     -v, --version: Show version
   * </pre>
   * 
   * @param args Options, or target of database arguments.
   * @throws IOException Throw if mysqldump command is failed.
   * @throws SQLException Throw if invalid SQL is given.
   * @throws InterruptedException Throw if mysqldump command is failed.
   */
  public static void main(String[] args) throws IOException, SQLException, InterruptedException {
    App app = new App();

    CmdLineParser parser = new CmdLineParser(app);
    try {
      parser.parseArgument(args);
    } catch (CmdLineException e) {
      throw new IllegalArgumentException("Invalid arguments are detected: " + Arrays.asList(args));
    }

    if (app.showVersion) {
      System.out.println(Optional.ofNullable(App.class.getPackage().getImplementationVersion())
          .orElse("Missing Version")); // XXX "Missing Version" maybe used by testing only...
      return;
    }

    if (app.showUsage) {
      System.out.println(getUsageMessage());
      return;
    }

    List<String> coreArgs = Arrays.asList(Optional.ofNullable(args).orElse(new String[0]));
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
        RemoteDbArg remoteDbArg = new App().new RemoteDbArg();
        CmdLineParser remoteDbArgParser = new CmdLineParser(remoteDbArg);
        try {
          remoteDbArgParser.parseArgument(arg.substring(1, arg.length() - 1).split(" "));
        } catch (CmdLineException e) {
          throw new IllegalArgumentException("Invalid remote DB argument is detected: " + arg);
        }

        if (remoteDbArg.dbName == null || remoteDbArg.dbName.isEmpty()) {
          throw new IllegalArgumentException("Invalid remote DB argument is detected: " + arg);
        }

        MySqlConnectionInfo.Builder mysqlConnectionInfoBuilder = MySqlConnectionInfo.builder();

        if (remoteDbArg.host != null) {
          mysqlConnectionInfoBuilder.host(remoteDbArg.host);
        }

        if (remoteDbArg.user != null) {
          mysqlConnectionInfoBuilder.user(remoteDbArg.user);
        }

        if (remoteDbArg.pass != null) {
          mysqlConnectionInfoBuilder.pass(remoteDbArg.pass);
        }

        schema =
            schemaDumper.dumpFromRemoteDb(remoteDbArg.dbName, mysqlConnectionInfoBuilder.build());
      } else {
        // for local server
        schema = schemaDumper.dumpFromLocalDb(arg);
      }

      parsed.add(SchemaParser.parse(schema));
    }

    String diff = DiffExtractor.extractDiff(parsed.get(0), parsed.get(1));
    System.out.println(diff);
  }

  private static String getUsageMessage() {
    return "[Usage]\n"
        + "    java -jar [old_database] [new_database]\n"
        + "[Examples]\n"
        + "* Take diff between createtable1.sql and createtable2.sql "
        + "(both of SQL files on your machine)\n"
        + "    java -jar createtable1.sql createtable2.sql\n"
        + "* Take diff between dbname1 and dbname2 "
        + "(both of databases on the local MySQL)\n"
        + "    java -jar dbname1 dbname2\n"
        + "* Take diff between dbname1 and dbname2 "
        + "(both of databases on remote MySQL)\n"
        + "    java -jar '-u root -h localhost dbname1' '-u root -h localhost dbname2'"
        + "\n"
        + "[Options]\n"
        + "    -h, --help:    Show usage\n"
        + "    -v, --version: Show version";
  }
}
