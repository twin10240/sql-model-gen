# ModelConvertor CLI and Oracle Metadata Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a Java 8-compatible personal CLI that converts Oracle SELECT result metadata into DZ model source files without fetching result rows.

**Architecture:** Keep SQL inspection, JDBC metadata, model rendering, CLI input, and file output as small package-private units under `org.sqlmodel`. `ModelConvertorApplication` coordinates them; pure naming, parsing, mapping, and rendering code is tested without Oracle, while JDBC behavior is tested with small proxy fakes and one optional real-Oracle integration path.

**Tech Stack:** Java 8, Maven 3, JUnit 4.13.2, JDBC (`java.sql`), Oracle `ojdbc8.jar` supplied only at runtime, Windows CMD.

---

## File map

### Build and entry point

- Modify `pom.xml`: Java 8, UTF-8, JUnit, compiler, test and executable-JAR configuration.
- Create `src/main/java/org/sqlmodel/Main.java`: process entry point and exit-code boundary.
- Create `modelconvertor.cmd`: locates the application JAR and external `ojdbc8.jar`.

### Core model and rendering

- Create `src/main/java/org/sqlmodel/ColumnSpec.java`: immutable generated-column contract.
- Create `src/main/java/org/sqlmodel/JavaNames.java`: SQL label to valid Java identifier conversion.
- Create `src/main/java/org/sqlmodel/JdbcTypeMapper.java`: JDBC metadata to Java type mapping.
- Create `src/main/java/org/sqlmodel/DzModelRenderer.java`: deterministic DZ Java source rendering.

### SQL and Oracle metadata

- Create `src/main/java/org/sqlmodel/SqlInspector.java`: comment/semicolon cleanup, SELECT/WITH validation, select-item and direct-column analysis.
- Create `src/main/java/org/sqlmodel/SqlInspection.java`: immutable inspected SQL and direct-column mappings.
- Create `src/main/java/org/sqlmodel/SourceColumn.java`: owner/table/column reference for comment lookup.
- Create `src/main/java/org/sqlmodel/OracleConfig.java`: external properties loading and validation.
- Create `src/main/java/org/sqlmodel/OracleMetadataReader.java`: `PreparedStatement.getMetaData()` first, zero-row fallback second, duplicate checks.
- Create `src/main/java/org/sqlmodel/OracleCommentReader.java`: `ALL_COL_COMMENTS` lookup for unambiguous direct columns.

### CLI and output

- Create `src/main/java/org/sqlmodel/CliOptions.java`: minimal option parser.
- Create `src/main/java/org/sqlmodel/SqlInput.java`: file, stdin, and `:end` interactive input.
- Create `src/main/java/org/sqlmodel/SourceOutput.java`: stdout or safe UTF-8 file output.
- Create `src/main/java/org/sqlmodel/ModelConvertorApplication.java`: orchestration and user-facing errors.

### Replacement and tests

- Delete `src/main/java/org/sqlmodel/FileCreator.java`.
- Delete `src/main/java/org/sqlmodel/SqlContents.java`.
- Delete `src/main/java/org/sqlmodel/SqlToModelTest.java`.
- Replace legacy generator API only after the new application is covered; remove `GenerationOptions.java`, `ModelGenerator.java`, `SimpleModelGeneratorImpl.java`, and `SqlModel.java` in the final cleanup task.
- Create focused tests under `src/test/java/org/sqlmodel/` matching each unit.

## Agent ownership and communication contract

- Foundation implementer owns Tasks 1-2 and publishes the `ColumnSpec`, naming, and type-mapping signatures.
- SQL implementer owns Task 3 only.
- Oracle implementer owns Tasks 4-5 only and consumes the published SQL contracts.
- CLI/rendering implementer owns Tasks 6-7 only and consumes `ColumnSpec`.
- No two implementers edit the same production file concurrently.
- Contract changes are sent to the other active agents before editing; the root agent approves cross-boundary changes.
- Every implementation task gets a fresh spec-compliance review followed by a code-quality review.

### Task 1: Establish Java 8 Maven test and executable-JAR baseline

**Files:**
- Modify: `pom.xml`
- Create: `src/main/java/org/sqlmodel/Main.java`
- Create: `src/test/java/org/sqlmodel/MainTest.java`

- [ ] **Step 1: Write the failing entry-point test**

```java
package org.sqlmodel;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class MainTest {
    @Test
    public void helpReturnsSuccess() {
        assertEquals(0, Main.run(new String[]{"--help"}));
    }
}
```

- [ ] **Step 2: Configure Maven and verify the test fails**

Add JUnit 4.13.2 with test scope. Configure `maven-compiler-plugin` for source/target 8 and UTF-8, `maven-surefire-plugin`, and `maven-jar-plugin` with `org.sqlmodel.Main` as `Main-Class`.

Run: `mvn test -Dtest=MainTest`

Expected: FAIL because `Main` does not exist.

- [ ] **Step 3: Add the smallest entry-point boundary**

```java
package org.sqlmodel;

public final class Main {
    private Main() {}

    public static void main(String[] args) {
        System.exit(run(args));
    }

    static int run(String[] args) {
        if (args.length == 1 && "--help".equals(args[0])) {
            System.out.println("Usage: modelconvertor [options]");
            return 0;
        }
        return 2;
    }
}
```

- [ ] **Step 4: Run the focused and full baseline**

Run: `mvn test -Dtest=MainTest`

Expected: PASS, 1 test.

Run: `mvn test`

Expected: BUILD SUCCESS.

- [ ] **Step 5: Commit**

```bash
git add pom.xml src/main/java/org/sqlmodel/Main.java src/test/java/org/sqlmodel/MainTest.java
git commit -m "build: establish Java 8 CLI test baseline"
```

### Task 2: Define column naming and JDBC type contracts

**Files:**
- Create: `src/main/java/org/sqlmodel/ColumnSpec.java`
- Create: `src/main/java/org/sqlmodel/JavaNames.java`
- Create: `src/main/java/org/sqlmodel/JdbcTypeMapper.java`
- Create: `src/test/java/org/sqlmodel/JavaNamesTest.java`
- Create: `src/test/java/org/sqlmodel/JdbcTypeMapperTest.java`

- [ ] **Step 1: Write naming tests**

Cover these exact cases:

```java
assertEquals("empNm", JavaNames.toFieldName("EMP_NM"));
assertEquals("employeeName", JavaNames.toFieldName("employeeName"));
assertEquals("f_1stValue", JavaNames.toFieldName("1ST_VALUE"));
assertEquals("class_", JavaNames.toFieldName("class"));
assertEquals("EmpNm", JavaNames.toAccessorSuffix("EMP_NM"));
```

- [ ] **Step 2: Write type tests using a metadata value object**

The mapper signature is fixed as:

```java
static String map(int jdbcType, int precision, int scale, String databaseTypeName)
```

Test `VARCHAR` to `String`, `NUMBER(9,0)` to `Integer`, `NUMBER(18,0)` to `Long`, larger or scaled NUMBER to `BigDecimal`, floating types to `Double`, and DATE/TIMESTAMP variants to `LocalDateTime`.

- [ ] **Step 3: Run tests to verify failure**

Run: `mvn test -Dtest=JavaNamesTest,JdbcTypeMapperTest`

Expected: FAIL because production classes do not exist.

- [ ] **Step 4: Implement immutable `ColumnSpec`**

Required constructor fields and accessors:

```java
ColumnSpec(int ordinal, String resultLabel, String fieldName,
           String javaType, String description)
```

Reject blank result labels and field names with `IllegalArgumentException`.

- [ ] **Step 5: Implement naming and type mapping minimally**

Use `java.sql.Types`, `Locale.ROOT`, and a fixed Java-keyword set. Preserve SQL labels outside this class; only Java identifiers are normalized.

- [ ] **Step 6: Run focused tests and commit**

Run: `mvn test -Dtest=JavaNamesTest,JdbcTypeMapperTest`

Expected: PASS.

```bash
git add src/main/java/org/sqlmodel/ColumnSpec.java src/main/java/org/sqlmodel/JavaNames.java src/main/java/org/sqlmodel/JdbcTypeMapper.java src/test/java/org/sqlmodel/JavaNamesTest.java src/test/java/org/sqlmodel/JdbcTypeMapperTest.java
git commit -m "feat: add column naming and JDBC type mapping"
```

### Task 3: Inspect SELECT structure and direct source columns

**Files:**
- Create: `src/main/java/org/sqlmodel/SourceColumn.java`
- Create: `src/main/java/org/sqlmodel/SqlInspection.java`
- Create: `src/main/java/org/sqlmodel/SqlInspector.java`
- Create: `src/test/java/org/sqlmodel/SqlInspectorTest.java`

- [ ] **Step 1: Write validation tests**

Verify:

- leading block and line comments are removed;
- one trailing semicolon is removed;
- SELECT and WITH queries are accepted;
- UPDATE and multiple statements are rejected;
- commas inside functions and string literals do not split select items.

- [ ] **Step 2: Write direct-column tests**

```sql
SELECT E.EMP_NM AS employeeName, E.EMP_NO
FROM HR_EMP E
```

Expected label-to-source mappings:

```text
employeeName -> HR_EMP.EMP_NM
EMP_NO       -> HR_EMP.EMP_NO
```

`COALESCE(E.EMP_NM, U.USER_NM) AS displayName` and CASE expressions must have no source mapping.

- [ ] **Step 3: Run tests to verify failure**

Run: `mvn test -Dtest=SqlInspectorTest`

Expected: FAIL because inspector classes do not exist.

- [ ] **Step 4: Implement immutable contracts**

`SqlInspection` exposes normalized SQL and an unmodifiable `Map<String, SourceColumn>` keyed case-insensitively by result label. `SourceColumn` exposes optional owner, table, and column names.

- [ ] **Step 5: Implement only the required parser**

Reuse the current depth/string-aware select-list splitting logic, but add escaped single-quote handling. Resolve simple `FROM table alias` and `JOIN table alias` declarations. Only map expressions matching `alias.column [AS] label` or `alias.column label`; do not infer function or arithmetic sources.

- [ ] **Step 6: Run tests and commit**

Run: `mvn test -Dtest=SqlInspectorTest`

Expected: PASS.

```bash
git add src/main/java/org/sqlmodel/SourceColumn.java src/main/java/org/sqlmodel/SqlInspection.java src/main/java/org/sqlmodel/SqlInspector.java src/test/java/org/sqlmodel/SqlInspectorTest.java
git commit -m "feat: inspect SQL result labels and source columns"
```

### Task 4: Load external Oracle configuration

**Files:**
- Create: `src/main/java/org/sqlmodel/OracleConfig.java`
- Create: `src/test/java/org/sqlmodel/OracleConfigTest.java`

- [ ] **Step 1: Write configuration tests**

Use JUnit `TemporaryFolder` to verify UTF-8 properties loading, default path resolution under `${user.home}/.modelconvertor/oracle.properties`, missing-key messages, and that `toString()` never contains the password.

- [ ] **Step 2: Run test to verify failure**

Run: `mvn test -Dtest=OracleConfigTest`

Expected: FAIL because `OracleConfig` does not exist.

- [ ] **Step 3: Implement config loading**

Required properties are `oracle.url`, `oracle.username`, `oracle.password`, and `oracle.schema`. Load with `Files.newBufferedReader(path, StandardCharsets.UTF_8)`. Expose getters and a redacted `toString()`.

- [ ] **Step 4: Run test and commit**

Run: `mvn test -Dtest=OracleConfigTest`

Expected: PASS.

```bash
git add src/main/java/org/sqlmodel/OracleConfig.java src/test/java/org/sqlmodel/OracleConfigTest.java
git commit -m "feat: load external Oracle configuration"
```

### Task 5: Read Oracle result metadata and column comments

**Files:**
- Create: `src/main/java/org/sqlmodel/OracleMetadataReader.java`
- Create: `src/main/java/org/sqlmodel/OracleCommentReader.java`
- Create: `src/test/java/org/sqlmodel/OracleMetadataReaderTest.java`
- Create: `src/test/java/org/sqlmodel/OracleCommentReaderTest.java`
- Create: `src/test/java/org/sqlmodel/JdbcProxyFixtures.java`

- [ ] **Step 1: Write proxy-backed metadata tests**

Use `java.lang.reflect.Proxy` fixtures for `Connection`, `PreparedStatement`, and `ResultSetMetaData`. Verify:

- `prepareStatement(normalizedSql).getMetaData()` is attempted first;
- null metadata triggers `SELECT * FROM (<sql>) WHERE 1 = 0` and `executeQuery()`;
- `setMaxRows(0)` is not used as a substitute for zero-row SQL;
- duplicate result labels throw an error asking for AS aliases;
- normalized Java field-name duplicates also fail;
- returned order, label, precision, scale and mapped Java type are preserved.

- [ ] **Step 2: Write comment-reader tests**

Verify the query uses owner, table name, and column name parameters against `ALL_COL_COMMENTS`, returns an empty string for no row/null comment, and never queries for labels without a direct `SourceColumn`.

- [ ] **Step 3: Run tests to verify failure**

Run: `mvn test -Dtest=OracleMetadataReaderTest,OracleCommentReaderTest`

Expected: FAIL because readers do not exist.

- [ ] **Step 4: Implement metadata-first/fallback behavior**

`OracleMetadataReader.read(Connection, SqlInspection)` returns ordered `List<ColumnSpec>`. Always close statements/result sets with try-with-resources. Do not fetch application rows; the fallback query is guaranteed false.

- [ ] **Step 5: Implement comment lookup**

Use:

```sql
SELECT COMMENTS
FROM ALL_COL_COMMENTS
WHERE OWNER = ? AND TABLE_NAME = ? AND COLUMN_NAME = ?
```

Normalize Oracle identifiers with `Locale.ROOT`. Only use configured schema when `SourceColumn.owner` is absent.

- [ ] **Step 6: Run tests and commit**

Run: `mvn test -Dtest=OracleMetadataReaderTest,OracleCommentReaderTest`

Expected: PASS.

```bash
git add src/main/java/org/sqlmodel/OracleMetadataReader.java src/main/java/org/sqlmodel/OracleCommentReader.java src/test/java/org/sqlmodel/OracleMetadataReaderTest.java src/test/java/org/sqlmodel/OracleCommentReaderTest.java src/test/java/org/sqlmodel/JdbcProxyFixtures.java
git commit -m "feat: read Oracle result metadata and comments"
```

### Task 6: Render deterministic DZ Java source

**Files:**
- Create: `src/main/java/org/sqlmodel/DzModelRenderer.java`
- Create: `src/test/java/org/sqlmodel/DzModelRendererTest.java`

- [ ] **Step 1: Write exact-output tests**

Cover:

- package declaration;
- fixed DZ/Gson imports;
- conditional `BigDecimal` and `LocalDateTime` imports;
- SQL label preserved in all three annotation string values;
- DB comment used only as `desc`;
- camelCase field and PascalCase accessors;
- quote/backslash/newline escaping in descriptions.

- [ ] **Step 2: Run test to verify failure**

Run: `mvn test -Dtest=DzModelRendererTest`

Expected: FAIL because renderer does not exist.

- [ ] **Step 3: Implement renderer**

Fixed signature:

```java
String render(String packageName, String className, List<ColumnSpec> columns)
```

Validate package/class names before rendering. End files with exactly one newline. Do not add Lombok or templates.

- [ ] **Step 4: Run test and commit**

Run: `mvn test -Dtest=DzModelRendererTest`

Expected: PASS.

```bash
git add src/main/java/org/sqlmodel/DzModelRenderer.java src/test/java/org/sqlmodel/DzModelRendererTest.java
git commit -m "feat: render DZ model source"
```

### Task 7: Implement CLI input, safe output, and application orchestration

**Files:**
- Create: `src/main/java/org/sqlmodel/CliOptions.java`
- Create: `src/main/java/org/sqlmodel/SqlInput.java`
- Create: `src/main/java/org/sqlmodel/SourceOutput.java`
- Create: `src/main/java/org/sqlmodel/ModelConvertorApplication.java`
- Modify: `src/main/java/org/sqlmodel/Main.java`
- Create: `src/test/java/org/sqlmodel/CliOptionsTest.java`
- Create: `src/test/java/org/sqlmodel/SqlInputTest.java`
- Create: `src/test/java/org/sqlmodel/SourceOutputTest.java`
- Create: `src/test/java/org/sqlmodel/ModelConvertorApplicationTest.java`

- [ ] **Step 1: Write option and input tests**

Verify every documented option, unknown/missing values, `--sql-file` precedence, piped stdin, interactive `:end`, and UTF-8 file input.

- [ ] **Step 2: Write output tests**

Use `TemporaryFolder` to verify the default `{cwd}/src/main/java/{package}/{class}.java`, directory creation, UTF-8 Korean comments, existing-file rejection, `--overwrite` truncation rather than append, and stdout mode with no file.

- [ ] **Step 3: Write application tests with injected JDBC connection**

Verify validation occurs before DB connection, successful flow reports the absolute path, failures return non-zero, and error output never contains `oracle.password`.

- [ ] **Step 4: Run tests to verify failure**

Run: `mvn test -Dtest=CliOptionsTest,SqlInputTest,SourceOutputTest,ModelConvertorApplicationTest`

Expected: FAIL because CLI units do not exist.

- [ ] **Step 5: Implement CLI and output minimally**

`CliOptions.parse(String[])` holds only documented options. `SqlInput` accepts supplied streams/readers for tests. `SourceOutput` uses `Files.newBufferedWriter` with `CREATE_NEW` or `CREATE` plus `TRUNCATE_EXISTING`.

- [ ] **Step 6: Wire application and Main**

`Main.run` creates standard streams and delegates to `ModelConvertorApplication`. Load the Oracle driver through `DriverManager.getConnection(config.url(), config.username(), config.password())`; do not import Oracle classes. Help returns 0, usage/input errors return 2, DB/processing errors return 1.

- [ ] **Step 7: Run focused and full tests, then commit**

Run: `mvn test -Dtest=CliOptionsTest,SqlInputTest,SourceOutputTest,ModelConvertorApplicationTest`

Expected: PASS.

Run: `mvn test`

Expected: BUILD SUCCESS.

```bash
git add src/main/java/org/sqlmodel/Main.java src/main/java/org/sqlmodel/CliOptions.java src/main/java/org/sqlmodel/SqlInput.java src/main/java/org/sqlmodel/SourceOutput.java src/main/java/org/sqlmodel/ModelConvertorApplication.java src/test/java/org/sqlmodel/CliOptionsTest.java src/test/java/org/sqlmodel/SqlInputTest.java src/test/java/org/sqlmodel/SourceOutputTest.java src/test/java/org/sqlmodel/ModelConvertorApplicationTest.java
git commit -m "feat: add interactive model converter CLI"
```

### Task 8: Package, remove the legacy prototype, and verify end to end

**Files:**
- Create: `modelconvertor.cmd`
- Modify: `README.md`
- Delete: `src/main/java/org/sqlmodel/FileCreator.java`
- Delete: `src/main/java/org/sqlmodel/GenerationOptions.java`
- Delete: `src/main/java/org/sqlmodel/ModelGenerator.java`
- Delete: `src/main/java/org/sqlmodel/SimpleModelGeneratorImpl.java`
- Delete: `src/main/java/org/sqlmodel/SqlContents.java`
- Delete: `src/main/java/org/sqlmodel/SqlModel.java`
- Delete: `src/main/java/org/sqlmodel/SqlToModelTest.java`
- Create: `src/test/java/org/sqlmodel/GeneratedSourceCompilationTest.java`

- [ ] **Step 1: Write generated-source compilation test**

Use `javax.tools.JavaCompiler` with small test stubs for DZ/Gson annotation classes. Render a model containing String, BigDecimal, and LocalDateTime and assert Java 8 compilation succeeds.

- [ ] **Step 2: Run test to verify any missing imports or syntax fail**

Run: `mvn test -Dtest=GeneratedSourceCompilationTest`

Expected before final fixes: FAIL if generated source is not independently valid.

- [ ] **Step 3: Add runtime CMD**

The script resolves its own directory and runs:

```bat
@echo off
setlocal
set "APP_DIR=%~dp0"
java -cp "%APP_DIR%modelconvertor.jar;%APP_DIR%ojdbc8.jar" org.sqlmodel.Main %*
exit /b %ERRORLEVEL%
```

- [ ] **Step 4: Replace README with installation and usage**

Document Java 8, directory layout, external config path, direct paste, SQL file, stdin, output behavior, `--stdout`, `--overwrite`, Oracle read-only account, and supported SQL limitations.

- [ ] **Step 5: Remove legacy classes**

Delete only after `rg` confirms no new class imports them. RN/ROWNUM auto-exclusion is intentionally removed; every JDBC result column is generated.

- [ ] **Step 6: Run all verification commands**

Run: `mvn clean test`

Expected: BUILD SUCCESS, all tests pass.

Run: `mvn package -DskipTests`

Expected: executable JAR produced under `target/` with `Main-Class: org.sqlmodel.Main`.

Run: `java -jar target/modelconvertor-1.0-SNAPSHOT.jar --help`

Expected: exit 0 and usage containing every documented option.

Run: `git diff --check`

Expected: no output.

- [ ] **Step 7: Commit**

```bash
git add -A README.md modelconvertor.cmd pom.xml src
git commit -m "feat: complete Oracle model converter CLI"
```

## Final review gates

- [ ] Spec reviewer maps every section of `docs/superpowers/specs/2026-07-11-modelconvertor-cli-oracle-design.md` to implementation and tests.
- [ ] Code-quality reviewer checks Java 8 compatibility, resource closure, secret redaction, SQL execution safety, and absence of speculative abstractions.
- [ ] Claude Code runs as an independent external verifier with Sonnet, read-only plan permissions, and a bounded turn count:

```powershell
claude.cmd -p --model sonnet --permission-mode plan --max-turns 8 "Review this worktree against docs/superpowers/specs/2026-07-11-modelconvertor-cli-oracle-design.md and docs/superpowers/plans/2026-07-11-modelconvertor-cli-oracle-implementation.md. Inspect the implementation and tests. Do not edit files. Report blocking, important, and minor findings with file:line evidence, then state whether the implementation is ready."
```

- [ ] Root agent shares Claude's findings with active implementation/review agents, verifies each finding against the code, and fixes only confirmed issues before re-running the external review when necessary.
- [ ] Root agent runs the full test/package/help verification from a clean worktree.
- [ ] Optional real-Oracle checks are reported separately when Oracle credentials/driver are unavailable; all proxy-backed behavior must still pass.
