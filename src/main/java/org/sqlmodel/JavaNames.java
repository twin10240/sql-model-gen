package org.sqlmodel;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

final class JavaNames {
    private static final Set<String> KEYWORDS = new HashSet<String>(Arrays.asList(
            "abstract", "assert", "boolean", "break", "byte", "case", "catch", "char", "class",
            "const", "continue", "default", "do", "double", "else", "enum", "extends", "final",
            "finally", "float", "for", "goto", "if", "implements", "import", "instanceof", "int",
            "interface", "long", "native", "new", "package", "private", "protected", "public",
            "return", "short", "static", "strictfp", "super", "switch", "synchronized", "this",
            "throw", "throws", "transient", "try", "void", "volatile", "while", "true", "false", "null"));

    private JavaNames() {}

    static String toFieldName(String label) {
        String[] words = label.split("[^A-Za-z0-9]+");
        StringBuilder result = new StringBuilder();
        for (String word : words) {
            if (word.isEmpty()) continue;
            String normalized = isAllUpperCase(word) ? word.toLowerCase(Locale.ROOT) : word;
            if (result.length() == 0) {
                result.append(Character.toLowerCase(normalized.charAt(0))).append(normalized.substring(1));
            } else {
                result.append(Character.toUpperCase(normalized.charAt(0))).append(normalized.substring(1));
            }
        }
        if (result.length() == 0) result.append("field");
        if (!Character.isJavaIdentifierStart(result.charAt(0))) result.insert(0, "f_");
        for (int i = 1; i < result.length(); i++) {
            if (!Character.isJavaIdentifierPart(result.charAt(i))) result.setCharAt(i, '_');
        }
        if (KEYWORDS.contains(result.toString())) result.append('_');
        return result.toString();
    }

    static String toAccessorSuffix(String label) {
        String field = toFieldName(label);
        return Character.toUpperCase(field.charAt(0)) + field.substring(1);
    }

    private static boolean isAllUpperCase(String value) {
        return value.equals(value.toUpperCase(Locale.ROOT));
    }
}
