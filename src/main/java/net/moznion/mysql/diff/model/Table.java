package net.moznion.mysql.diff.model;

import java.util.HashMap;
import java.util.Map;

import lombok.Data;

@Data
public class Table {
	private String name;
	private String primaryKey;
	private Map<String, Boolean> primaries = new HashMap<>();
	private Map<String, String> indices = new HashMap<>();
	private Map<String, Boolean> uniqueKeys = new HashMap<>();
	private Map<String, Boolean> fullTextIndices = new HashMap<>();
	private Map<String, String> fields = new HashMap<>();
	private String options;

	public boolean hasPrimaryKey() {
		if (primaryKey == null) {
			return false;
		}
		return true;
	}

	public boolean hasIndex(String key) {
		return indices.containsKey(key);
	}

	public boolean hasFullTextIndex(String key) {
		return fullTextIndices.containsKey(key);
	}

	public boolean hasField(String fieldName) {
		return fields.containsKey(fieldName);
	}

	public void addPrimary(String primary) {
		primaries.put(primary, true);
	}

	public void putIndex(String key, String value) {
		indices.put(key, value);
	}

	public void addFullTextIndex(String key) {
		fullTextIndices.put(key, true);
	}

	public void addUniqueKey(String key) {
		uniqueKeys.put(key, true);
	}

	public void putField(String fieldName, String fieldDefinition) {
		fields.put(fieldName, fieldDefinition);
	}
}
