# Global Installation Guide Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Windows의 여러 Java 프로젝트에서 Claude Code 전역 스킬과 PATH 기반 ModelConvertor CLI를 안전하게 설치·검증·업데이트·제거하는 가이드를 제공한다.

**Architecture:** `docs/global-installation-guide.md`를 상세 절차의 단일 원본으로 만들고 README에는 짧은 소개와 링크만 추가한다. 문서는 PowerShell 변수로 경로를 정의하고, 기존 파일·PATH·Oracle 설정을 확인 없이 덮어쓰거나 삭제하지 않는 절차를 사용한다.

**Tech Stack:** Markdown, PowerShell, Maven, Windows CMD, Claude Code Agent Skills

---

## File Structure

- Create: `docs/global-installation-guide.md` — 전역 스킬, CLI, JDBC 드라이버, PATH, Oracle 설정, 검증·업데이트·제거 상세 절차
- Modify: `README.md` — 전역 설치 소개와 상세 문서 링크
- Reference: `modelconvertor.cmd` — JAR와 드라이버를 같은 디렉터리에서 읽는 현재 classpath 계약
- Reference: `.claude/skills/modelconvertor/SKILL.md` — 전역 위치로 복사할 스킬 원본

### Task 1: 전역 설치 상세 가이드 작성

**Files:**
- Create: `docs/global-installation-guide.md`

- [ ] **Step 1: 목적, 전제 조건, 설치 구조를 작성한다**

다음 내용을 포함한다.

````markdown
# ModelConvertor 전역 설치 가이드

이 가이드는 Windows에서 ModelConvertor CLI와 Claude Code 스킬을 전역으로 설치해 여러 Java 프로젝트에서 사용하는 절차를 설명합니다.

## 준비 사항

- Git
- Java 8 이상
- Maven 3.x
- Oracle JDBC 드라이버 `ojdbc8.jar`
- Claude Code
- 접근 가능한 ModelConvertor Git 저장소

Oracle JDBC 드라이버는 저장소와 애플리케이션 JAR에 포함되지 않습니다. 사용 권한이 있는 드라이버를 직접 준비하세요.

## 설치 구조

```text
C:\tools\modelconvertor\
├─ modelconvertor.cmd
├─ modelconvertor.jar
└─ ojdbc8.jar

%USERPROFILE%\.claude\skills\modelconvertor\
└─ SKILL.md

%USERPROFILE%\.modelconvertor\
└─ oracle.properties
```

소스 저장소, CLI 설치 폴더, 전역 스킬 및 실제 Java 프로젝트는 서로 독립된 위치입니다.
````

- [ ] **Step 2: 저장소 준비와 빌드 절차를 작성한다**

저장소 주소는 사용자가 실제 주소로 교체하도록 명시하고 다음 PowerShell 예시를 제공한다.

```powershell
$repo = 'C:\study\modelconvertor'
git clone <MODEL_CONVERTOR_GIT_URL> $repo
Set-Location $repo
mvn clean package
```

이미 clone한 저장소는 `git pull --ff-only` 후 다시 빌드한다고 설명한다. 성공 산출물이 `$repo\target\modelconvertor.jar`인지 확인하도록 안내한다.

- [ ] **Step 3: CLI와 JDBC 드라이버 배치 절차를 작성한다**

다음 PowerShell 예시와 안전 설명을 제공한다.

```powershell
$repo = 'C:\study\modelconvertor'
$installDir = 'C:\tools\modelconvertor'
$ojdbc = 'C:\path\to\ojdbc8.jar'

New-Item -ItemType Directory -Force -Path $installDir | Out-Null
Copy-Item -LiteralPath "$repo\modelconvertor.cmd" -Destination $installDir
Copy-Item -LiteralPath "$repo\target\modelconvertor.jar" -Destination $installDir
Copy-Item -LiteralPath $ojdbc -Destination "$installDir\ojdbc8.jar"
```

실행 전에 각 원본 파일의 존재를 확인하고 대상 파일이 이미 있으면 중단해 백업 또는 덮어쓰기 여부를 결정하도록 안내한다. 대상 Java 프로젝트의 Maven 의존성이나 `lib` 폴더에 있는 드라이버는 자동으로 classpath에 포함되지 않으므로, 재사용할 때도 CLI 폴더로 복사한다고 명시한다.

- [ ] **Step 4: 사용자 PATH 등록 절차를 작성한다**

기존 사용자 PATH를 읽고 중복이 없을 때만 추가하는 예시를 제공한다.

```powershell
$installDir = 'C:\tools\modelconvertor'
$userPath = [Environment]::GetEnvironmentVariable('Path', 'User')
$entries = @($userPath -split ';' | Where-Object { $_ })
if ($entries -notcontains $installDir) {
    [Environment]::SetEnvironmentVariable('Path', (($entries + $installDir) -join ';'), 'User')
}
```

새 터미널을 연 뒤 `modelconvertor.cmd --help`로 확인하도록 안내한다. PATH 전체를 새 값으로 교체하지 말라고 경고한다.

- [ ] **Step 5: Claude Code 전역 스킬 설치 절차를 작성한다**

다음 경로와 명령을 제공한다.

```powershell
$repo = 'C:\study\modelconvertor'
$globalSkill = Join-Path $env:USERPROFILE '.claude\skills\modelconvertor'

if (Test-Path $globalSkill) {
    throw "Global skill already exists: $globalSkill"
}
New-Item -ItemType Directory -Force -Path $globalSkill | Out-Null
Copy-Item -LiteralPath "$repo\.claude\skills\modelconvertor\SKILL.md" -Destination $globalSkill
```

설치 후 새 Claude Code 세션을 시작하고 `/modelconvertor`로 확인하도록 안내한다. 프로젝트 스킬 원본과 전역 복사본은 자동 동기화되지 않는다고 명시한다.

- [ ] **Step 6: Oracle 설정 절차를 작성한다**

`%USERPROFILE%\.modelconvertor\oracle.properties`의 네 필수 키 예시를 제공하되 실제 비밀번호를 사용하지 않는다. 기존 파일이 있으면 덮어쓰지 않고 재사용 또는 백업하도록 안내한다. 설정 파일을 Git에 추가하거나 채팅에 붙여넣지 말라고 경고한다.

- [ ] **Step 7: 다른 Java 프로젝트에서의 검증과 사용 예시를 작성한다**

다음 흐름을 제공한다.

```powershell
Set-Location C:\work\hr-system
modelconvertor.cmd --help
claude
```

```text
/modelconvertor employee.sql로 EmployeeModel을 만들어줘. 패키지는 com.company.hr.model이야.
```

기본 출력이 `C:\work\hr-system\src\main\java\com\company\hr\model\EmployeeModel.java`임을 설명한다. 실제 Oracle E2E 확인 전에는 `--stdout` 요청으로 생성 결과를 먼저 검토하도록 권장한다.

- [ ] **Step 8: 업데이트, 제거, 오류 확인 절차를 작성한다**

업데이트는 저장소 갱신·재빌드 후 CLI 파일과 전역 `SKILL.md`를 사용자 확인 아래 다시 복사하도록 작성한다. 제거는 전역 스킬, 사용자 PATH 항목, CLI 설치 폴더, Oracle 설정 파일을 별개 대상으로 설명하며 Oracle 설정은 명시적 요청 없이는 삭제하지 않도록 경고한다.

다음 오류 확인 표를 포함한다.

| 증상 | 확인 사항 |
|---|---|
| `modelconvertor.cmd`를 찾을 수 없음 | 새 터미널, 사용자 PATH, CLI 설치 폴더 |
| `Could not find or load main class` | CLI 폴더의 `modelconvertor.jar` |
| `No suitable driver` | CLI 폴더의 `ojdbc8.jar` |
| Oracle 설정 오류 | `%USERPROFILE%\.modelconvertor\oracle.properties` 경로와 필수 키 |
| 모델이 잘못된 위치에 생성됨 | Claude Code를 시작한 현재 디렉터리 또는 `--output` |
| `/modelconvertor`가 보이지 않음 | 전역 `SKILL.md` 경로와 새 Claude Code 세션 |

- [ ] **Step 9: 상세 가이드 필수 내용을 검증한다**

Run:

```powershell
$path = 'docs/global-installation-guide.md'
$text = Get-Content -Raw -Encoding UTF8 $path
foreach ($term in @('C:\tools\modelconvertor','ojdbc8.jar','.claude\skills\modelconvertor','SetEnvironmentVariable','oracle.properties','업데이트','제거','No suitable driver')) {
    if (-not $text.Contains($term)) { throw "Missing guide term: $term" }
}
git diff --check -- $path
```

Expected: 출력 없이 종료 코드 `0`.

- [ ] **Step 10: 상세 가이드를 커밋한다**

```powershell
git add -- docs/global-installation-guide.md
git diff --cached --check
git commit -m "docs: add global installation guide"
```

### Task 2: README 전역 설치 진입점 추가

**Files:**
- Modify: `README.md:43-59`
- Modify: `README.md`의 `## 상세 문서` 목록

- [ ] **Step 1: 설치 섹션에 전역 사용 안내를 추가한다**

기존 로컬 CLI 설치 설명 뒤에 다음 문단을 추가한다.

```markdown
여러 Java 프로젝트에서 Claude Code 스킬로 사용하려면 CLI 폴더를 사용자 `PATH`에 추가하고 스킬을 사용자 전역 위치에 설치합니다. 자세한 절차는 [전역 설치 가이드](docs/global-installation-guide.md)를 참고하세요.
```

- [ ] **Step 2: 상세 문서 목록에 링크를 추가한다**

```markdown
- [전역 설치 가이드](docs/global-installation-guide.md)
```

- [ ] **Step 3: 링크와 문서 형식을 검증한다**

Run:

```powershell
$readme = Get-Content -Raw -Encoding UTF8 README.md
if (-not $readme.Contains('(docs/global-installation-guide.md)')) { throw 'README global guide link is missing' }
if (-not (Test-Path 'docs/global-installation-guide.md')) { throw 'Global guide file is missing' }
git diff --check -- README.md docs/global-installation-guide.md
```

Expected: 출력 없이 종료 코드 `0`.

- [ ] **Step 4: README 변경을 커밋한다**

```powershell
git add -- README.md
git diff --cached --check
git commit -m "docs: link global installation guide"
```

### Task 3: 최종 대조 검증

**Files:**
- Verify: `README.md`
- Verify: `docs/global-installation-guide.md`
- Reference: `modelconvertor.cmd`
- Reference: `.claude/skills/modelconvertor/SKILL.md`

- [ ] **Step 1: 설치 계약을 구현과 대조한다**

다음을 직접 확인한다.

- `modelconvertor.cmd`가 `%~dp0modelconvertor.jar`와 `%~dp0ojdbc8.jar`를 classpath에 사용한다.
- Maven JAR 이름이 `target/modelconvertor.jar`다.
- 프로젝트 스킬 원본이 `.claude/skills/modelconvertor/SKILL.md`에 존재한다.
- 가이드가 Codex 전역 설치를 완료된 기능처럼 안내하지 않는다.

- [ ] **Step 2: 비밀값과 placeholder를 검사한다**

Run:

```powershell
$files = @('README.md','docs/global-installation-guide.md')
$matches = rg -n "TODO|TBD|sk-[A-Za-z0-9]" $files
if ($LASTEXITCODE -eq 0) { Write-Output $matches; throw 'Placeholder or secret-like value found' }
if ($LASTEXITCODE -ne 1) { throw "rg failed: $LASTEXITCODE" }
$passwordLines = rg -n "oracle.password=" $files
if ($LASTEXITCODE -notin 0,1) { throw "rg failed: $LASTEXITCODE" }
$unexpected = $passwordLines | Where-Object { $_ -notmatch 'oracle\.password=MY_PASSWORD' }
if ($unexpected) { Write-Output $unexpected; throw 'Unexpected Oracle password example found' }
```

Expected: 출력 없이 종료 코드 `0`.

- [ ] **Step 3: 전체 테스트와 작업 트리를 확인한다**

Run:

```powershell
& 'C:\Users\admin\AppData\Local\Programs\IntelliJ IDEA\plugins\maven\lib\maven3\bin\mvn.cmd' test
git status --short
```

Expected: 59 tests, 0 failures, 0 errors. 이번 문서 변경은 모두 커밋됐고 기존 사용자 변경만 남아 있다.
