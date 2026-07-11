package org.sqlmodel;

import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SqlInspectorTest {
    @Test
    public void removesLeadingCommentsAndOneTrailingSemicolon() {
        SqlInspection inspection = SqlInspector.inspect(
                "/* generated */\n-- report\nSELECT E.EMP_NO FROM HR_EMP E;");

        assertEquals("SELECT E.EMP_NO FROM HR_EMP E", inspection.normalizedSql());
    }

    @Test
    public void acceptsSelectAndWithQueries() {
        assertEquals("SELECT 1 FROM DUAL", SqlInspector.inspect("SELECT 1 FROM DUAL").normalizedSql());
        assertEquals("WITH X AS (SELECT 1 N FROM DUAL) SELECT X.N FROM X",
                SqlInspector.inspect("WITH X AS (SELECT 1 N FROM DUAL) SELECT X.N FROM X").normalizedSql());
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsUpdate() {
        SqlInspector.inspect("UPDATE HR_EMP SET EMP_NM = 'x'");
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsMultipleStatements() {
        SqlInspector.inspect("SELECT 1 FROM DUAL; SELECT 2 FROM DUAL");
    }

    @Test
    public void commasInsideFunctionsAndEscapedStringLiteralsDoNotSplitItems() {
        SqlInspection inspection = SqlInspector.inspect(
                "SELECT COALESCE(E.EMP_NM, 'Last, First''s') AS displayName, E.EMP_NO FROM HR_EMP E");

        assertFalse(inspection.sourceColumns().containsKey("displayName"));
        assertEquals("EMP_NO", inspection.sourceColumns().get("EMP_NO").column());
    }

    @Test
    public void mapsAliasedAndUnaliasedDirectColumnsCaseInsensitively() {
        SqlInspection inspection = SqlInspector.inspect(
                "SELECT E.EMP_NM AS employeeName, E.EMP_NO FROM HR_EMP E");
        Map<String, SourceColumn> columns = inspection.sourceColumns();

        assertEquals(new SourceColumn(null, "HR_EMP", "EMP_NM"), columns.get("EMPLOYEENAME"));
        assertEquals(new SourceColumn(null, "HR_EMP", "EMP_NO"), columns.get("emp_no"));
    }

    @Test
    public void mapsOwnerQualifiedTablesAndJoinAliases() {
        SqlInspection inspection = SqlInspector.inspect(
                "SELECT E.EMP_NO, U.USER_NM userName FROM HR.HR_EMP E JOIN APP_USER U ON U.USER_ID = E.USER_ID");

        assertEquals(new SourceColumn("HR", "HR_EMP", "EMP_NO"), inspection.sourceColumns().get("EMP_NO"));
        assertEquals(new SourceColumn(null, "APP_USER", "USER_NM"), inspection.sourceColumns().get("userName"));
    }

    @Test
    public void expressionsHaveNoDirectSourceMapping() {
        SqlInspection inspection = SqlInspector.inspect(
                "SELECT COALESCE(E.EMP_NM, U.USER_NM) AS displayName, "
                        + "CASE WHEN E.ACTIVE_YN = 'Y' THEN E.EMP_NM END AS activeName "
                        + "FROM HR_EMP E JOIN APP_USER U ON U.USER_ID = E.USER_ID");

        assertTrue(inspection.sourceColumns().isEmpty());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void sourceMappingsAreUnmodifiable() {
        SqlInspection inspection = SqlInspector.inspect("SELECT E.EMP_NO FROM HR_EMP E");
        inspection.sourceColumns().put("other", new SourceColumn(null, "T", "C"));
    }
}
