package org.sqlmodel;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.util.Collections;

import static org.junit.Assert.*;

public class ModelConvertorApplicationTest {
    @Rule public TemporaryFolder temporary = new TemporaryFolder();

    @Test public void validatesNamesBeforeOpeningDatabase() {
        final boolean[] opened = {false};
        ModelConvertorApplication app = application(configFile(), config -> { opened[0] = true; return null; }, new StringReader("SELECT 1 FROM dual"));
        assertEquals(2, app.run(new String[]{"--class-name", "bad-name", "--package", "p", "--stdout"}));
        assertFalse(opened[0]);
    }

    @Test public void successfulFlowReportsAbsoluteOutputPath() throws Exception {
        Path root = temporary.newFolder("generated").toPath();
        StringWriter out = new StringWriter();
        ModelConvertorApplication app = new ModelConvertorApplication(new StringReader("SELECT d.DUMMY FROM dual d"), out,
                new StringWriter(), temporary.getRoot().toPath(), path -> OracleConfig.load(configFile()),
                config -> null,
                (connection, inspection, config) -> Collections.singletonList(new ColumnSpec(1, "DUMMY", "dummy", "String", "")));
        assertEquals(0, app.run(new String[]{"--class-name", "DualModel", "--package", "com.example", "--output", root.toString()}));
        Path expected = root.resolve("com/example/DualModel.java").toAbsolutePath();
        assertTrue(Files.exists(expected));
        assertTrue(out.toString().contains(expected.toString()));
    }

    @Test public void processingFailureReturnsOneAndDoesNotLeakPassword() {
        StringWriter err = new StringWriter();
        ModelConvertorApplication app = new ModelConvertorApplication(new StringReader("SELECT 1 FROM dual"), new StringWriter(), err,
                temporary.getRoot().toPath(), path -> OracleConfig.load(configFile()), config -> { throw new Exception("oracle.password=secret"); },
                (connection, inspection, config) -> Collections.emptyList());
        assertEquals(1, app.run(new String[]{"--class-name", "DualModel", "--package", "p", "--stdout"}));
        assertFalse(err.toString().contains("secret"));
        assertFalse(err.toString().contains("oracle.password"));
    }

    private ModelConvertorApplication application(Path config, ModelConvertorApplication.ConnectionOpener opener, StringReader input) {
        return new ModelConvertorApplication(input, new StringWriter(), new StringWriter(), temporary.getRoot().toPath(),
                ignored -> OracleConfig.load(config), opener, (connection, inspection, loaded) -> Collections.emptyList());
    }

    private Path configFile() {
        try {
            Path file = temporary.newFile().toPath();
            Files.write(file, ("oracle.url=jdbc:test\noracle.username=user\noracle.password=secret\noracle.schema=APP\n").getBytes(StandardCharsets.UTF_8));
            return file;
        } catch (Exception e) { throw new AssertionError(e); }
    }
}
