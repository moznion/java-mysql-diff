package net.moznion.mysql.diff;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

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

@RunWith(Enclosed.class)
public class DiffExtractorTest {
  public static class forTableDiff {
    @Test
    public void shouldDetectColumnDiffRightly() throws SQLException, IOException,
        InterruptedException {
      String oldSQL = "CREATE TABLE `sample` (" +
          "  `id` int(10) NOT NULL AUTO_INCREMENT," +
          "  `title` varchar(64) NOT NULL," +
          "  `created_on` int(10) unsigned NOT NULL," +
          "  PRIMARY KEY (`id`)" +
          ") ENGINE=InnoDB DEFAULT CHARSET=utf8;";
      String newSQL = "CREATE TABLE `sample` (" +
          "  `id` int(10) NOT NULL AUTO_INCREMENT," +
          "  `title` varchar(64) DEFAULT NULL," +
          "  `updated_on` int(10) unsigned NOT NULL," +
          "  PRIMARY KEY (`id`)" +
          ") ENGINE=InnoDB DEFAULT CHARSET=utf8;";

      SchemaDumper sd = new SchemaDumper();
      String oldSchema = sd.dump(oldSQL);
      String newSchema = sd.dump(newSQL);

      List<Table> oldTables = SchemaParser.parse(oldSchema);
      List<Table> newTables = SchemaParser.parse(newSchema);

      String diff = DiffExtractor.extractDiff(oldTables, newTables);
      diff = diff.replaceAll("\r?\n", "");

      assertTrue(diff.startsWith("ALTER TABLE `sample` "));
      assertTrue(diff.endsWith(";"));
      diff = diff.replaceFirst("^ALTER TABLE `sample` ", "");
      diff = diff.replaceFirst(";$", "");

      Set<String> modifiers = Arrays.stream(diff.split(", ")).collect(Collectors.toSet());
      Set<String> expected = Stream.of(
          "DROP `created_on`",
          "MODIFY `title` varchar(64) DEFAULT NULL",
          "ADD `updated_on` int(10) unsigned NOT NULL"
          ).collect(Collectors.toSet());
      assertEquals(modifiers, expected);
    }

    @Test
    public void shouldDetectKeyDiffRightly() throws SQLException, IOException, InterruptedException {
      String oldSQL = "CREATE TABLE `sample` (" +
          "  `id` int(10) NOT NULL AUTO_INCREMENT," +
          "  `name` varchar(16) NOT NULL," +
          "  `email` varchar(16) NOT NULL," +
          "  `title` varchar(64) NOT NULL," +
          "  `created_on` int (10) unsigned NOT NULL," +
          "  `updated_on` int (10) unsigned NOT NULL," +
          "  PRIMARY KEY (`id`)," +
          "  UNIQUE KEY `name` (`name`)," +
          "  KEY `updated_on` (`updated_on`)" +
          ") ENGINE=InnoDB DEFAULT CHARSET=utf8;";
      String newSQL = "CREATE TABLE `sample` (" +
          "  `id` int(10) NOT NULL AUTO_INCREMENT," +
          "  `name` varchar(16) NOT NULL," +
          "  `email` varchar(16) NOT NULL," +
          "  `title` varchar(64) NOT NULL," +
          "  `created_on` int (10) unsigned NOT NULL," +
          "  `updated_on` int (10) unsigned NOT NULL," +
          "  PRIMARY KEY (`id`)," +
          "  UNIQUE KEY `identifier` (`email`,`name`)," +
          "  KEY `timestamp` (`created_on`,`updated_on`)" +
          ") ENGINE=InnoDB DEFAULT CHARSET=utf8;";

      SchemaDumper sd = new SchemaDumper();
      String oldSchema = sd.dump(oldSQL);
      String newSchema = sd.dump(newSQL);

      List<Table> oldTables = SchemaParser.parse(oldSchema);
      List<Table> newTables = SchemaParser.parse(newSchema);

      String diff = DiffExtractor.extractDiff(oldTables, newTables);
      diff = diff.replaceAll("\r?\n", "");

      assertTrue(diff.startsWith("ALTER TABLE `sample` "));
      assertTrue(diff.endsWith(";"));
      diff = diff.replaceFirst("^ALTER TABLE `sample` ", "");
      diff = diff.replaceFirst(";$", "");

      Set<String> modifiers = Arrays.stream(diff.split(", ")).collect(Collectors.toSet());
      Set<String> expected = Stream.of(
          "ADD INDEX `created_on_updated_on` (`created_on`,`updated_on`)",
          "DROP INDEX `updated_on`",
          "ADD UNIQUE INDEX `email_name` (`email`,`name`)",
          "DROP INDEX `name`"
          ).collect(Collectors.toSet());
      assertEquals(modifiers, expected);
    }
  }
}
