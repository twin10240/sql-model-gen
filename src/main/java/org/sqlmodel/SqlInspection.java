package org.sqlmodel;

import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

final class SqlInspection {
    private final String normalizedSql;
    private final Map<String, SourceColumn> sourceColumns;

    SqlInspection(String normalizedSql, Map<String, SourceColumn> sourceColumns) {
        this.normalizedSql = normalizedSql;
        Map<String, SourceColumn> copy = new TreeMap<String, SourceColumn>(String.CASE_INSENSITIVE_ORDER);
        copy.putAll(sourceColumns);
        this.sourceColumns = Collections.unmodifiableMap(copy);
    }

    String normalizedSql() { return normalizedSql; }
    Map<String, SourceColumn> sourceColumns() { return sourceColumns; }
}
