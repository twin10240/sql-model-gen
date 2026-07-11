package org.sqlmodel;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ColumnSpecTest {
    @Test
    public void exposesConstructorValues() {
        ColumnSpec column = new ColumnSpec(1, "EMP_NM", "empNm", "String", "Employee name");

        assertEquals(1, column.ordinal());
        assertEquals("EMP_NM", column.resultLabel());
        assertEquals("empNm", column.fieldName());
        assertEquals("String", column.javaType());
        assertEquals("Employee name", column.description());
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsBlankResultLabel() {
        new ColumnSpec(1, " ", "empNm", "String", "");
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsBlankFieldName() {
        new ColumnSpec(1, "EMP_NM", " ", "String", "");
    }
}
