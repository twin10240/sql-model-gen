# Fixed Default Oracle Config Path Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make `C:\Douzone\dews-web\config\modelconvertor\oracle.properties` the default Oracle configuration file while preserving `--config` overrides.

**Architecture:** Change the existing default-path provider only. The existing CLI override and properties loader remain unchanged.

**Tech Stack:** Java 8, `java.nio.file.Path`, JUnit 4, Maven

---

### Task 1: Fixed default Oracle config path

**Files:**
- Modify: `src/test/java/org/sqlmodel/OracleConfigTest.java`
- Modify: `src/main/java/org/sqlmodel/OracleConfig.java`

- [ ] **Step 1: Write the failing test**

Replace the current user-home-based default-path assertion with:

```java
@Test public void defaultPathUsesDewsWebConfigDirectory() {
    assertEquals(Paths.get("C:\\Douzone\\dews-web\\config\\modelconvertor\\oracle.properties"),
            OracleConfig.defaultPath());
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `mvn -Dtest=OracleConfigTest#defaultPathUsesDewsWebConfigDirectory test`

Expected: FAIL because the current implementation resolves the path from `user.home`.

- [ ] **Step 3: Implement the fixed path**

Replace the existing default-path methods with:

```java
static Path defaultPath() {
    return Paths.get("C:\\Douzone\\dews-web\\config\\modelconvertor\\oracle.properties");
}
```

- [ ] **Step 4: Run verification**

Run: `mvn -Dtest=OracleConfigTest test`

Expected: PASS.

Run: `mvn test`

Expected: all tests PASS.

- [ ] **Step 5: Commit**

```text
git add src/main/java/org/sqlmodel/OracleConfig.java src/test/java/org/sqlmodel/OracleConfigTest.java docs/superpowers/plans/2026-07-13-fixed-default-oracle-config-path.md
git commit -m "feat: fix default Oracle config path"
```
