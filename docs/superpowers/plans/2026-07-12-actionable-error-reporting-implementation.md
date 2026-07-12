# Actionable Error Reporting Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Report the failing file or Oracle cause while guaranteeing that configured passwords are not printed.

**Architecture:** Keep error handling at the application orchestration boundaries where both the operation and its path/config are known. Narrow the injected loader interfaces to their real checked exceptions, add one password-redaction helper, and retain the existing catch-all only for unexpected failures.

**Tech Stack:** Java 8, Java NIO, JDBC, JUnit 4, Maven

---

### Task 1: Add failing regression tests for actionable errors

**Files:**
- Modify: `src/test/java/org/sqlmodel/ModelConvertorApplicationTest.java`

- [ ] **Step 1: Replace the generic secret-leak test with Oracle-specific assertions**

Add `import java.sql.SQLException;` and replace `processingFailureReturnsOneAndDoesNotLeakPassword` with:

```java
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
```

- [ ] **Step 2: Add missing config and SQL file tests**

```java
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
```

- [ ] **Step 3: Run focused tests and verify RED**

Run: `mvn -q -Dtest=ModelConvertorApplicationTest test`

Expected: three failures because current output is only `Error: Processing failed`.

### Task 2: Handle failures at their operation boundaries

**Files:**
- Modify: `src/main/java/org/sqlmodel/ModelConvertorApplication.java`
- Test: `src/test/java/org/sqlmodel/ModelConvertorApplicationTest.java`

- [ ] **Step 1: Narrow checked exception contracts**

Add imports for `java.io.IOException` and `java.sql.SQLException`, then change the injected interfaces to:

```java
interface ConfigLoader { OracleConfig load(Path path) throws IOException; }
interface ConnectionOpener { Connection open(OracleConfig config) throws SQLException; }
interface MetadataLoader { List<ColumnSpec> read(Connection connection, SqlInspection inspection,
                                                  OracleConfig config) throws SQLException; }
```

- [ ] **Step 2: Catch SQL input and config failures locally**

Replace the direct SQL/config statements with:

```java
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
```

- [ ] **Step 3: Catch and sanitize Oracle failures locally**

Replace the connection block with:

```java
List<ColumnSpec> columns;
try (Connection connection = connections.open(config)) {
    columns = metadata.read(connection, inspection, config);
} catch (SQLException e) {
    String message = detail(e);
    if (!config.password().isEmpty()) message = message.replace(config.password(), "***");
    error("Oracle processing failed (code " + e.getErrorCode() + "): " + message);
    return 1;
}
```

Add the helper:

```java
private static String detail(Exception error) {
    String message = error.getMessage();
    return message == null || message.trim().isEmpty() ? error.getClass().getSimpleName() : message;
}
```

Keep the final catch-all unchanged for unexpected exceptions.

- [ ] **Step 4: Run focused and full tests and verify GREEN**

Run: `mvn -q -Dtest=ModelConvertorApplicationTest test`

Expected: PASS.

Run: `mvn -q test`

Expected: all tests PASS.

- [ ] **Step 5: Commit code and tests**

Run: `git add -- src/main/java/org/sqlmodel/ModelConvertorApplication.java src/test/java/org/sqlmodel/ModelConvertorApplicationTest.java`

Run: `git commit -m "fix: report actionable processing errors"`

### Task 3: Record deferred interactive retry behavior

**Files:**
- Create: `docs/future-improvements.md`

- [ ] **Step 1: Add the deferred item**

```markdown
# Future Improvements

## Interactive class and package retry

Interactive mode currently exits with code 2 when the class name or package name is empty or invalid. A future UX improvement may prompt again instead.

Proposed behavior:

- Repeat the prompt for empty or invalid values.
- Show the validation reason before prompting again.
- Treat EOF as cancellation and exit instead of retrying forever.
- Keep piped mode unchanged: `--class-name` and `--package` remain required.
```

- [ ] **Step 2: Verify formatting and commit**

Run: `git diff --check`

Expected: no output.
Run: `git add -- docs/future-improvements.md`

Run: `git commit -m "docs: record interactive input retry follow-up"`

### Task 4: Final verification

**Files:**
- Verify: `pom.xml`
- Verify: `src/main/java/org/sqlmodel/ModelConvertorApplication.java`
- Verify: `src/test/java/org/sqlmodel/ModelConvertorApplicationTest.java`

- [ ] **Step 1: Run a clean package build**

Run: `mvn clean package`

Expected: BUILD SUCCESS with zero test failures.

- [ ] **Step 2: Verify executable help and repository diff**

Run: `java -jar target/modelconvertor.jar --help`

Expected: usage text and exit code 0.

Run: `git diff --check`

Expected: no output.
