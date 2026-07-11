package org.sqlmodel;

final class ColumnSpec {
    private final int ordinal;
    private final String resultLabel;
    private final String fieldName;
    private final String javaType;
    private final String description;

    ColumnSpec(int ordinal, String resultLabel, String fieldName, String javaType, String description) {
        if (isBlank(resultLabel) || isBlank(fieldName)) {
            throw new IllegalArgumentException("Result label and field name are required");
        }
        this.ordinal = ordinal;
        this.resultLabel = resultLabel;
        this.fieldName = fieldName;
        this.javaType = javaType;
        this.description = description;
    }

    int ordinal() { return ordinal; }
    String resultLabel() { return resultLabel; }
    String fieldName() { return fieldName; }
    String javaType() { return javaType; }
    String description() { return description; }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
