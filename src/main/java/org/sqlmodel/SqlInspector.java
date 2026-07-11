package org.sqlmodel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class SqlInspector {
    private static final String IDENTIFIER = "[A-Za-z_$#][A-Za-z0-9_$#]*";
    private static final Pattern TABLE = Pattern.compile(
            "(?i)\\b(?:FROM|JOIN)\\s+(" + IDENTIFIER + ")(?:\\.(" + IDENTIFIER + "))?\\s+(?:AS\\s+)?(" + IDENTIFIER + ")");
    private static final Pattern DIRECT_COLUMN = Pattern.compile(
            "(?i)^\\s*(" + IDENTIFIER + ")\\.(" + IDENTIFIER + ")(?:\\s+(?:AS\\s+)?(" + IDENTIFIER + "))?\\s*$");

    private SqlInspector() {}

    static SqlInspection inspect(String sql) {
        String normalized = normalize(sql);
        int select = findTopLevelKeyword(normalized, "SELECT", 0);
        if (select < 0 || (!startsWithKeyword(normalized, "SELECT") && !startsWithKeyword(normalized, "WITH"))) {
            throw new IllegalArgumentException("Only SELECT or WITH queries are supported");
        }
        int from = findTopLevelKeyword(normalized, "FROM", select + 6);
        if (from < 0) throw new IllegalArgumentException("SELECT query must contain FROM");

        Map<String, TableName> aliases = tableAliases(normalized.substring(from));
        Map<String, SourceColumn> sources = new HashMap<String, SourceColumn>();
        for (String item : splitSelectItems(normalized.substring(select + 6, from))) {
            Matcher match = DIRECT_COLUMN.matcher(item);
            if (!match.matches()) continue;
            TableName table = aliases.get(match.group(1).toUpperCase(Locale.ROOT));
            if (table == null) continue;
            String column = match.group(2);
            String label = match.group(3) == null ? column : match.group(3);
            sources.put(label, new SourceColumn(table.owner, table.table, column));
        }
        return new SqlInspection(normalized, sources);
    }

    private static String normalize(String sql) {
        if (sql == null) throw new IllegalArgumentException("SQL is required");
        String value = stripComments(sql).trim();
        int semicolon = findOutsideLiteral(value, ';', 0);
        if (semicolon >= 0) {
            if (semicolon != value.length() - 1 || findOutsideLiteral(value, ';', semicolon + 1) >= 0) {
                throw new IllegalArgumentException("Multiple SQL statements are not supported");
            }
            value = value.substring(0, value.length() - 1).trim();
        }
        if (value.isEmpty()) throw new IllegalArgumentException("SQL is required");
        return value;
    }

    private static String stripComments(String sql) {
        StringBuilder result = new StringBuilder(sql.length());
        boolean string = false;
        for (int i = 0; i < sql.length(); i++) {
            char c = sql.charAt(i);
            if (string) {
                result.append(c);
                if (c == '\'' && i + 1 < sql.length() && sql.charAt(i + 1) == '\'') result.append(sql.charAt(++i));
                else if (c == '\'') string = false;
            } else if (c == '\'') {
                string = true;
                result.append(c);
            } else if (sql.startsWith("--", i)) {
                int end = sql.indexOf('\n', i + 2);
                if (end < 0) break;
                result.append('\n');
                i = end;
            } else if (sql.startsWith("/*", i)) {
                int end = sql.indexOf("*/", i + 2);
                if (end < 0) throw new IllegalArgumentException("Unterminated SQL comment");
                boolean newline = false;
                for (int j = i + 2; j < end; j++) {
                    if (sql.charAt(j) == '\n') {
                        result.append('\n');
                        newline = true;
                    }
                }
                if (!newline && result.length() > 0 && !Character.isWhitespace(result.charAt(result.length() - 1))
                        && end + 2 < sql.length() && !Character.isWhitespace(sql.charAt(end + 2))) {
                    result.append(' ');
                }
                i = end + 1;
            } else {
                result.append(c);
            }
        }
        return result.toString();
    }

    private static boolean startsWithKeyword(String sql, String keyword) {
        return sql.regionMatches(true, 0, keyword, 0, keyword.length())
                && (sql.length() == keyword.length() || !isIdentifier(sql.charAt(keyword.length())));
    }

    private static int findTopLevelKeyword(String sql, String keyword, int start) {
        int depth = 0;
        boolean string = false;
        for (int i = start; i < sql.length(); i++) {
            char c = sql.charAt(i);
            if (string) {
                if (c == '\'' && i + 1 < sql.length() && sql.charAt(i + 1) == '\'') i++;
                else if (c == '\'') string = false;
                continue;
            }
            if (c == '\'') { string = true; continue; }
            if (sql.startsWith("--", i)) { i = lineEnd(sql, i + 2); continue; }
            if (sql.startsWith("/*", i)) { i = blockEnd(sql, i + 2); continue; }
            if (c == '(') depth++;
            else if (c == ')') depth--;
            else if (depth == 0 && matchesKeyword(sql, i, keyword)) return i;
        }
        return -1;
    }

    private static boolean matchesKeyword(String sql, int index, String keyword) {
        int end = index + keyword.length();
        return end <= sql.length() && sql.regionMatches(true, index, keyword, 0, keyword.length())
                && (index == 0 || !isIdentifier(sql.charAt(index - 1)))
                && (end == sql.length() || !isIdentifier(sql.charAt(end)));
    }

    private static boolean isIdentifier(char c) {
        return Character.isLetterOrDigit(c) || c == '_' || c == '$' || c == '#';
    }

    private static int lineEnd(String sql, int start) {
        int end = sql.indexOf('\n', start);
        return end < 0 ? sql.length() : end;
    }

    private static int blockEnd(String sql, int start) {
        int end = sql.indexOf("*/", start);
        if (end < 0) throw new IllegalArgumentException("Unterminated SQL comment");
        return end + 1;
    }

    private static int findOutsideLiteral(String sql, char target, int start) {
        boolean string = false;
        for (int i = start; i < sql.length(); i++) {
            char c = sql.charAt(i);
            if (c == '\'' && string && i + 1 < sql.length() && sql.charAt(i + 1) == '\'') { i++; continue; }
            if (c == '\'') string = !string;
            else if (!string && c == target) return i;
        }
        return -1;
    }

    private static List<String> splitSelectItems(String selectList) {
        List<String> items = new ArrayList<String>();
        int depth = 0;
        int start = 0;
        boolean string = false;
        for (int i = 0; i < selectList.length(); i++) {
            char c = selectList.charAt(i);
            if (c == '\'' && string && i + 1 < selectList.length() && selectList.charAt(i + 1) == '\'') { i++; continue; }
            if (c == '\'') string = !string;
            else if (!string && c == '(') depth++;
            else if (!string && c == ')') depth--;
            else if (!string && depth == 0 && c == ',') {
                items.add(selectList.substring(start, i));
                start = i + 1;
            }
        }
        items.add(selectList.substring(start));
        return items;
    }

    private static Map<String, TableName> tableAliases(String fromClause) {
        Map<String, TableName> aliases = new HashMap<String, TableName>();
        Matcher matcher = TABLE.matcher(fromClause);
        while (matcher.find()) {
            String owner = matcher.group(2) == null ? null : matcher.group(1);
            String table = matcher.group(2) == null ? matcher.group(1) : matcher.group(2);
            aliases.put(matcher.group(3).toUpperCase(Locale.ROOT), new TableName(owner, table));
        }
        return aliases;
    }

    private static final class TableName {
        private final String owner;
        private final String table;

        private TableName(String owner, String table) {
            this.owner = owner;
            this.table = table;
        }
    }
}
