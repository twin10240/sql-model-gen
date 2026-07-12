# ModelConvertor Fat JAR 및 Release 배포 구현 계획

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** `ojdbc8`을 포함한 단일 실행 JAR(fat jar)을 빌드하고, 소스 트리 커밋 없이 GitHub Release 에셋으로 배포하도록 패키징과 설치 절차를 바꾼다. CLI 동작·SQL·오류 로직은 바꾸지 않는다.

**Architecture:** `pom.xml`에 `ojdbc8` runtime 의존성과 `maven-shade-plugin`을 추가해 fat jar를 만든다. `modelconvertor.cmd`는 `java -jar`로 단순화한다. 설치는 clone(가벼운 텍스트) + Release 다운로드(무거운 jar)로 분리한다.

**Tech Stack:** Java 8 (release 8), Maven, maven-shade-plugin, Oracle `ojdbc8`(Maven Central), PowerShell, GitHub Release

**Reference spec:** `docs/superpowers/specs/2026-07-12-fatjar-release-distribution-design.md`

**환경 메모:**
- 빌드용 Maven: `C:\Users\admin\AppData\Local\Programs\IntelliJ IDEA\plugins\maven\lib\maven3\bin\mvn.cmd`
- 현재 PC의 `java`는 17이며 `release 8` 산출물 실행에 문제없다.
- `gh` 미설치 → Release 발행(Task 5)은 `gh` 설치 또는 GitHub 웹 UI로 사용자가 수행한다.

---

## File Structure

- Modify: `pom.xml` — `ojdbc8` 의존성 + `maven-shade-plugin`
- Modify: `modelconvertor.cmd` — `java -jar` 단일 실행
- Create: `install.ps1` — clone 후 Release 에셋을 내려받아 설치
- Modify (검증 후): `README.md`, `docs/global-installation-guide.md` — 설치·드라이버 안내를 fat jar/Release 기준으로 재조정
- Reference: `docs/superpowers/specs/2026-07-12-fatjar-release-distribution-design.md`

### Task 1: pom.xml에 ojdbc8 의존성과 shade 추가

**Files:**
- Modify: `pom.xml`

- [ ] **Step 1: `ojdbc8` runtime 의존성을 추가한다**

`<dependencies>`에 다음을 추가한다. 버전은 JDK8 호환 ojdbc8 최신값으로 확정한다.

```xml
<dependency>
  <groupId>com.oracle.database.jdbc</groupId>
  <artifactId>ojdbc8</artifactId>
  <version>21.11.0.0</version>
  <scope>runtime</scope>
</dependency>
```

- [ ] **Step 2: `maven-shade-plugin`을 추가한다**

`<plugins>`에 추가한다. `ServicesResourceTransformer`는 드라이버 등록에 필수다.

```xml
<plugin>
  <groupId>org.apache.maven.plugins</groupId>
  <artifactId>maven-shade-plugin</artifactId>
  <version>3.5.1</version>
  <executions>
    <execution>
      <phase>package</phase>
      <goals><goal>shade</goal></goals>
      <configuration>
        <transformers>
          <transformer implementation="org.apache.maven.plugins.shade.resource.ManifestResourceTransformer">
            <mainClass>org.sqlmodel.Main</mainClass>
          </transformer>
          <transformer implementation="org.apache.maven.plugins.shade.resource.ServicesResourceTransformer"/>
        </transformers>
      </configuration>
    </execution>
  </executions>
</plugin>
```

- [ ] **Step 3: pom이 유효한지 확인한다**

Run:

```powershell
& 'C:\Users\admin\AppData\Local\Programs\IntelliJ IDEA\plugins\maven\lib\maven3\bin\mvn.cmd' -q validate
```

Expected: 종료 코드 `0`.

### Task 2: modelconvertor.cmd 단순화

**Files:**
- Modify: `modelconvertor.cmd`

- [ ] **Step 1: `java -jar` 단일 실행으로 바꾼다**

```bat
@echo off
setlocal
set "APP_DIR=%~dp0"
java -jar "%APP_DIR%modelconvertor.jar" %*
exit /b %ERRORLEVEL%
```

`-cp`와 외부 `ojdbc8.jar` 참조를 제거한다.

### Task 3: 빌드와 드라이버 포함 검증

**Files:**
- Verify only

- [ ] **Step 1: fat jar를 빌드한다**

Run:

```powershell
& 'C:\Users\admin\AppData\Local\Programs\IntelliJ IDEA\plugins\maven\lib\maven3\bin\mvn.cmd' clean package
```

Expected: `BUILD SUCCESS`, `target/modelconvertor.jar` 생성(드라이버 포함으로 수 MB).

- [ ] **Step 2: 실행 진입을 확인한다**

Run:

```powershell
java -jar target\modelconvertor.jar --help
```

Expected: 종료 코드 `0`과 usage 출력.

- [ ] **Step 3: 드라이버 포함 게이트를 확인한다**

접속 불가한 임시 설정으로 접속을 시도해, 오류가 드라이버 부재(`No suitable driver`)가 아니라 접속 실패인지 확인한다.

Run:

```powershell
$cfg = Join-Path $env:TEMP 'mc-fake-oracle.properties'
@'
oracle.url=jdbc:oracle:thin:@127.0.0.1:1599/NOPE
oracle.username=u
oracle.password=p
oracle.schema=S
'@ | Set-Content -Encoding UTF8 $cfg
'SELECT 1 AS X FROM DUAL' | java -jar target\modelconvertor.jar --class-name T --package p.q --config $cfg --stdout 2>&1 | Out-String -OutVariable out | Out-Null
if ($out -match 'No suitable driver') { throw 'FAIL: driver not bundled (shade ServiceLoader merge missing)' }
"OK: driver bundled (error is connection-level, not driver lookup)"
```

Expected: `No suitable driver`가 없고 접속/처리 실패 메시지가 나온다.

- [ ] **Step 4: 기존 테스트를 실행한다**

Run:

```powershell
& 'C:\Users\admin\AppData\Local\Programs\IntelliJ IDEA\plugins\maven\lib\maven3\bin\mvn.cmd' test
```

Expected: 59 tests, 0 failures, 0 errors. (shade는 패키징만 바꾸므로 로직 테스트 무영향)

- [ ] **Step 5: pom과 cmd 변경을 커밋한다**

```powershell
git add -- pom.xml modelconvertor.cmd
git diff --cached --check
git commit -m "feat: bundle ojdbc8 into fat jar via shade"
```

### Task 4: install.ps1 작성 (Release 다운로드 설치)

**Files:**
- Create: `install.ps1`

- [ ] **Step 1: 설치 스크립트를 만든다**

동작: CLI 폴더 생성 → Release에서 jar 다운로드 → `modelconvertor.cmd` 배치 → 사용자 PATH 등록(멱등) → `SKILL.md` 전역 복사 → `oracle.properties` 존재 확인. 기존 대상이 있으면 확인 없이 덮어쓰지 않는다.

- 공개 저장소: `https://github.com/<owner>/<repo>/releases/latest/download/modelconvertor.jar`를 `Invoke-WebRequest`로 받는다.
- 비공개 저장소: `gh release download --repo <owner>/<repo> --pattern modelconvertor.jar`를 사용한다(해당 PC에서 `gh auth login` 1회 필요).

- [ ] **Step 2: 스크립트를 구문 검사한다**

Run:

```powershell
$null = [System.Management.Automation.Language.Parser]::ParseFile((Resolve-Path .\install.ps1), [ref]$null, [ref]$null)
"parse OK"
```

Expected: `parse OK`, 종료 코드 `0`.

### Task 5: Release 발행 절차

**Files:**
- Verify only(문서화된 절차)

- [ ] **Step 1: 태그와 에셋으로 Release를 만든다**

`gh` 설치 후:

```powershell
gh release create v1.0.0 target\modelconvertor.jar --title "ModelConvertor v1.0.0" --notes "Fat jar with bundled ojdbc8"
```

`gh`가 없으면 GitHub 웹의 Releases에서 태그를 만들고 `target\modelconvertor.jar`를 첨부한다.

- [ ] **Step 2: 다운로드 URL을 확인한다**

Run:

```powershell
Invoke-WebRequest -Uri 'https://github.com/<owner>/<repo>/releases/latest/download/modelconvertor.jar' -OutFile "$env:TEMP\mc-verify.jar"
(Get-Item "$env:TEMP\mc-verify.jar").Length -gt 0
```

Expected: `True`(파일이 실제로 받아진다).

### Task 6: 문서 재조정 (검증 후)

**Files:**
- Modify: `README.md`
- Modify: `docs/global-installation-guide.md`

- [ ] **Step 1: README를 fat jar/Release 기준으로 갱신한다**

빌드·설치·드라이버 절을 `java -jar`/Release 다운로드로 바꾸고, 외부 `ojdbc8.jar` 배치 문구를 제거한다.

- [ ] **Step 2: 전역 설치 가이드의 설치 흐름을 갱신한다**

"2. CLI와 JDBC 드라이버 설치"를 Release 다운로드로 교체하고, 세 파일 복사 대신 cmd + 다운로드한 jar 구성으로 바꾼다. `install.ps1` 사용을 안내한다.

- [ ] **Step 3: 문서 변경을 커밋한다**

```powershell
git add -- README.md docs/global-installation-guide.md install.ps1
git diff --cached --check
git commit -m "docs: switch install flow to fat jar release download"
```

## 완료 기준

- `target/modelconvertor.jar`가 fat jar로 빌드되고 `java -jar ... --help`가 종료 코드 0이다.
- 드라이버가 jar에 포함되어 `No suitable driver`가 발생하지 않는다.
- 기존 59개 테스트가 통과한다.
- 소스 트리에 jar 바이너리를 커밋하지 않는다(Release 에셋으로만 배포).
- 실제 Oracle 접속 E2E는 본 계획 범위 밖이며, 접속 가능한 환경이 준비되면 별도로 확인한다.
