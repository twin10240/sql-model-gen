package org.sqlmodel;

import java.sql.Types;
import java.util.Locale;

final class JdbcTypeMapper {
    private JdbcTypeMapper() {}

    static String map(int jdbcType, int precision, int scale, String databaseTypeName) {
        String typeName = databaseTypeName == null ? "" : databaseTypeName.toUpperCase(Locale.ROOT);
        if (typeName.contains("TIMESTAMP") || jdbcType == Types.DATE || jdbcType == Types.TIMESTAMP
                || jdbcType == Types.TIMESTAMP_WITH_TIMEZONE) {
            return "LocalDateTime";
        }
        if (jdbcType == Types.NUMERIC || jdbcType == Types.DECIMAL || "NUMBER".equals(typeName)) {
            if (scale == 0 && precision > 0 && precision <= 9) return "Integer";
            if (scale == 0 && precision > 0 && precision <= 18) return "Long";
            return "BigDecimal";
        }
        if (jdbcType == Types.FLOAT || jdbcType == Types.REAL || jdbcType == Types.DOUBLE
                || "BINARY_FLOAT".equals(typeName) || "BINARY_DOUBLE".equals(typeName)) {
            return "Double";
        }
        switch (jdbcType) {
            case Types.CHAR:
            case Types.VARCHAR:
            case Types.LONGVARCHAR:
            case Types.NCHAR:
            case Types.NVARCHAR:
            case Types.LONGNVARCHAR:
            case Types.CLOB:
            case Types.NCLOB:
                return "String";
            default:
                return "String";
        }
    }
}
