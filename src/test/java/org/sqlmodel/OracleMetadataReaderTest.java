package org.sqlmodel;

import org.junit.Test;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Types;
import java.util.Collections;
import java.util.List;
import static org.junit.Assert.*;

public class OracleMetadataReaderTest {
    @Test public void readsPreparedStatementMetadataFirstAndPreservesColumns() throws Exception {
        JdbcProxyFixtures jdbc = new JdbcProxyFixtures();
        jdbc.directMetadata = JdbcProxyFixtures.metadata(new String[] {"EMP_ID", "NAME"},
                new int[] {Types.NUMERIC, Types.VARCHAR}, new int[] {9, 20}, new int[] {0, 0},
                new String[] {"NUMBER", "VARCHAR2"});
        List<ColumnSpec> columns = new OracleMetadataReader(config()).read(jdbc.connection(),
                new SqlInspection("SELECT emp_id, name FROM emp", Collections.<String, SourceColumn>emptyMap()));
        assertEquals(Collections.singletonList("SELECT emp_id, name FROM emp"), jdbc.preparedSql);
        assertEquals(0, jdbc.executeQueryCalls);
        assertEquals(2, columns.size());
        assertEquals(1, columns.get(0).ordinal()); assertEquals("EMP_ID", columns.get(0).resultLabel());
        assertEquals("empId", columns.get(0).fieldName()); assertEquals("Integer", columns.get(0).javaType());
        assertEquals(2, columns.get(1).ordinal()); assertEquals("String", columns.get(1).javaType());
    }

    @Test public void nullMetadataUsesGuaranteedZeroRowFallbackWithoutFetchingRows() throws Exception {
        JdbcProxyFixtures jdbc = new JdbcProxyFixtures();
        jdbc.fallbackResultSet = jdbc.resultSet(JdbcProxyFixtures.metadata(new String[] {"VALUE"},
                new int[] {Types.NUMERIC}, new int[] {18}, new int[] {0}, new String[] {"NUMBER"}), null, false);
        new OracleMetadataReader(config()).read(jdbc.connection(),
                new SqlInspection("SELECT value FROM t", Collections.<String, SourceColumn>emptyMap()));
        assertEquals("SELECT * FROM (SELECT value FROM t) WHERE 1 = 0", jdbc.preparedSql.get(1));
        assertEquals(1, jdbc.executeQueryCalls); assertEquals(0, jdbc.setMaxRowsCalls);
        assertEquals(0, jdbc.resultSetNextCalls);
    }

    @Test public void rejectsDuplicateLabelsWithAliasGuidance() throws Exception {
        assertDuplicate(new String[] {"ID", "id"});
    }

    @Test public void rejectsDuplicateNormalizedFieldNamesWithAliasGuidance() throws Exception {
        assertDuplicate(new String[] {"EMP_ID", "emp-id"});
    }

    @Test public void attachesDirectColumnCommentCaseInsensitively() throws Exception {
        JdbcProxyFixtures jdbc = new JdbcProxyFixtures();
        jdbc.directMetadata = JdbcProxyFixtures.metadata(new String[] {"NAME"}, new int[] {Types.VARCHAR},
                new int[] {20}, new int[] {0}, new String[] {"VARCHAR2"});
        jdbc.commentResultSet = jdbc.resultSet(null, "Employee name", true);
        java.util.Map<String, SourceColumn> sources = Collections.singletonMap("name",
                new SourceColumn(null, "employees", "name"));
        List<ColumnSpec> columns = new OracleMetadataReader(config()).read(jdbc.connection(),
                new SqlInspection("SELECT name FROM employees", sources));
        assertEquals("Employee name", columns.get(0).description());
    }

    private void assertDuplicate(String[] labels) throws Exception {
        JdbcProxyFixtures jdbc = new JdbcProxyFixtures();
        jdbc.directMetadata = JdbcProxyFixtures.metadata(labels, new int[] {Types.VARCHAR, Types.VARCHAR},
                new int[] {0, 0}, new int[] {0, 0}, new String[] {"VARCHAR2", "VARCHAR2"});
        try {
            new OracleMetadataReader(config()).read(jdbc.connection(),
                    new SqlInspection("SELECT x FROM t", Collections.<String, SourceColumn>emptyMap()));
            fail("Expected duplicate error");
        } catch (IllegalArgumentException expected) { assertTrue(expected.getMessage().contains("AS")); }
    }

    static OracleConfig config() throws Exception {
        Path file = Files.createTempFile("oracle", ".properties");
        Files.write(file, ("oracle.url=x\noracle.username=u\noracle.password=p\noracle.schema=app\n").getBytes("UTF-8"));
        return OracleConfig.load(file);
    }
}
