package org.sqlmodel;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

final class SourceOutput {
    private final Path cwd;
    private final Writer stdout;

    SourceOutput(Path cwd, Writer stdout) { this.cwd = cwd; this.stdout = stdout; }

    Path write(String source, String packageName, String className, Path sourceRoot,
               boolean overwrite, boolean toStdout) throws IOException {
        if (toStdout) {
            stdout.write(source);
            stdout.flush();
            return null;
        }
        Path root = sourceRoot == null ? cwd.resolve("src/main/java") : sourceRoot;
        Path directory = root.resolve(packageName.replace('.', '/'));
        Files.createDirectories(directory);
        Path file = directory.resolve(className + ".java").toAbsolutePath();
        try (Writer writer = overwrite
                ? Files.newBufferedWriter(file, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)
                : Files.newBufferedWriter(file, StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW)) {
            writer.write(source);
        }
        return file;
    }
}
