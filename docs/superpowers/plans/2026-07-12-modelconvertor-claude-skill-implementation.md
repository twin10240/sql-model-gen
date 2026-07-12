# ModelConvertor Claude Code Skill Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Claude Code가 Oracle SQL 기반 DZ 모델 생성 요청을 발견하고 기존 `modelconvertor.cmd`를 안전하게 호출하도록 최소 프로젝트 스킬을 제공한다.

**Architecture:** `.claude/skills/modelconvertor/SKILL.md` 한 파일에 발견 조건과 실행 지시를 둔다. 별도 parser, prompt 코드, 설치 검사, 실행 래퍼 및 supporting file을 만들지 않고 기존 CLI의 입력·출력·오류 경계를 그대로 사용한다.

**Tech Stack:** Claude Code Agent Skills, Markdown, PowerShell, 기존 Windows CMD CLI

---

## File Structure

- Create: `.claude/skills/modelconvertor/SKILL.md` — Claude Code의 자동·명시 호출 메타데이터와 최소 실행 지시
- Reference: `modelconvertor.cmd` — 추가 래퍼 없이 직접 호출할 기존 진입점
- Reference: `README.md` — 설치 오류가 발생했을 때 안내할 원본 문서
- Reference: `docs/skill-usage-guide.md` — 검증할 사용자 흐름

`skill-creator`의 `init_skill.py`는 Codex용 `agents/openai.yaml`을 함께 생성하므로 이번 Claude Code 단일 파일 범위에서는 사용하지 않는다.

### Task 1: RED 기준선 기록

**Files:**
- Verify only: 저장소 파일 변경 없음

- [ ] **Step 1: 스킬이 아직 없는지 확인한다**

Run:

```powershell
if (Test-Path '.claude/skills/modelconvertor/SKILL.md') { throw 'Skill already exists; RED baseline is invalid' }
```

Expected: 출력 없이 종료 코드 `0`.

- [ ] **Step 2: SQL 본문 요청의 기준선을 실행한다**

Run:

```powershell
claude.cmd -p --model sonnet --permission-mode plan --max-turns 4 "다음 SQL로 DZ 모델을 생성하려고 한다. 실행할 명령과 필요한 추가 정보를 답해라. SELECT EMP_NO FROM HR_EMP"
```

Record: `modelconvertor.cmd`, `--class-name`, `--package` 중 무엇을 알지 못하거나 빠뜨리는지 응답에서 확인한다.

- [ ] **Step 3: 파일 경로와 누락값 요청의 기준선을 실행한다**

Run:

```powershell
claude.cmd -p --model sonnet --permission-mode plan --max-turns 4 "C:\queries\employee.txt로 DZ 모델을 만들어줘. 클래스명과 패키지명은 아직 정하지 않았어."
```

Record: 실행 전에 클래스명과 패키지명을 질문하는지, 존재 여부를 확인하지 않고 파일로 단정하는지 확인한다.

- [ ] **Step 4: 출력·실패 안전 기준선을 실행한다**

Run:

```powershell
claude.cmd -p --model sonnet --permission-mode plan --max-turns 4 "employee.sql로 모델을 만들되 파일은 쓰지 말고 결과만 보여줘. 명령이 실패하면 어떻게 처리할지도 답해줘."
```

Record: `--stdout`, stderr 전달, 자동 재시도 금지를 빠뜨리는지 확인한다.

- [ ] **Step 5: 기준선 실패 패턴을 한 문단으로 요약한다**

구현 메모에 실제 응답에서 확인한 누락만 기록한다. 예상 실패를 사실처럼 기록하지 않는다. 최소 스킬은 이 누락만 보완한다.

### Task 2: GREEN 최소 Claude Code 스킬 작성

**Files:**
- Create: `.claude/skills/modelconvertor/SKILL.md`

- [ ] **Step 1: 스킬 디렉터리와 파일을 만든다**

`apply_patch`로 다음 파일을 생성한다.

```markdown
---
name: modelconvertor
description: Use when generating a Java DZ model from an Oracle SELECT query, SQL text, or a SQL file with ModelConvertor.
---

# ModelConvertor

Use the existing `modelconvertor.cmd` directly. Do not create another wrapper, parser, or installer.

1. Treat an existing user-provided text-file path as `--sql-file`. Treat provided SQL text as standard input; use a temporary UTF-8 file only when multiline piping is unsafe, then delete it.
2. Before running, ask only for a missing class name or package name. Pass both with `--class-name` and `--package`.
3. Use `--stdout` for preview-only requests, `--output` for an explicit source root, and `--overwrite` only when the user explicitly requests replacement.
4. Do not alter the SQL or request an Oracle password in chat or command arguments.
5. If the command is missing or installation fails, point to the repository `README.md`; do not add preflight components.
6. On a nonzero exit, report the exit code and stderr without omitting or rewriting the error, and do not retry automatically. The CLI masks its configured password in Oracle/JDBC errors.
```

- [ ] **Step 2: frontmatter와 최소 범위를 검증한다**

Run:

```powershell
$path = '.claude/skills/modelconvertor/SKILL.md'
$text = Get-Content -Raw -Encoding UTF8 $path
if ($text -notmatch '(?s)^---\r?\nname: modelconvertor\r?\ndescription: Use when') { throw 'Invalid skill frontmatter' }
foreach ($term in @('modelconvertor.cmd','--sql-file','--class-name','--package','--stdout','--overwrite','stderr','do not retry automatically')) {
    if (-not $text.Contains($term)) { throw "Missing skill term: $term" }
}
foreach ($forbidden in @('scripts/','wrapper.ps1','parser.py','agents/openai.yaml')) {
    if ($text.Contains($forbidden)) { throw "Unexpected scope: $forbidden" }
}
```

Expected: 출력 없이 종료 코드 `0`.

- [ ] **Step 3: 스킬 크기와 구조를 검증한다**

Run:

```powershell
$path = '.claude/skills/modelconvertor/SKILL.md'
$bodyLines = (Get-Content -Encoding UTF8 $path).Count
if ($bodyLines -gt 30) { throw "Skill is too long: $bodyLines lines" }
$extra = Get-ChildItem '.claude/skills/modelconvertor' -Recurse -File | Where-Object Name -ne 'SKILL.md'
if ($extra) { throw "Unexpected supporting files: $($extra.FullName -join ', ')" }
git diff --check -- $path
```

Expected: 출력 없이 종료 코드 `0`.

- [ ] **Step 4: 최소 스킬을 커밋한다**

```powershell
git add -- .claude/skills/modelconvertor/SKILL.md
git diff --cached --check
git commit -m "feat: add modelconvertor Claude skill"
```

### Task 3: GREEN/REFACTOR Claude Code 수동 검증

**Files:**
- Modify only if a verified gap exists: `.claude/skills/modelconvertor/SKILL.md`

- [ ] **Step 1: 자동 호출을 검증한다**

새 Claude Code 세션 또는 스킬 변경을 감지한 세션에서 실행한다.

```powershell
claude.cmd -p --model sonnet --permission-mode plan --max-turns 6 "다음 SQL로 DZ 모델을 만들어줘. 클래스는 EmployeeModel, 패키지는 com.company.hr.model이야. SELECT EMP_NO FROM HR_EMP"
```

Expected: `modelconvertor` 스킬을 사용하고 SQL 본문을 표준 입력으로 전달하는 `modelconvertor.cmd` 명령을 제안한다. `--class-name EmployeeModel`과 `--package com.company.hr.model`을 포함하고 추가 래퍼를 만들지 않는다.

- [ ] **Step 2: 명시 호출과 누락값 질문을 검증한다**

Run:

```powershell
claude.cmd -p --model sonnet --permission-mode plan --max-turns 4 "/modelconvertor C:\queries\employee.txt로 DZ 모델을 만들어줘."
```

Expected: CLI를 실행하기 전에 누락된 클래스명과 패키지명을 질문한다. 존재하지 않는 경로라면 `--sql-file` 실행을 확정하지 않는다.

- [ ] **Step 3: preview와 실패 전달을 검증한다**

Run:

```powershell
claude.cmd -p --model sonnet --permission-mode plan --max-turns 6 "/modelconvertor employee.sql로 모델을 만들되 파일은 쓰지 말고 결과만 보여줘. 실패 시 처리도 설명해줘. 클래스는 EmployeeModel, 패키지는 com.company.hr.model이야."
```

Expected: `--stdout`을 포함하고, 0이 아닌 종료 코드와 stderr를 사용자에게 전달하며 자동 재시도하지 않는다고 답한다.

- [ ] **Step 4: 덮어쓰기 안전성을 검증한다**

Run:

```powershell
claude.cmd -p --model sonnet --permission-mode plan --max-turns 4 "/modelconvertor employee.sql로 EmployeeModel을 만들어줘. 패키지는 com.company.hr.model이야."
```

Expected: 사용자가 교체를 요청하지 않았으므로 `--overwrite`를 포함하지 않는다.

- [ ] **Step 5: 검증에서 발견된 실제 누락만 최소 수정한다**

응답이 기대와 다를 때 원인을 한 가지로 특정하고 `SKILL.md` 한 문장만 수정한다. 같은 시나리오를 다시 실행해 통과 여부를 확인한다. 새로운 supporting file이나 실행 코드를 추가하지 않는다.

- [ ] **Step 6: 수정이 있었다면 커밋한다**

```powershell
git add -- .claude/skills/modelconvertor/SKILL.md
git diff --cached --check
git diff --cached --quiet; if ($LASTEXITCODE -ne 0) { git commit -m "fix: clarify modelconvertor skill workflow" }
```

### Task 4: 최종 검증과 Codex 배치 판단

**Files:**
- Verify: `.claude/skills/modelconvertor/SKILL.md`
- Do not create yet: `.agents/skills/modelconvertor/SKILL.md`

- [ ] **Step 1: 저장소 테스트를 실행한다**

Run:

```powershell
& 'C:\Users\admin\AppData\Local\Programs\IntelliJ IDEA\plugins\maven\lib\maven3\bin\mvn.cmd' test
```

Expected: 59 tests, 0 failures, 0 errors.

- [ ] **Step 2: 최종 스킬 구조를 확인한다**

Run:

```powershell
git diff --check HEAD~1..HEAD
Get-ChildItem '.claude/skills/modelconvertor' -Recurse -File | Select-Object FullName
git status --short
```

Expected: 스킬 디렉터리에는 `SKILL.md`만 있다. 이번 작업 변경은 모두 커밋되어 있고 기존 사용자 변경은 보존된다.

- [ ] **Step 3: E2E 검증 범위를 보고한다**

실제 JAR와 Oracle 연결을 사용했다면 결과를 기록한다. 사용하지 못했다면 Claude의 스킬 선택, 명령 구성, 질문, 오류 전달까지만 검증했으며 실제 모델 생성 E2E는 미검증이라고 명시한다.

- [ ] **Step 4: Codex 배치 여부를 결정한다**

Claude Code 시나리오가 모두 통과했을 때만 동일 본문을 `.agents/skills/modelconvertor/SKILL.md`에 배치하는 후속 작업을 제안한다. 이번 계획에서는 복사본이나 동기화 스크립트를 만들지 않는다.
