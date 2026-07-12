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
import java.sql.SQLException;
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
        assertFalse(out.toString().contains("Overwrite:"));
    }

    @Test public void databaseFailureReportsCauseAndRedactsPassword() {
        StringWriter err = new StringWriter();
        ModelConvertorApplication app = new ModelConvertorApplication(
                new StringReader("SELECT 1 FROM dual"), new StringWriter(), err,
                temporary.getRoot().toPath(), path -> OracleConfig.load(configFile()),
                config -> { throw new SQLException("login failed for secret", "28000", 1017); },
                (connection, inspection, config) -> Collections.emptyList());

        assertEquals(1, app.run(new String[]{"--class-name", "DualModel", "--package", "p", "--stdout"}));
        assertTrue(err.toString().contains("Oracle processing failed (code 1017): login failed for ***"));
        assertFalse(err.toString().contains("secret"));
    }

    @Test public void missingConfigReportsConfigPath() {
        Path missing = temporary.getRoot().toPath().resolve("missing.properties").toAbsolutePath();
        StringWriter err = new StringWriter();
        ModelConvertorApplication app = new ModelConvertorApplication(
                new StringReader("SELECT 1 FROM dual"), new StringWriter(), err,
                temporary.getRoot().toPath());

        assertEquals(1, app.run(new String[]{"--class-name", "DualModel", "--package", "p",
                "--config", missing.toString(), "--stdout"}));
        assertTrue(err.toString().contains("Oracle config failed: " + missing));
    }

    @Test public void missingSqlFileReportsSqlPath() {
        Path missing = temporary.getRoot().toPath().resolve("missing.sql").toAbsolutePath();
        StringWriter err = new StringWriter();
        ModelConvertorApplication app = new ModelConvertorApplication(
                new StringReader(""), new StringWriter(), err, temporary.getRoot().toPath());

        assertEquals(1, app.run(new String[]{"--sql-file", missing.toString(), "--class-name", "DualModel",
                "--package", "p", "--stdout"}));
        assertTrue(err.toString().contains("SQL input failed: " + missing));
    }

    @Test public void interactiveModePreviewsPathAndOverwriteStatusBeforeWriting() throws Exception {
        Path expected = temporary.getRoot().toPath().resolve("src/main/java/com/example/DualModel.java").toAbsolutePath();
        Files.createDirectories(expected.getParent());
        Files.write(expected, "existing".getBytes(StandardCharsets.UTF_8));
        StringWriter out = new StringWriter();
        StringWriter err = new StringWriter();
        ModelConvertorApplication app = new ModelConvertorApplication(
                new StringReader("DualModel\ncom.example\nSELECT d.DUMMY FROM dual d\n:end\n"), out,
                err, temporary.getRoot().toPath(), true, path -> OracleConfig.load(configFile()),
                config -> null, (connection, inspection, config) -> Collections.emptyList());

        assertEquals(1, app.run(new String[0]));
        assertTrue(out.toString().contains(expected.toString()));
        assertTrue(out.toString().contains("Overwrite: disabled"));
        assertEquals("existing", new String(Files.readAllBytes(expected), StandardCharsets.UTF_8));
        assertTrue(err.toString().contains(expected.toString()));
        assertTrue(err.toString().contains("Use --overwrite"));
    }

    @Test public void terminalPartialOptionsUseEndMarker() {
        final String[] sql = {null};
        ModelConvertorApplication app = new ModelConvertorApplication(
                new StringReader("DualModel\ncom.example\nSELECT 1 FROM dual\n:end\nignored"), new StringWriter(),
                new StringWriter(), temporary.getRoot().toPath(), true, path -> OracleConfig.load(configFile()),
                config -> null, (connection, inspection, config) -> { sql[0] = inspection.normalizedSql(); return Collections.emptyList(); });

        assertEquals(0, app.run(new String[]{"--overwrite", "--stdout"}));
        assertEquals("SELECT 1 FROM dual", sql[0]);
    }

    @Test public void pipedInputReadsToEofEvenWithPartialOptions() {
        final String[] sql = {null};
        ModelConvertorApplication app = new ModelConvertorApplication(
                new StringReader("SELECT 1 FROM dual\n:end\n"), new StringWriter(), new StringWriter(),
                temporary.getRoot().toPath(), false, path -> OracleConfig.load(configFile()), config -> null,
                (connection, inspection, config) -> { sql[0] = inspection.normalizedSql(); return Collections.emptyList(); });

        assertEquals(0, app.run(new String[]{"--class-name", "DualModel", "--package", "p", "--stdout"}));
        assertTrue(sql[0].endsWith(":end"));
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
