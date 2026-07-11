package org.sqlmodel;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.assertEquals;

public class SqlInputTest {
    @Rule public TemporaryFolder temporary = new TemporaryFolder();

    @Test public void sqlFileTakesPrecedenceOverStdin() throws Exception {
        Path file = temporary.newFile("query.sql").toPath();
        Files.write(file, "SELECT '한글' FROM dual".getBytes(StandardCharsets.UTF_8));
        assertEquals("SELECT '한글' FROM dual", new SqlInput(new StringReader("stdin")).read(file, false));
    }

    @Test public void readsPipedStandardInputToEndOfFile() throws Exception {
        assertEquals("SELECT 1\nFROM dual\n", new SqlInput(new StringReader("SELECT 1\nFROM dual\n")).read(null, false));
    }

    @Test public void interactiveInputStopsAtEndMarker() throws Exception {
        assertEquals("SELECT 1\nFROM dual\n", new SqlInput(new StringReader("SELECT 1\nFROM dual\n:end\nignored")).read(null, true));
    }
}
