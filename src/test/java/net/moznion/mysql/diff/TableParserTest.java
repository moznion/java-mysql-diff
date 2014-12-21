package net.moznion.mysql.diff;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import net.moznion.mysql.diff.model.Table;

import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import java.util.Map;

@RunWith(Enclosed.class)
public class TableParserTest {
	public static class Normally {
		@Test
		public void basicCase() {
			Table table = TableParser.parse("CREATE TABLE foo (\n" +
					"  id INT(11) NOT NULL auto_increment,\n" +
					"  secondary_id INT(11) NOT NULL,\n" +
					"  foreign_id INT(11) NOT NULL,\n" +
					"  PRIMARY KEY (id, secondary_id),\n" +
					"  KEY fid (foreign_id),\n" +
					");\n" +
					"/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;");
			assertTrue(table.getName().equals("foo"));
			assertTrue(table.getPrimaryKey().equals("id, secondary_id"));

			Map<String, Boolean> primaries = table.getPrimaries();
			assertTrue(primaries.remove("id"));
			assertTrue(primaries.remove("secondary_id"));
			assertTrue(primaries.isEmpty());

			Map<String, String> indices = table.getIndices();
			assertTrue(indices.remove("fid").equals("foreign_id"));
			assertTrue(indices.isEmpty());

			Map<String, String> fields = table.getFields();
			assertTrue(fields.remove("id").equals("INT(11) NOT NULL auto_increment"));
			assertTrue(fields.remove("secondary_id").equals("INT(11) NOT NULL"));
			assertTrue(fields.remove("foreign_id").equals("INT(11) NOT NULL"));
			assertTrue(fields.isEmpty());

			assertTrue(table.getUniqueKeys().isEmpty());
			assertTrue(table.getFullTextIndices().isEmpty());
			assertTrue(table.getOptions().isEmpty());
		}
	}

	@RunWith(Enclosed.class)
	public static class ExceptionalHandling {
		public static class IllegalArgumentsErrorExpectation {
			@Test
			public void missingAnyStatements() {
				try {
					TableParser.parse("");
				} catch (IllegalArgumentException e) {
					assertTrue(e.getMessage().equals(
							"Table definition doesn't include a table name"));
					return;
				}
				fail();
			}

			@Test
			public void missingCreateTableStatement() {
				try {
					TableParser.parse("id INT(11) NOT NULL auto_increment");
				} catch (IllegalArgumentException e) {
					assertTrue(e.getMessage().equals(
							"Table definition doesn't include a table name"));
					return;
				}
				fail();
			}

			@Test
			public void duplicatedPrimaryKeys() {
				try {
					TableParser.parse("CREATE TABLE foo (\n" +
							"  id INT(11) NOT NULL auto_increment,\n" +
							"  foreign_id INT(11) NOT NULL,\n" +
							"  PRIMARY KEY (id),\n" +
							"  PRIMARY KEY (foreign_id)\n" +
							");\n");
				} catch (IllegalArgumentException e) {
					assertTrue(e.getMessage().equals(
							"Two primary keys in table foo: 'foreign_id', 'id'"));
					return;
				}
				fail();
			}

			@Test
			public void duplicatedIndice() {
				try {
					TableParser.parse("CREATE TABLE foo (\n" +
							"  id INT(11) NOT NULL auto_increment,\n" +
							"  INDEX __id__ (id),\n" +
							"  INDEX __id__ (id)\n" +
							");\n");
				} catch (IllegalArgumentException e) {
					assertTrue(e.getMessage().equals(
							"index '__id__' duplicated in table 'foo'"));
					return;
				}
				fail();
			}

			@Test
			public void duplicatedFullTextIndice() {
				try {
					TableParser.parse("CREATE TABLE foo (\n" +
							"  message TEXT NOT NULL,\n" +
							"  FULLTEXT __message__ (message),\n" +
							"  FULLTEXT __message__ (message)\n" +
							");\n");
				} catch (IllegalArgumentException e) {
					assertTrue(e.getMessage().equals(
							"FULLTEXT index '__message__' duplicated in table 'foo'"));
					return;
				}
				fail();
			}

			@Test
			public void duplicatedIndiceWithFullTextIndex() {
				try {
					TableParser.parse("CREATE TABLE foo (\n" +
							"  message TEXT NOT NULL,\n" +
							"  INDEX __message__ (message),\n" +
							"  FULLTEXT __message__ (message)\n" +
							");\n");
				} catch (IllegalArgumentException e) {
					assertTrue(e.getMessage().equals(
							"index '__message__' duplicated in table 'foo'"));
					return;
				}
				fail();
			}

			@Test
			public void duplicatedFields() {
				try {
					TableParser.parse("CREATE TABLE foo (\n" +
							"  message TEXT NOT NULL,\n" +
							"  message TEXT NOT NULL\n" +
							");\n");
				} catch (IllegalArgumentException e) {
					assertTrue(e.getMessage().equals(
							"definition for field 'message' duplicated in table 'foo'"));
					return;
				}
				fail();
			}

			@Test
			public void missingTerminator() {
				try {
					TableParser.parse("CREATE TABLE foo (\n" +
							"  id INT(11) NOT NULL auto_increment\n");
				} catch (IllegalArgumentException e) {
					assertTrue(e.getMessage().equals("table 'foo' didn't have terminator"));
					return;
				}
				fail();
			}

			@Test
			public void withGarbage() {
				try {
					TableParser.parse("CREATE TABLE foo (\n" +
							"  id INT(11) NOT NULL auto_increment\n" +
							");\n" +
							"I\n" +
							"am\n" +
							"garbage\n");
				} catch (IllegalArgumentException e) {
					assertTrue(e.getMessage().equals("table 'foo' had trailing garbage:\n" +
							"I\n" +
							"am\n" +
							"garbage"));
					return;
				}
				fail();
			}
		}
	}
}
