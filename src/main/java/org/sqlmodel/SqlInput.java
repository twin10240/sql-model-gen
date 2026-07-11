package org.sqlmodel;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

final class SqlInput {
    private final BufferedReader input;

    SqlInput(Reader input) { this.input = input instanceof BufferedReader ? (BufferedReader) input : new BufferedReader(input); }

    String read(Path sqlFile, boolean interactive) throws IOException {
        if (sqlFile != null) {
            byte[] bytes = Files.readAllBytes(sqlFile);
            return new String(bytes, StandardCharsets.UTF_8);
        }
        StringBuilder sql = new StringBuilder();
        String line;
        while ((line = input.readLine()) != null) {
            if (interactive && ":end".equals(line)) break;
            sql.append(line).append('\n');
        }
        return sql.toString();
    }

    String prompt(String label, java.io.Writer output) throws IOException {
        output.write(label);
        output.flush();
        String value = input.readLine();
        if (value == null || value.trim().isEmpty()) throw new IllegalArgumentException("Required input is missing");
        return value.trim();
    }
}
