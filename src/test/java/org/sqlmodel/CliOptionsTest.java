package org.sqlmodel;

import org.junit.Test;

import java.nio.file.Paths;

import static org.junit.Assert.*;

public class CliOptionsTest {
    @Test public void parsesEveryDocumentedOption() {
        CliOptions options = CliOptions.parse(new String[]{"--sql-file", "query.sql", "--class-name", "EmployeeModel",
                "--package", "com.example", "--output", "generated", "--config", "oracle.properties",
                "--overwrite", "--stdout"});
        assertEquals(Paths.get("query.sql"), options.sqlFile());
        assertEquals("EmployeeModel", options.className());
        assertEquals("com.example", options.packageName());
        assertEquals(Paths.get("generated"), options.output());
        assertEquals(Paths.get("oracle.properties"), options.config());
        assertTrue(options.overwrite());
        assertTrue(options.stdout());
    }

    @Test public void helpIsRecognized() { assertTrue(CliOptions.parse(new String[]{"--help"}).help()); }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsUnknownOption() { CliOptions.parse(new String[]{"--wat"}); }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsMissingValue() { CliOptions.parse(new String[]{"--class-name"}); }
}
