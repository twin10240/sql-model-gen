package org.sqlmodel;

import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;

public final class Main {
    private Main() {}

    public static void main(String[] args) {
        System.exit(run(args));
    }

    static int run(String[] args) {
        return new ModelConvertorApplication(new InputStreamReader(System.in, StandardCharsets.UTF_8),
                new OutputStreamWriter(System.out, StandardCharsets.UTF_8),
                new OutputStreamWriter(System.err, StandardCharsets.UTF_8), Paths.get("").toAbsolutePath()).run(args);
    }
}
