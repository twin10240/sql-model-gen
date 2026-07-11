package org.sqlmodel;

import org.junit.Test;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import static org.junit.Assert.*;

public class OracleCommentReaderTest {
    @Test public void queriesDirectColumnWithUppercaseOwnerTableAndColumn() throws Exception {
        JdbcProxyFixtures jdbc = new JdbcProxyFixtures();
        jdbc.commentResultSet = jdbc.resultSet(null, "Employee name", true);
        Map<String, SourceColumn> sources = new LinkedHashMap<String, SourceColumn>();
        sources.put("name", new SourceColumn("hr", "employees", "name"));
        assertEquals("Employee name", new OracleCommentReader(OracleMetadataReaderTest.config())
                .read(jdbc.connection(), new SqlInspection("select name", sources)).get("name"));
        assertTrue(jdbc.preparedSql.get(0).contains("ALL_COL_COMMENTS"));
        assertEquals("HR", jdbc.parameters.get(0).get(1));
        assertEquals("EMPLOYEES", jdbc.parameters.get(0).get(2));
        assertEquals("NAME", jdbc.parameters.get(0).get(3));
    }

    @Test public void usesConfiguredSchemaAndReturnsEmptyForNullOrMissingComment() throws Exception {
        JdbcProxyFixtures jdbc = new JdbcProxyFixtures();
        jdbc.commentResultSet = jdbc.resultSet(null, null, true);
        Map<String, SourceColumn> sources = Collections.singletonMap("x", new SourceColumn(null, "t", "c"));
        assertEquals("", new OracleCommentReader(OracleMetadataReaderTest.config())
                .read(jdbc.connection(), new SqlInspection("select x", sources)).get("x"));
        assertEquals("APP", jdbc.parameters.get(0).get(1));
    }

    @Test public void neverQueriesLabelsWithoutDirectSources() throws Exception {
        JdbcProxyFixtures jdbc = new JdbcProxyFixtures();
        assertTrue(new OracleCommentReader(OracleMetadataReaderTest.config()).read(jdbc.connection(),
                new SqlInspection("select 1 as x", Collections.<String, SourceColumn>emptyMap())).isEmpty());
        assertTrue(jdbc.preparedSql.isEmpty());
    }

    @Test public void returnsEmptyForNoCommentRow() throws Exception {
        JdbcProxyFixtures jdbc = new JdbcProxyFixtures();
        jdbc.commentResultSet = jdbc.resultSet(null, null, false);
        Map<String, SourceColumn> sources = Collections.singletonMap("x", new SourceColumn(null, "t", "c"));
        assertEquals("", new OracleCommentReader(OracleMetadataReaderTest.config())
                .read(jdbc.connection(), new SqlInspection("select x", sources)).get("x"));
    }
}
