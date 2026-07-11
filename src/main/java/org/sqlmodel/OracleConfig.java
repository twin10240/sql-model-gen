package org.sqlmodel;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

final class OracleConfig {
    private static final String URL = "oracle.url";
    private static final String USERNAME = "oracle.username";
    private static final String PASSWORD = "oracle.password";
    private static final String SCHEMA = "oracle.schema";

    private final String url;
    private final String username;
    private final String password;
    private final String schema;

    private OracleConfig(String url, String username, String password, String schema) {
        this.url = url;
        this.username = username;
        this.password = password;
        this.schema = schema;
    }

    static OracleConfig loadDefault() throws IOException {
        return load(defaultPath());
    }

    static OracleConfig load(Path path) throws IOException {
        Properties properties = new Properties();
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            properties.load(reader);
        }

        List<String> missing = new ArrayList<String>();
        require(properties, URL, missing);
        require(properties, USERNAME, missing);
        require(properties, PASSWORD, missing);
        require(properties, SCHEMA, missing);
        if (!missing.isEmpty()) {
            throw new IllegalArgumentException("Missing required Oracle properties: " + missing);
        }

        return new OracleConfig(properties.getProperty(URL).trim(),
                properties.getProperty(USERNAME).trim(),
                properties.getProperty(PASSWORD),
                properties.getProperty(SCHEMA).trim());
    }

    static Path defaultPath() {
        return defaultPath(Paths.get(System.getProperty("user.home")));
    }

    static Path defaultPath(Path userHome) {
        return userHome.resolve(".modelconvertor").resolve("oracle.properties");
    }

    private static void require(Properties properties, String key, List<String> missing) {
        String value = properties.getProperty(key);
        if (value == null || value.trim().isEmpty()) {
            missing.add(key);
        }
    }

    String url() {
        return url;
    }

    String username() {
        return username;
    }

    String password() {
        return password;
    }

    String schema() {
        return schema;
    }

    @Override
    public String toString() {
        return "OracleConfig{url='" + url + "', username='" + username
                + "', password=***, schema='" + schema + "'}";
    }
}
