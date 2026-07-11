package org.sqlmodel;

import org.junit.Test;

import java.sql.Types;

import static org.junit.Assert.assertEquals;

public class JdbcTypeMapperTest {
    @Test
    public void mapsJdbcMetadataToJavaTypes() {
        assertEquals("String", JdbcTypeMapper.map(Types.VARCHAR, 0, 0, "VARCHAR2"));
        assertEquals("Integer", JdbcTypeMapper.map(Types.NUMERIC, 9, 0, "NUMBER"));
        assertEquals("Long", JdbcTypeMapper.map(Types.NUMERIC, 18, 0, "NUMBER"));
        assertEquals("BigDecimal", JdbcTypeMapper.map(Types.NUMERIC, 19, 0, "NUMBER"));
        assertEquals("BigDecimal", JdbcTypeMapper.map(Types.NUMERIC, 9, 2, "NUMBER"));
        assertEquals("Double", JdbcTypeMapper.map(Types.FLOAT, 0, 0, "BINARY_FLOAT"));
        assertEquals("LocalDateTime", JdbcTypeMapper.map(Types.DATE, 0, 0, "DATE"));
        assertEquals("LocalDateTime", JdbcTypeMapper.map(Types.TIMESTAMP, 0, 0, "TIMESTAMP"));
        assertEquals("LocalDateTime", JdbcTypeMapper.map(Types.TIMESTAMP_WITH_TIMEZONE, 0, 0, "TIMESTAMP WITH TIME ZONE"));
        assertEquals("LocalDateTime", JdbcTypeMapper.map(Types.TIMESTAMP, 0, 0, "TIMESTAMP WITH LOCAL TIME ZONE"));
        assertEquals("String", JdbcTypeMapper.map(Types.OTHER, 0, 0, "UNKNOWN"));
    }
}
