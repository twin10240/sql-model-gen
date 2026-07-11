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

    Path path(String packageName, String className, Path sourceRoot) {
        Path root = sourceRoot == null ? cwd.resolve("src/main/java") : sourceRoot;
        if (!root.isAbsolute()) root = cwd.resolve(root);
        return root.resolve(packageName.replace('.', '/')).resolve(className + ".java").toAbsolutePath();
    }

    Path write(String source, String packageName, String className, Path sourceRoot,
               boolean overwrite, boolean toStdout) throws IOException {
        if (toStdout) {
            stdout.write(source);
            stdout.flush();
            return null;
        }
        Path file = path(packageName, className, sourceRoot);
        Path directory = file.getParent();
        Files.createDirectories(directory);
        try (Writer writer = overwrite
                ? Files.newBufferedWriter(file, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)
                : Files.newBufferedWriter(file, StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW)) {
            writer.write(source);
        }
        return file;
    }
}
