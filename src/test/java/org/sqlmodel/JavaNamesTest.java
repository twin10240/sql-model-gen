package org.sqlmodel;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class JavaNamesTest {
    @Test
    public void normalizesSqlLabelsToJavaNames() {
        assertEquals("empNm", JavaNames.toFieldName("EMP_NM"));
        assertEquals("employeeName", JavaNames.toFieldName("employeeName"));
        assertEquals("f_1stValue", JavaNames.toFieldName("1ST_VALUE"));
        assertEquals("class_", JavaNames.toFieldName("class"));
        assertEquals("EmpNm", JavaNames.toAccessorSuffix("EMP_NM"));
    }
}
