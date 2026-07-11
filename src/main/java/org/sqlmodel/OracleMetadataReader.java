package org.sqlmodel;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class OracleMetadataReader {
    private final OracleCommentReader commentReader;

    OracleMetadataReader(OracleConfig config) {
        this.commentReader = new OracleCommentReader(config);
    }

    List<ColumnSpec> read(Connection connection, SqlInspection inspection) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(inspection.normalizedSql())) {
            ResultSetMetaData metadata = statement.getMetaData();
            if (metadata != null) return columns(metadata, commentReader.read(connection, inspection));
        }
        String fallback = "SELECT * FROM (" + inspection.normalizedSql() + ") WHERE 1 = 0";
        try (PreparedStatement statement = connection.prepareStatement(fallback);
             ResultSet result = statement.executeQuery()) {
            return columns(result.getMetaData(), commentReader.read(connection, inspection));
        }
    }

    private static List<ColumnSpec> columns(ResultSetMetaData metadata, Map<String, String> comments)
            throws SQLException {
        List<ColumnSpec> columns = new ArrayList<ColumnSpec>();
        Set<String> labels = new HashSet<String>();
        Set<String> fields = new HashSet<String>();
        for (int ordinal = 1; ordinal <= metadata.getColumnCount(); ordinal++) {
            String label = metadata.getColumnLabel(ordinal);
            String field = JavaNames.toFieldName(label);
            if (!labels.add(label.toLowerCase(java.util.Locale.ROOT)) || !fields.add(field)) {
                throw new IllegalArgumentException("Duplicate result labels or Java field names; use AS aliases to make each column unique");
            }
            String javaType = JdbcTypeMapper.map(metadata.getColumnType(ordinal), metadata.getPrecision(ordinal),
                    metadata.getScale(ordinal), metadata.getColumnTypeName(ordinal));
            String description = comments.get(label);
            columns.add(new ColumnSpec(ordinal, label, field, javaType, description == null ? "" : description));
        }
        return columns;
    }
}
