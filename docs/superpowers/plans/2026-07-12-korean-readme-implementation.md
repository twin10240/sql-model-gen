# Korean README Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the English README with an accurate Korean installation and usage guide and publish all completed project documentation.

**Architecture:** Keep `README.md` task-oriented and link to the detailed flow document instead of duplicating internal implementation. Verify every command, option, path, error behavior, and limitation against the current source and launcher.

**Tech Stack:** Markdown, Mermaid link, Windows PowerShell, Java 8, Maven, Oracle JDBC

---

### Task 1: Write the Korean README

**Files:**
- Modify: `README.md`
- Include: `docs/modelconvertor-flow.md`

- [ ] **Step 1: Replace README sections**

Write the approved sections in this order: overview, features, prerequisites, build, installation layout, Oracle properties, CLI options, interactive/file/pipe/stdout/overwrite/config examples, output behavior, error table, SQL limitations, tests, and flow-document link.

- [ ] **Step 2: Verify commands against implementation**

Run: `java -jar target/modelconvertor.jar --help`

Expected: README option list matches the application usage text.

Check `modelconvertor.cmd`, `OracleConfig.defaultPath()`, `SourceOutput.path()`, and `ModelConvertorApplication.run()` against the documented classpath, config path, output path, and exit codes.

- [ ] **Step 3: Verify Markdown formatting**

Run: `git diff --check -- README.md docs/modelconvertor-flow.md`

Expected: no output.

- [ ] **Step 4: Commit documentation**

Run: `git add -- README.md docs/modelconvertor-flow.md docs/superpowers/plans/2026-07-12-korean-readme-implementation.md`

Run: `git commit -m "docs: add Korean usage guide"`

### Task 2: Verify and publish

**Files:**
- Verify: `README.md`
- Verify: `docs/modelconvertor-flow.md`

- [ ] **Step 1: Run full verification**

Run: `mvn clean package`

Expected: BUILD SUCCESS with all tests passing.

Run: `java -jar target/modelconvertor.jar --help`

Expected: usage output and exit code 0.

- [ ] **Step 2: Confirm only intended local changes remain**

Run: `git status --short`

Expected: only pre-existing local IDE/tool settings remain outside commits.

- [ ] **Step 3: Push master**

Run: `git push origin master`

Expected: remote `master` advances to the verified documentation commit.
