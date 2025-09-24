package org.sqlmodel;

import java.util.*;

class SimpleModelGeneratorImpl implements ModelGenerator {

    @Override
    public String generateModel(String sql, String className) {
        // 1. SELECT ~ FROM 사이 추출
        String upper = sql.toUpperCase();
        int start = upper.indexOf("SELECT") + "SELECT".length();
        int end = upper.indexOf("FROM");
        if (start < 0 || end < 0 || end <= start) {
            throw new IllegalArgumentException("Invalid SQL: cannot find proper SELECT ... FROM segment.");
        }
        String columnPart = sql.substring(start, end).trim();

        // 2. 컬럼 분리
        String[] cols = columnPart.split(",");

        // 3. 클래스 빌드
        StringBuilder sb = new StringBuilder();
        sb.append("import com.douzone.gpd.restful.annotation.DzModel;").append("\n");
        sb.append("import com.douzone.gpd.restful.annotation.DzModelField;").append("\n");
        sb.append("import com.google.gson.annotations.SerializedName;").append("\n");

        sb.append("\n");

        sb.append("@DzModel(name = \"").append(className).append("\", desc = \"\")\n");
        sb.append("public class ").append(className).append(" extends DzAbstractModel {\n\n");

        // 필드 정의 + 타입 기록
        List<String> fieldNames = new ArrayList<>();
        List<String> fieldTypes = new ArrayList<>();

        for (String col : cols) {
            String fieldName = extractAliasOrName(col.trim());
            // 어노테이션
            sb.append("    @SerializedName(\"").append(fieldName).append("\")\n");
            sb.append("    @DzModelField(name = \"").append(fieldName).append("\", desc = \"\", colName = \"").append(fieldName).append("\")\n");

            String[] s = fieldName.split("_");
            getType(sb, fieldNames, fieldTypes, fieldName, s);

            sb.append("\n");
        }
        sb.append("\n");

        // getter/setter (필드 타입과 일치!)
        for (int i = 0; i < fieldNames.size(); i++) {
            String fieldName = fieldNames.get(i);
            String type = fieldTypes.get(i);
            String methodName = Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);

            sb.append("    public ").append(type).append(" get").append(methodName).append("() {\n")
                    .append("        return ").append(fieldName).append(";\n")
                    .append("    }\n\n");

            sb.append("    public void set").append(methodName).append("(").append(type).append(" ").append(fieldName).append(") {\n")
                    .append("        this.").append(fieldName).append(" = ").append(fieldName).append(";\n")
                    .append("    }\n\n");
        }

        sb.append("}");
        return sb.toString();
    }

    @Override
    public String generateModelPagingAware(String sql, String className, boolean includePagingColumn) {
        String cleaned = stripComments(sql);
        String selectList = findSelectList(cleaned);

        // 바깥이 *면 FROM ( ... ) 안쪽으로 한 번 드릴다운
        if (isOnlyStar(selectList)) {
            String inner = firstFromSubquery(cleaned);
            if (inner != null) {
                cleaned = inner;
                selectList = findSelectList(cleaned);
            }
        }

        String[] items = splitSelectItems(selectList);

        StringBuilder sb = new StringBuilder();
        sb.append("import com.douzone.gpd.restful.annotation.DzModel;").append("\n");
        sb.append("import com.douzone.gpd.restful.annotation.DzModelField;").append("\n");
        sb.append("import com.google.gson.annotations.SerializedName;").append("\n");

        sb.append("\n");

        sb.append("@DzModel(name = \"").append(className).append("\", desc = \"\")\n");
        sb.append("public class ").append(className).append(" extends DzAbstractModel {\n\n");

        List<String> fieldNames = new ArrayList<>();
        List<String> fieldTypes = new ArrayList<>();

        // 1) 필드
        for (String raw : items) {
            String expr = raw.trim();
            if (expr.isEmpty() || expr.equals("*")) continue;

            String alias = extractAliasOrName(expr);
            if (!includePagingColumn && looksLikePagingColumn(expr, alias)) {
                continue; // rn/rownum 기본 제외
            }

            String fieldName = extractAliasOrName(alias);
            // 어노테이션
            sb.append("    @SerializedName(\"").append(fieldName).append("\")\n");
            sb.append("    @DzModelField(name = \"").append(fieldName).append("\", desc = \"\", colName = \"").append(fieldName).append("\")\n");

            String[] s = fieldName.split("_");
            getType(sb, fieldNames, fieldTypes, fieldName, s);

            sb.append("\n");
        }
        sb.append("\n");

        // 2) getter/setter (필드 타입과 일치!)
        for (int i = 0; i < fieldNames.size(); i++) {
            String fieldName = fieldNames.get(i);
            String type = fieldTypes.get(i);
            String methodName = Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);

            sb.append("    public ").append(type).append(" get").append(methodName).append("() {\n")
                    .append("        return ").append(fieldName).append(";\n")
                    .append("    }\n\n");

            sb.append("    public void set").append(methodName).append("(").append(type).append(" ").append(fieldName).append(") {\n")
                    .append("        this.").append(fieldName).append(" = ").append(fieldName).append(";\n")
                    .append("    }\n\n");
        }

        sb.append("}");
        return sb.toString();
    }

    private void getType(StringBuilder sb, List<String> fieldNames, List<String> fieldTypes, String fieldName, String[] s) {
//        String type = switch (s[s.length - 1]) {
//            case "SQ" -> "Integer";
//            case "QT" -> "BigDecimal";
//            default -> "String";
//        };

        String type;
        switch (s[s.length - 1]) {
            case "SQ":
                type = "Integer";
                break;
            case "QT":
            case "AMT":
            case "UM":
            case "RT":
            case "VR":
                type = "BigDecimal";
                break;
            default:
                type = "String";
                break;
        }
        sb.append("    private ").append(type).append(" ").append(fieldName).append(";\n");
        fieldNames.add(fieldName);
        fieldTypes.add(type);
    }

    // ===== helpers =====
    private static String stripComments(String s) {
        s = s.replaceAll("/\\*.*?\\*/", " "); // 블록 주석 (/* ... */)**을 제거
        s = s.replaceAll("--[^\\n]*", " "); // 한 줄 주석 (-- ...)**을 제거
        return s;
    }

    private static String findSelectList(String sql) {
        String upper = sql.toUpperCase();
        int pSel = upper.indexOf("SELECT");
        if (pSel < 0) throw new IllegalArgumentException("SELECT not found.");
        int i = pSel + "SELECT".length();
        int depth = 0; boolean inStr = false;
        for (; i < sql.length(); i++) {
            char ch = sql.charAt(i);
            if (ch == '\'') inStr = !inStr;
            if (inStr) continue;
            if (ch == '(') depth++;
            else if (ch == ')') depth = Math.max(0, depth - 1);
            if (depth == 0 && upper.startsWith(" FROM ", i)) {
                return sql.substring(pSel + "SELECT".length(), i).trim();
            }
        }
        return sql.substring(pSel + "SELECT".length()).trim();
    }

    private static boolean isOnlyStar(String list) {
        String[] parts = list.split(",");
        for (String p : parts) if (!p.trim().equals("*")) return false;
        return true;
    }

    private static String firstFromSubquery(String sql) {
        String u = sql.toUpperCase();
        int from = u.indexOf(" FROM ");
        if (from < 0) return null;
        int i = from + 6;
        while (i < sql.length() && Character.isWhitespace(sql.charAt(i))) i++;
        if (i < sql.length() && sql.charAt(i) == '(') {
            int start = i + 1, depth = 1; boolean inStr = false;
            for (i = start; i < sql.length(); i++) {
                char ch = sql.charAt(i);
                if (ch == '\'') inStr = !inStr;
                if (inStr) continue;
                if (ch == '(') depth++;
                else if (ch == ')') { depth--; if (depth == 0) return sql.substring(start, i); }
            }
        }
        return null;
    }

    private static String[] splitSelectItems(String list) {
        List<String> out = new ArrayList<>();
        StringBuilder buf = new StringBuilder();
        int depth = 0; boolean inStr = false;
        for (int i=0; i<list.length(); i++) {
            char ch = list.charAt(i);
            if (ch=='\'') { inStr = !inStr; buf.append(ch); continue; }
            if (!inStr) {
                if (ch=='(') { depth++; buf.append(ch); continue; }
                if (ch==')') { depth = Math.max(0, depth-1); buf.append(ch); continue; }
                if (ch==',' && depth==0) { out.add(buf.toString().trim()); buf.setLength(0); continue; }
            }
            buf.append(ch);
        }
//        if (!buf.isEmpty())
        if (buf.length() > 0)
            out.add(buf.toString().trim());

        return out.toArray(new String[0]);
    }

    private static String extractAliasOrName(String expr) {
        String[] parts = expr.trim().split("\\s+");
        for (int i = 0; i < parts.length; i++) {
            if ("AS".equalsIgnoreCase(parts[i]) && i + 1 < parts.length) {
                return sanitize(parts[i + 1]);
            }
        }

        if (parts.length > 1) return sanitize(parts[parts.length - 1]);

        String col = parts[0];
        if (col.contains(".")) {
            String[] ss = col.split("\\.");
            col = ss[ss.length - 1];
        }

        return sanitize(col);
    }

    private static boolean looksLikePagingColumn(String expr, String alias) {
        String u = expr.toUpperCase();
        if (u.contains("ROWNUM")) return true;
        if (u.contains("ROW_NUMBER")) return true;
        if (alias != null) {
            String a = alias.toLowerCase();

            return a.equals("rn") || a.equals("rownum");
        }

        return false;
    }

    private static String sanitize(String s) {
        return s.replaceAll("[^A-Za-z0-9_]", "_");
    }

    private static String toCamelCase(String name) {
        name = sanitize(name).toLowerCase();
        String[] parts = name.split("_");
        StringBuilder result = new StringBuilder(parts[0]);
        for (int i = 1; i < parts.length; i++) {
            if (parts[i].isEmpty()) continue;
            result.append(Character.toUpperCase(parts[i].charAt(0))).append(parts[i].substring(1));
        }
        String cand = result.toString();
        if (cand.matches("^[0-9].*")) cand = "f_" + cand;
//        if (java.util.Set.of("class","enum","record","default","package").contains(cand)) cand = cand + "_";
        // Java 8버전용
        Set<String> reserved = new HashSet<>(Arrays.asList( "class", "enum", "record", "default", "package" ));
        if (reserved.contains(cand)) {
            cand = cand + "_";
        }
        
        return cand;
    }
}
