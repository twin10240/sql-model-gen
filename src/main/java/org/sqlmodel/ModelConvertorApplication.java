package org.sqlmodel;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;

final class ModelConvertorApplication {
    interface ConfigLoader { OracleConfig load(Path path) throws IOException; }
    interface ConnectionOpener { Connection open(OracleConfig config) throws SQLException; }
    interface MetadataLoader { List<ColumnSpec> read(Connection connection, SqlInspection inspection,
                                                      OracleConfig config) throws SQLException; }

    private final SqlInput input;
    private final Writer out;
    private final Writer err;
    private final Path cwd;
    private final boolean terminalInput;
    private final ConfigLoader configs;
    private final ConnectionOpener connections;
    private final MetadataLoader metadata;

    ModelConvertorApplication(Reader input, Writer out, Writer err, Path cwd) {
        this(input, out, err, cwd, false);
    }

    ModelConvertorApplication(Reader input, Writer out, Writer err, Path cwd, boolean terminalInput) {
        this(input, out, err, cwd, terminalInput,
                path -> path == null ? OracleConfig.loadDefault() : OracleConfig.load(path),
                config -> DriverManager.getConnection(config.url(), config.username(), config.password()),
                (connection, inspection, config) -> new OracleMetadataReader(config).read(connection, inspection));
    }

    ModelConvertorApplication(Reader input, Writer out, Writer err, Path cwd, ConfigLoader configs,
                              ConnectionOpener connections, MetadataLoader metadata) {
        this(input, out, err, cwd, false, configs, connections, metadata);
    }

    ModelConvertorApplication(Reader input, Writer out, Writer err, Path cwd, boolean terminalInput,
                              ConfigLoader configs, ConnectionOpener connections, MetadataLoader metadata) {
        this.input = new SqlInput(input);
        this.out = out;
        this.err = err;
        this.cwd = cwd;
        this.terminalInput = terminalInput;
        this.configs = configs;
        this.connections = connections;
        this.metadata = metadata;
    }

    int run(String[] args) {
        try {
            CliOptions options = CliOptions.parse(args);
            if (options.help()) { out.write(usage()); out.flush(); return 0; }
            if (!terminalInput && (options.className() == null || options.packageName() == null)) {
                throw new IllegalArgumentException("--class-name and --package are required for piped input");
            }
            String className = options.className() == null ? input.prompt("Class name: ", out) : options.className();
            String packageName = options.packageName() == null ? input.prompt("Package name: ", out) : options.packageName();
            new DzModelRenderer().render(packageName, className, java.util.Collections.<ColumnSpec>emptyList());
            boolean interactiveSql = options.sqlFile() == null && terminalInput;
            if (interactiveSql) { out.write("Paste SQL; enter :end on its own line to finish.\n"); out.flush(); }
            SqlInspection inspection;
            try {
                inspection = SqlInspector.inspect(input.read(options.sqlFile(), interactiveSql));
            } catch (IOException e) {
                Path path = options.sqlFile();
                error("SQL input failed: " + (path == null ? "standard input" : path.toAbsolutePath())
                        + ": " + detail(e));
                return 1;
            }
            Path configPath = options.config() == null ? OracleConfig.defaultPath() : options.config().toAbsolutePath();
            OracleConfig config;
            try {
                config = configs.load(options.config());
            } catch (IOException e) {
                error("Oracle config failed: " + configPath + ": " + detail(e));
                return 1;
            }
            List<ColumnSpec> columns;
            try (Connection connection = connections.open(config)) {
                columns = metadata.read(connection, inspection, config);
            } catch (SQLException e) {
                String message = detail(e);
                if (!config.password().isEmpty()) message = message.replace(config.password(), "***");
                error("Oracle processing failed (code " + e.getErrorCode() + "): " + message);
                return 1;
            }
            String source = new DzModelRenderer().render(packageName, className, columns);
            SourceOutput output = new SourceOutput(cwd, out);
            if (terminalInput && !options.stdout()) {
                out.write("Output: " + output.path(packageName, className, options.output()) + System.lineSeparator());
                out.write("Overwrite: " + (options.overwrite() ? "enabled" : "disabled") + System.lineSeparator());
                out.flush();
            }
            Path path = output.write(source, packageName, className, options.output(), options.overwrite(), options.stdout());
            if (path != null) { out.write("Created " + path + System.lineSeparator()); out.flush(); }
            return 0;
        } catch (FileAlreadyExistsException e) {
            error("Output file already exists: " + e.getFile() + ". Use --overwrite to replace it");
            return 1;
        } catch (IllegalArgumentException e) {
            error(e.getMessage());
            return 2;
        } catch (Exception e) {
            error("Processing failed");
            return 1;
        }
    }

    private void error(String message) {
        try { err.write("Error: " + message + System.lineSeparator()); err.flush(); } catch (Exception ignored) { }
    }

    private static String detail(Exception error) {
        String message = error.getMessage();
        return message == null || message.trim().isEmpty() ? error.getClass().getSimpleName() : message;
    }

    static String usage() {
        return "Usage: modelconvertor [--sql-file <path>] [--class-name <name>] [--package <name>] "
                + "[--output <source-root>] [--config <path>] [--overwrite] [--stdout] [--help]" + System.lineSeparator();
    }
}
