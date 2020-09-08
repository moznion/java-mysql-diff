package net.moznion.mysql.diff;

import com.mysql.cj.jdbc.exceptions.CommunicationsException;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.moznion.mysql.diff.model.Table;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

@RunWith(Enclosed.class)
public class DiffExtractorTest {
  private static final SchemaDumper SCHEMA_DUMPER = new SchemaDumper();

  public static class ForTableDiff {
    private String getAlterTableDiffRightly(String oldSql, String newSql) throws SQLException,
        IOException, InterruptedException {
      String oldSchema = SCHEMA_DUMPER.dump(oldSql);
      String newSchema = SCHEMA_DUMPER.dump(newSql);

      List<Table> oldTables = SchemaParser.parse(oldSchema);
      List<Table> newTables = SchemaParser.parse(newSchema);

      String diff = DiffExtractor.extractDiff(oldTables, newTables);
      diff = diff.replaceAll("\r?\n", "");

      assertTrue(diff.startsWith("ALTER TABLE `sample` "));
      assertTrue(diff.endsWith(";"));
      diff = diff.replaceFirst("^ALTER TABLE `sample` ", "");
      diff = diff.replaceFirst(";$", "");

      return diff;
    }

    @Test
    public void shouldDetectColumnDiffRightly() throws SQLException, IOException,
        InterruptedException {
      String oldSql = "CREATE TABLE `sample` ("
          + "  `id` int(10) NOT NULL AUTO_INCREMENT,"
          + "  `title` varchar(64) NOT NULL,"
          + "  `created_on` int(10) unsigned NOT NULL,"
          + "  PRIMARY KEY (`id`)"
          + ") ENGINE=InnoDB DEFAULT CHARSET=utf8;";
      String newSql = "CREATE TABLE `sample` ("
          + "  `id` int(10) NOT NULL AUTO_INCREMENT,"
          + "  `title` varchar(64) DEFAULT NULL,"
          + "  `updated_on` int(10) unsigned NOT NULL,"
          + "  PRIMARY KEY (`id`)"
          + ") ENGINE=InnoDB DEFAULT CHARSET=utf8;";

      try {
        String diff = getAlterTableDiffRightly(oldSql, newSql);
        Set<String> modifiers = Arrays.stream(diff.split(", ")).collect(Collectors.toSet());
        Set<String> expected = Stream.of(
            "DROP `created_on`",
            "MODIFY `title` varchar(64) DEFAULT NULL",
            "ADD `updated_on` int(10) unsigned NOT NULL"
            ).collect(Collectors.toSet());
        assertEquals(modifiers, expected);
      } catch (CommunicationsException e) {
        assumeTrue("MySQL maybe not launched", false);
      } catch (Exception e) {
        assertTrue(false);
      }
    }

    @Test
    public void shouldDetectKeyDiffRightly()
        throws SQLException, IOException, InterruptedException {
      String oldSql = "CREATE TABLE `sample` ("
          + "  `id` int(10) NOT NULL AUTO_INCREMENT,"
          + "  `name` varchar(16) NOT NULL,"
          + "  `email` varchar(16) NOT NULL,"
          + "  `title` varchar(64) NOT NULL,"
          + "  `created_on` int (10) unsigned NOT NULL,"
          + "  `updated_on` int (10) unsigned NOT NULL,"
          + "  PRIMARY KEY (`id`),"
          + "  UNIQUE KEY `name` (`name`),"
          + "  KEY `updated_on` (`updated_on`)"
          + ") ENGINE=InnoDB DEFAULT CHARSET=utf8;";
      String newSql = "CREATE TABLE `sample` ("
          + "  `id` int(10) NOT NULL AUTO_INCREMENT,"
          + "  `name` varchar(16) NOT NULL,"
          + "  `email` varchar(16) NOT NULL,"
          + "  `title` varchar(64) NOT NULL,"
          + "  `created_on` int (10) unsigned NOT NULL,"
          + "  `updated_on` int (10) unsigned NOT NULL,"
          + "  PRIMARY KEY (`id`),"
          + "  UNIQUE KEY `identifier` (`email`,`name`),"
          + "  KEY `timestamp` (`created_on`,`updated_on`)"
          + ") ENGINE=InnoDB DEFAULT CHARSET=utf8;";

      try {
        String diff = getAlterTableDiffRightly(oldSql, newSql);
        Set<String> modifiers = Arrays.stream(diff.split(", ")).collect(Collectors.toSet());
        Set<String> expected = Stream.of(
            "ADD INDEX `created_on_updated_on` (`created_on`,`updated_on`)",
            "DROP INDEX `updated_on`",
            "ADD UNIQUE INDEX `email_name` (`email`,`name`)",
            "DROP INDEX `name`"
            ).collect(Collectors.toSet());
        assertEquals(modifiers, expected);
      } catch (CommunicationsException e) {
        assumeTrue("MySQL maybe not launched", false);
      } catch (Exception e) {
        assertTrue(false);
      }
    }
  }

  public static class ForMissingTable {
    @Test
    public void shouldDetectMissingTableRightly() throws SQLException, IOException,
        InterruptedException {
      String oldSql = "CREATE TABLE `sample` ("
          + "  `id` int(10) NOT NULL AUTO_INCREMENT,"
          + "  PRIMARY KEY (`id`)"
          + ") ENGINE=InnoDB DEFAULT CHARSET=utf8;";
      String newSql = "CREATE TABLE `new_one` ("
          + "  `id` int(10) NOT NULL AUTO_INCREMENT,"
          + "  PRIMARY KEY (`id`)"
          + ") ENGINE=InnoDB DEFAULT CHARSET=utf8;";

      try {
        String oldSchema = SCHEMA_DUMPER.dump(oldSql);
        String newSchema = SCHEMA_DUMPER.dump(newSql);

        List<Table> oldTables = SchemaParser.parse(oldSchema);
        List<Table> newTables = SchemaParser.parse(newSchema);

        String diff = DiffExtractor.extractDiff(oldTables, newTables);
        List<String> got = Arrays.stream(diff.split("\r?\n"))
            .map(line -> line.trim())
            .collect(Collectors.toList());
        List<String> expected = Arrays.asList(
            "CREATE TABLE `new_one` (",
            "`id` int(10) NOT NULL AUTO_INCREMENT,",
            "PRIMARY KEY (`id`)",
            ") ENGINE=InnoDB DEFAULT CHARSET=utf8;");
        assertEquals(got, expected);
      } catch (CommunicationsException e) {
        assumeTrue("MySQL maybe not launched", false);
      } catch (Exception e) {
        assertTrue(false);
      }
    }
  }
}
