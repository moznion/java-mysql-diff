package net.moznion.mysql.diff;

import net.moznion.mysql.diff.model.Table;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Parser for SQL table definition.
 * 
 * @author moznion
 *
 */
public class TableParser {
	public static void parse(String tableDefinition) {
		Matcher matcherForQuoteNames = Pattern.compile("`([^`]+)`").matcher(tableDefinition);
		if (matcherForQuoteNames.find()) {
			tableDefinition = matcherForQuoteNames.replaceAll(matcherForQuoteNames.group(1));
		}

		List<String> lines = Arrays.asList(tableDefinition.split("\\r?\\n")).stream().map(line -> {
			line = line.trim();
			line = line.replaceFirst(",$", "");
			return line;
		}).filter(line -> !line.isEmpty()).collect(Collectors.toList());

		String firstLine = lines.remove(0);
		Matcher matcherForTableName =
				Pattern.compile("^\\s*create\\s+table\\s+(\\S+)\\s+\\(\\s*$",
						Pattern.CASE_INSENSITIVE)
						.matcher(firstLine);
		if (!matcherForTableName.find()) {
			throw new IllegalArgumentException("Table definition doesn't include a table name");
		}

		Table table = new Table();
		table.setName(matcherForTableName.group(1));

		Pattern patternForPrimaryKey =
				Pattern.compile("^primary\\s+key\\s+(.+)$", Pattern.CASE_INSENSITIVE);
		Pattern patternForKey =
				Pattern
						.compile(
								"^(key|index|unique(?:\\s+(?:key|index))?)\\s+(\\S+?)(?:\\s+using\\s+(?:btree|hash|rtree))?\\s*\\((.*)\\)$",
								Pattern.CASE_INSENSITIVE);
		Pattern patternForFullTextIndex =
				Pattern.compile("^fulltext(?:\\s+(key|index))?\\s+(\\S+?)\\s*\\((.*)\\)$",
						Pattern.CASE_INSENSITIVE);
		Pattern patternForEndOfTableDefinition = Pattern.compile("^\\)\\s*(.*?);$");
		Pattern patternForFieldDefinition = Pattern.compile("^(\\S+)\\s*(.*)");

		Iterator<String> lineIter = lines.iterator();
		while (lineIter.hasNext()) {
			String line = lineIter.next();
			lineIter.remove();

			// For primary key
			Matcher matcherForPrimaryKey = patternForPrimaryKey.matcher(line);
			if (matcherForPrimaryKey.find()) {
				String primaryKey = matcherForPrimaryKey.group(1);
				if (table.hasPrimaryKey()) {
					throw new IllegalArgumentException(new StringBuilder()
							.append("Two primary keys in table ").append(table.getName())
							.append(": '")
							.append(primaryKey).append("', '").append(table.getPrimaryKey())
							.append("'")
							.toString());
				}
				table.setPrimaryKey(primaryKey);

				String primaries = primaryKey.replaceFirst("^\\(", "").replaceFirst("\\)$", "");
				for (String primary : primaries.split(",")) {
					table.addPrimary(primary.trim());
				}
				continue;
			}

			// For key
			Matcher matcherForKey = patternForKey.matcher(line);
			if (matcherForKey.find()) {
				String type = matcherForKey.group(1);
				String key = matcherForKey.group(2);
				String value = matcherForKey.group(3);

				if (table.hasIndex(key)) {
					throw new IllegalArgumentException(new StringBuilder().append("index '")
							.append(key)
							.append("' duplicated in table '").append(table.getName()).append("'")
							.toString());
				}
				table.putIndex(key, value);

				if (type.compareToIgnoreCase("unique") == 0) {
					table.addUniqueKey(key);
				}
				continue;
			}

			// For full-text index
			Matcher matcherForFullTextIndex = patternForFullTextIndex.matcher(line);
			if (matcherForFullTextIndex.find()) {
				String key = matcherForKey.group(1);
				String value = matcherForKey.group(2);

				if (table.hasFullTextIndex(key)) {
					throw new IllegalArgumentException(new StringBuilder()
							.append("FULLTEXT index '")
							.append(key).append("' duplicated in table '").append(table.getName())
							.append("'")
							.toString());
				}

				table.putIndex(key, value);
				table.addFullTextIndex(key);
				continue;
			}

			// For end of table definition
			Matcher matcherForEndOfTableDefinition = patternForEndOfTableDefinition.matcher(line);
			if (matcherForEndOfTableDefinition.find()) {
				table.setOptions(matcherForEndOfTableDefinition.group(1));
				break;
			}

			// For field definition
			Matcher matcherForFieldDefinition = patternForFieldDefinition.matcher(line);
			if (matcherForFieldDefinition.find()) {
				String fieldName = matcherForFieldDefinition.group(1);
				String fieldDefinition = matcherForFieldDefinition.group(2);

				if (table.hasField(fieldName)) {
					throw new IllegalArgumentException(new StringBuilder()
							.append("definition for field '")
							.append(fieldName).append("' duplicated in table '")
							.append(table.getName())
							.append("'").toString());
				}

				table.putField(fieldName, fieldDefinition);
				continue;
			}

			throw new IllegalArgumentException(new StringBuilder()
					.append("Cannot parse line in definition for table '").append(table.getName())
					.append("'").toString());
		}

		if (table.getOptions() == null) {
			throw new IllegalArgumentException(new StringBuilder().append("table '")
					.append(table.getName()).append("' didn't have terminator").toString());
		}

		List<String> garbageLines =
				lines.stream().filter(line -> !line.matches("^/\\*!40\\d{3} .*? \\*/;"))
						.filter(line -> !line.matches("^(?:SET |DROP TABLE)"))
						.collect(Collectors.toList());
		if (!garbageLines.isEmpty()) {
			throw new IllegalArgumentException(new StringBuilder().append("table '")
					.append(table.getName()).append("' had trailing garbage:\n")
					.append(String.join("\n", garbageLines)).toString());
		}
	}
}
