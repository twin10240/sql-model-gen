package org.sqlmodel;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

final class OracleCommentReader {
    private static final String SQL = "SELECT COMMENTS\nFROM ALL_COL_COMMENTS\n"
            + "WHERE OWNER = ? AND TABLE_NAME = ? AND COLUMN_NAME = ?";
    private final String schema;

    OracleCommentReader(OracleConfig config) {
        this.schema = config.schema();
    }

    Map<String, String> read(Connection connection, SqlInspection inspection) throws SQLException {
        Map<String, String> comments = new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER);
        for (Map.Entry<String, SourceColumn> entry : inspection.sourceColumns().entrySet()) {
            SourceColumn source = entry.getValue();
            try (PreparedStatement statement = connection.prepareStatement(SQL)) {
                statement.setString(1, upper(source.owner() == null ? schema : source.owner()));
                statement.setString(2, upper(source.table()));
                statement.setString(3, upper(source.column()));
                try (ResultSet result = statement.executeQuery()) {
                    String comment = result.next() ? result.getString(1) : null;
                    comments.put(entry.getKey(), comment == null ? "" : comment);
                }
            }
        }
        return comments;
    }

    private static String upper(String value) {
        return value.toUpperCase(Locale.ROOT);
    }
}
