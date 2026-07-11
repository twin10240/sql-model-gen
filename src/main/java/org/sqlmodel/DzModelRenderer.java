package org.sqlmodel;

import javax.lang.model.SourceVersion;
import java.util.List;

final class DzModelRenderer {
    String render(String packageName, String className, List<ColumnSpec> columns) {
        if (!SourceVersion.isName(packageName)) {
            throw new IllegalArgumentException("Invalid package name: " + packageName);
        }
        if (!SourceVersion.isIdentifier(className) || SourceVersion.isKeyword(className)) {
            throw new IllegalArgumentException("Invalid class name: " + className);
        }

        StringBuilder source = new StringBuilder();
        source.append("package ").append(packageName).append(";\n\n")
                .append("import com.douzone.gpd.jdbc.mybatis.model.DzAbstractModel;\n")
                .append("import com.douzone.gpd.restful.annotation.DzModel;\n")
                .append("import com.douzone.gpd.restful.annotation.DzModelField;\n")
                .append("import com.google.gson.annotations.SerializedName;\n");
        if (hasType(columns, "BigDecimal")) {
            source.append("import java.math.BigDecimal;\n");
        }
        if (hasType(columns, "LocalDateTime")) {
            source.append("import java.time.LocalDateTime;\n");
        }
        source.append("\n@DzModel(name = \"").append(className).append("\", desc = \"\")\n")
                .append("public class ").append(className).append(" extends DzAbstractModel {\n\n");

        for (ColumnSpec column : columns) {
            String label = escape(column.resultLabel());
            source.append("    @SerializedName(\"").append(label).append("\")\n")
                    .append("    @DzModelField(name = \"").append(label)
                    .append("\", desc = \"").append(escape(column.description()))
                    .append("\", colName = \"").append(label).append("\")\n")
                    .append("    private ").append(column.javaType()).append(' ')
                    .append(column.fieldName()).append(";\n\n");
        }

        for (ColumnSpec column : columns) {
            String suffix = Character.toUpperCase(column.fieldName().charAt(0)) + column.fieldName().substring(1);
            source.append("    public ").append(column.javaType()).append(" get").append(suffix).append("() {\n")
                    .append("        return ").append(column.fieldName()).append(";\n")
                    .append("    }\n\n")
                    .append("    public void set").append(suffix).append('(').append(column.javaType()).append(' ')
                    .append(column.fieldName()).append(") {\n")
                    .append("        this.").append(column.fieldName()).append(" = ").append(column.fieldName()).append(";\n")
                    .append("    }\n\n");
        }
        source.setLength(source.length() - 1);
        return source.append("}\n").toString();
    }

    private static boolean hasType(List<ColumnSpec> columns, String type) {
        for (ColumnSpec column : columns) {
            if (type.equals(column.javaType())) {
                return true;
            }
        }
        return false;
    }

    private static String escape(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", "\\r")
                .replace("\n", "\\n");
    }
}
