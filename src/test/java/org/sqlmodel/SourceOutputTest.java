package org.sqlmodel;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.*;

public class SourceOutputTest {
    @Rule public TemporaryFolder temporary = new TemporaryFolder();

    @Test public void writesUtf8ToDefaultPackagePathAndCreatesDirectories() throws Exception {
        Path cwd = temporary.getRoot().toPath();
        Path result = new SourceOutput(cwd, new StringWriter()).write("한글 설명", "com.example.model", "Employee", null, false, false);
        assertEquals(cwd.resolve("src/main/java/com/example/model/Employee.java").toAbsolutePath(), result);
        assertEquals("한글 설명", new String(Files.readAllBytes(result), StandardCharsets.UTF_8));
    }

    @Test(expected = FileAlreadyExistsException.class)
    public void rejectsExistingFileWithoutOverwrite() throws Exception {
        Path root = temporary.newFolder("source").toPath();
        SourceOutput output = new SourceOutput(temporary.getRoot().toPath(), new StringWriter());
        output.write("old", "p", "Model", root, false, false);
        output.write("new", "p", "Model", root, false, false);
    }

    @Test public void overwriteTruncatesInsteadOfAppending() throws Exception {
        Path root = temporary.newFolder("overwrite").toPath();
        SourceOutput output = new SourceOutput(temporary.getRoot().toPath(), new StringWriter());
        Path file = output.write("long old contents", "p", "Model", root, false, false);
        output.write("new", "p", "Model", root, true, false);
        assertEquals("new", new String(Files.readAllBytes(file), StandardCharsets.UTF_8));
    }

    @Test public void stdoutWritesNoFile() throws Exception {
        StringWriter stdout = new StringWriter();
        Path result = new SourceOutput(temporary.getRoot().toPath(), stdout).write("source", "p", "Model", null, false, true);
        assertNull(result);
        assertEquals("source", stdout.toString());
        assertEquals(0, temporary.getRoot().list().length);
    }
}
