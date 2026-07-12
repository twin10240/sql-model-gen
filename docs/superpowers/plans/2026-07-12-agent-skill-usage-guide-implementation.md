# Agent Skill Usage Guide Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Codex와 Claude Code 스킬 사용자가 자연어로 SQL 본문 또는 텍스트 파일을 전달하고, 누락 정보 질문과 출력·오류 동작을 정확히 이해할 수 있는 상세 가이드와 README 진입점을 제공한다.

**Architecture:** `docs/skill-usage-guide.md`를 스킬 사용법의 단일 원본으로 만들고 `README.md`에는 짧은 소개와 링크만 둔다. 설명은 현재 Java 구현의 입력 분기와 종료 코드 경계를 그대로 반영하며, 실제 스킬 파일이나 CLI 동작은 변경하지 않는다.

**Tech Stack:** Markdown, PowerShell 검증 명령, Git

---

## File Structure

- Create: `docs/skill-usage-guide.md` — Codex·Claude Code 스킬 사용 흐름, 입력, 누락값 질문, 출력, 오류 및 종료 코드의 상세 원본
- Modify: `README.md` — 짧은 스킬 사용 안내, 상세 가이드 링크, 기존 종료 코드 표의 정확한 경계

### Task 1: 상세 스킬 사용 가이드 작성

**Files:**
- Create: `docs/skill-usage-guide.md`
- Reference: `docs/superpowers/specs/2026-07-12-agent-skill-usage-guide-design.md`
- Reference: `src/main/java/org/sqlmodel/ModelConvertorApplication.java`
- Reference: `src/main/java/org/sqlmodel/SqlInput.java`
- Reference: `src/main/java/org/sqlmodel/SourceOutput.java`

- [ ] **Step 1: 상세 가이드 파일을 만들고 목적과 역할을 설명한다**

다음 제목과 도입부로 `docs/skill-usage-guide.md`를 만든다.

````markdown
# Codex·Claude Code 스킬 사용 가이드

ModelConvertor를 Codex 또는 Claude Code의 스킬로 사용할 때는 사용자가 `modelconvertor` 명령 옵션을 직접 조합할 필요가 없습니다. SQL과 원하는 모델 정보를 자연어로 전달하면 스킬이 필요한 값을 확인한 뒤 CLI를 실행합니다.

## 사용자, 스킬, CLI의 역할

```text
사용자 ↔ Codex·Claude Code 스킬 → ModelConvertor CLI → Java 모델 파일
```

- 사용자는 SQL 본문 또는 SQL이 저장된 UTF-8 텍스트 파일 경로를 전달합니다.
- 스킬은 클래스명과 패키지명 등 누락된 정보만 사용자에게 질문합니다.
- CLI는 스킬이 전달한 완성된 옵션을 검증하고 Oracle 메타데이터를 읽어 모델을 생성합니다.

스킬과 사용자의 대화는 대화형이지만, 스킬은 필요한 값을 모두 확보한 뒤 CLI를 비대화형으로 실행합니다.
````

- [ ] **Step 2: SQL 본문과 파일 입력 흐름을 작성한다**

다음 내용을 이어서 추가한다.

````markdown
## SQL 입력

### SQL 본문 직접 입력

SQL을 요청에 바로 붙여 넣을 수 있습니다.

```text
다음 SQL로 DZ 모델을 만들어줘.

SELECT EMP.EMP_NO,
       EMP.EMP_NM AS employeeName
  FROM HR_EMP EMP
```

스킬은 SQL 본문을 CLI의 표준 입력으로 전달합니다.

### 텍스트 파일 입력

SQL이 저장된 파일 경로를 전달할 수도 있습니다.

```text
C:\queries\employee.txt로 DZ 모델을 만들어줘.
```

파일 확장자는 `.sql`로 제한되지 않습니다. `.sql`, `.txt` 등 파일명이 무엇이든 존재하는 UTF-8 텍스트 파일이면 스킬이 `--sql-file`로 전달할 수 있습니다. 파일이 없거나 읽을 수 없으면 스킬은 경로와 오류 원인을 알려야 합니다.
````

- [ ] **Step 3: 클래스명과 패키지명 누락 시 질문 흐름을 작성한다**

다음 내용을 이어서 추가한다.

````markdown
## 클래스명과 패키지명

스킬로 실행할 때 클래스명과 패키지명은 최종적으로 모두 필요합니다. 사용자가 생략하면 스킬이 CLI를 실행하기 전에 누락된 값만 질문합니다.

```text
사용자: employee.sql로 DZ 모델을 만들어줘.
스킬: 생성할 클래스명과 패키지명을 알려주세요.
사용자: EmployeeModel, com.company.hr.model
```

클래스명만 제공했다면 패키지명만, 패키지명만 제공했다면 클래스명만 질문합니다. 두 값을 확보한 뒤 스킬은 `--class-name`과 `--package`를 포함하여 CLI를 실행합니다.

스킬이 값을 확보하지 않고 파이프 방식으로 CLI를 실행하면 CLI는 대화형으로 되묻지 않고 종료 코드 `2`와 함께 다음 오류를 반환합니다.

```text
Error: --class-name and --package are required for piped input
```
````

- [ ] **Step 4: 출력 위치와 파일 보호 동작을 작성한다**

다음 내용을 이어서 추가한다.

````markdown
## 출력

### 기본 출력

출력 경로를 지정하지 않으면 스킬이 CLI를 실행한 현재 작업 디렉터리를 기준으로 다음 파일을 생성합니다.

```text
src/main/java/<패키지 경로>/<클래스명>.java
```

예를 들어 현재 디렉터리가 `C:\work\my-project`, 클래스명이 `EmployeeModel`, 패키지명이 `com.company.hr.model`이면 다음 파일이 생성됩니다.

```text
C:\work\my-project\src\main\java\com\company\hr\model\EmployeeModel.java
```

필요한 패키지 디렉터리는 자동으로 생성됩니다.

### 출력 경로 지정

사용자가 출력 소스 루트를 지정하면 스킬은 `--output`으로 전달합니다.

```text
생성 파일을 C:\work\my-project\generated 아래에 만들어줘.
```

### 결과만 확인

파일을 생성하지 않고 결과만 확인해 달라고 요청하면 스킬은 `--stdout`을 사용합니다.

```text
파일은 만들지 말고 생성될 코드만 보여줘.
```

### 기존 파일

같은 파일이 이미 있으면 기본적으로 덮어쓰지 않고 종료 코드 `1`로 실패합니다. 사용자가 명시적으로 덮어쓰기를 요청한 경우에만 스킬이 `--overwrite`를 사용해야 합니다.
````

- [ ] **Step 5: 오류와 종료 코드 경계를 작성한다**

다음 내용을 이어서 추가한다.

````markdown
## 오류와 종료 코드

| 종료 코드 | 의미 | 대표 상황 |
|---|---|---|
| `0` | 정상 처리 | 모델 생성 또는 `--help` 출력 |
| `1` | 처리 실패 | SQL 입력 I/O, 설정 파일 로딩, 필수 설정 키 누락, Oracle 연결·JDBC 처리, Oracle 실행 중 SQL 실패, 출력 파일 충돌 |
| `2` | 호출·입력 검증 실패 | 잘못된 CLI 옵션, 클래스명·패키지명 검증, SQL 사전 검증, 중복 결과 라벨·Java 필드명, 파이프 입력의 필수 옵션 누락 |

SQL 오류는 발생 단계에 따라 종료 코드가 다릅니다.

- ModelConvertor가 Oracle에 전달하기 전에 SQL 형식이나 지원 범위를 거부하면 `2`입니다.
- Oracle 또는 JDBC 처리 중 SQL 실행이 실패하면 `1`입니다.

설정 파일의 필수 키 누락은 입력 검증처럼 보여도 설정 로딩 실패로 처리되므로 `1`입니다. Oracle 오류 메시지에 설정된 비밀번호가 포함되면 CLI는 해당 값을 `***`로 마스킹합니다.
````

- [ ] **Step 6: 준비 사항과 대표 요청 예시를 작성한다**

다음 내용으로 문서를 마무리한다.

````markdown
## 실행 전 준비 사항

스킬이 CLI를 호출할 수 있도록 다음 준비가 필요합니다.

- `modelconvertor.jar`, `modelconvertor.cmd`, `ojdbc8.jar` 설치
- `%USERPROFILE%\.modelconvertor\oracle.properties` 또는 별도 Oracle 설정 파일
- 가능한 경우 조회 전용 Oracle 계정
- 모델을 생성할 Java 프로젝트를 현재 작업 디렉터리로 사용

설정 파일이나 JDBC 드라이버가 없으면 스킬은 임의로 접속 정보를 만들지 않고 필요한 파일과 경로를 안내해야 합니다. 비밀번호를 채팅이나 명령 인자로 요청하지 않습니다.

## 요청 예시

```text
이 SELECT SQL로 EmployeeModel을 만들어줘. 패키지는 com.company.hr.model이야.
```

```text
C:\queries\employee.txt로 DZ 모델을 만들어줘. 클래스명은 EmployeeModel이야.
```

```text
employee.sql로 모델을 만들되 파일은 쓰지 말고 결과만 보여줘.
```

```text
employee.sql로 생성해서 기존 EmployeeModel.java를 덮어써줘.
```

스킬은 각 요청에서 빠진 클래스명이나 패키지명만 추가로 질문한 뒤 실행합니다.
````

- [ ] **Step 7: 상세 가이드의 필수 내용과 형식을 검증한다**

Run:

```powershell
$path = 'docs/skill-usage-guide.md'
if (-not (Test-Path $path)) { throw "$path is missing" }
$required = @('SQL 본문 직접 입력', '텍스트 파일 입력', '클래스명과 패키지명', '기본 출력', '--stdout', '--overwrite', '종료 코드', '필수 설정 키 누락')
$text = Get-Content -Raw -Encoding UTF8 $path
$required | ForEach-Object { if (-not $text.Contains($_)) { throw "Missing section or term: $_" } }
```

Expected: 출력 없이 종료 코드 `0`.

- [ ] **Step 8: 상세 가이드를 커밋한다**

```powershell
git add -- docs/skill-usage-guide.md
git diff --cached --check
git commit -m "docs: add agent skill usage guide"
```

### Task 2: README 진입점과 종료 코드 설명 정리

**Files:**
- Modify: `README.md:95`
- Modify: `README.md:212-220`
- Modify: `README.md:236-239`

- [ ] **Step 1: 사용법 앞에 짧은 스킬 안내를 추가한다**

`## 사용법` 바로 앞에 다음 섹션을 추가한다.

```markdown
## Codex·Claude Code 스킬 사용

스킬을 사용하면 명령 옵션을 직접 조합하지 않고 SQL 본문이나 `.sql`, `.txt` 등 UTF-8 텍스트 파일 경로를 자연어로 전달할 수 있습니다. 클래스명이나 패키지명이 빠져 있으면 스킬이 누락된 값만 질문한 뒤 ModelConvertor를 실행합니다.

자세한 입력·출력 흐름과 오류 처리는 [Codex·Claude Code 스킬 사용 가이드](docs/skill-usage-guide.md)를 참고하세요.
```

- [ ] **Step 2: 기존 종료 코드 표의 모호한 SQL 표현을 바로잡는다**

`## 오류 메시지와 종료 코드` 표와 바로 아래 설명을 다음 내용으로 교체한다.

```markdown
| 종료 코드 | 상황 |
|---|---|
| `0` | 정상 처리 또는 `--help` 출력 |
| `1` | SQL 입력 I/O, 설정 로딩과 필수 키 누락, Oracle 연결·JDBC 처리, Oracle 실행 중 SQL 실패, 출력 파일 충돌 등 처리 실패 |
| `2` | 잘못된 옵션, 클래스명·패키지명, SQL 사전 검증, 중복 결과 라벨·Java 필드명, 파이프 입력 필수 옵션 누락 등 호출·입력 검증 실패 |

SQL 사전 검증 실패는 `2`, Oracle/JDBC 처리 중 SQL 실패는 `1`입니다. 설정 파일의 필수 키 누락은 설정 로딩 실패이므로 `1`입니다. 설정 파일 또는 SQL 파일을 읽지 못하면 해당 경로와 원인을 출력합니다. Oracle 처리 실패 시 Oracle 오류 코드와 원인을 출력하되, 설정된 비밀번호가 오류 메시지에 포함되어도 `***`로 마스킹합니다.
```

- [ ] **Step 3: 상세 문서 목록에도 스킬 가이드 링크를 추가한다**

`## 상세 문서` 목록에 다음 항목을 추가한다.

```markdown
- [Codex·Claude Code 스킬 사용 가이드](docs/skill-usage-guide.md)
```

- [ ] **Step 4: README 링크와 중복 범위를 검증한다**

Run:

```powershell
$readme = Get-Content -Raw -Encoding UTF8 README.md
if (-not $readme.Contains('## Codex·Claude Code 스킬 사용')) { throw 'README skill section is missing' }
if (-not $readme.Contains('(docs/skill-usage-guide.md)')) { throw 'README skill guide link is missing' }
if (-not (Test-Path 'docs/skill-usage-guide.md')) { throw 'Linked skill guide does not exist' }
if (-not $readme.Contains('SQL 사전 검증 실패는 `2`')) { throw 'README exit-code boundary is missing' }
```

Expected: 출력 없이 종료 코드 `0`.

- [ ] **Step 5: 전체 문서 변경을 검증한다**

Run:

```powershell
git diff --check -- README.md docs/skill-usage-guide.md
rg -n "TODO|TBD|구현 예정" README.md docs/skill-usage-guide.md
```

Expected: `git diff --check` 성공. `rg`는 결과 없이 종료 코드 `1`.

- [ ] **Step 6: README 변경을 커밋한다**

```powershell
git add -- README.md
git diff --cached --check
git commit -m "docs: link agent skill usage guide"
```

### Task 3: 최종 구현 대조 검증

**Files:**
- Verify: `README.md`
- Verify: `docs/skill-usage-guide.md`
- Reference: `src/main/java/org/sqlmodel/CliOptions.java`
- Reference: `src/main/java/org/sqlmodel/SqlInput.java`
- Reference: `src/main/java/org/sqlmodel/ModelConvertorApplication.java`
- Reference: `src/main/java/org/sqlmodel/SourceOutput.java`

- [ ] **Step 1: 문서의 입력·출력 설명을 구현과 대조한다**

다음을 직접 확인한다.

- `SqlInput.read`: 확장자 검사 없이 UTF-8 파일을 읽고, 파일이 없으면 표준 입력을 사용한다.
- `ModelConvertorApplication.run`: 비터미널 입력에서 클래스명 또는 패키지명이 빠지면 `2`를 반환한다.
- `SourceOutput.path`: 기본 소스 루트가 `src/main/java`이고 파일명이 `<클래스명>.java`다.
- `SourceOutput.write`: `--stdout`이면 파일을 만들지 않고, 기본 쓰기는 기존 파일을 덮어쓰지 않는다.
- `ModelConvertorApplication.run`: 설정 키 누락과 Oracle/JDBC 실패는 `1`, 외부 `IllegalArgumentException`은 `2`다.

- [ ] **Step 2: 링크와 문서 형식을 최종 검증한다**

Run:

```powershell
$guide = 'docs/skill-usage-guide.md'
$readme = Get-Content -Raw -Encoding UTF8 README.md
if (-not (Test-Path $guide)) { throw "$guide is missing" }
if (-not $readme.Contains("($guide)")) { throw 'README does not link the guide' }
git diff --check HEAD~2..HEAD
```

Expected: 출력 없이 종료 코드 `0`.

- [ ] **Step 3: 작업 트리를 확인한다**

Run:

```powershell
git status --short
```

Expected: 이번 계획에서 추적한 `README.md`와 `docs/skill-usage-guide.md` 변경이 남아 있지 않다. 기존 사용자 변경인 `.idea/misc.xml`과 `.claude/`는 그대로 남아 있을 수 있다.
