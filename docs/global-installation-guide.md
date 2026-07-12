# ModelConvertor 전역 설치 가이드

이 가이드는 Windows에서 ModelConvertor CLI와 Claude Code 스킬을 전역으로 설치해 여러 Java 프로젝트에서 사용하는 절차를 설명합니다.

## 준비 사항

- Git
- Java 8 이상
- Maven 3.x
- Oracle JDBC 드라이버 `ojdbc8.jar`
- Claude Code
- [ModelConvertor Git 저장소](https://github.com/twin10240/sql-model-gen)

Oracle JDBC 드라이버는 저장소와 애플리케이션 JAR에 포함되지 않습니다. 사용 권한이 있는 드라이버를 직접 준비하세요.

## 설치 구조

이 가이드에서는 다음 위치를 기본값으로 사용합니다.

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

## 1. 저장소 준비와 JAR 빌드

처음 설치한다면 저장소를 clone하고 JAR를 빌드합니다.

```powershell
$repo = 'C:\study\modelconvertor'
git clone https://github.com/twin10240/sql-model-gen.git $repo
Set-Location $repo
mvn clean package
```

이미 clone한 저장소가 있다면 fast-forward로 갱신한 후 다시 빌드합니다. 커밋하지 않은 변경이 있다면 먼저 보존하세요.

```powershell
$repo = 'C:\study\modelconvertor'
Set-Location $repo
git pull --ff-only
mvn clean package
```

빌드가 끝나면 다음 파일이 있어야 합니다.

```text
C:\study\modelconvertor\target\modelconvertor.jar
```

## 2. CLI와 JDBC 드라이버 설치

설치에 사용할 경로를 지정하고 원본 파일이 모두 있는지 확인합니다. `$ojdbc`는 실제 드라이버 위치로 변경하세요.

```powershell
$repo = 'C:\study\modelconvertor'
$installDir = 'C:\tools\modelconvertor'
$ojdbc = 'C:\path\to\ojdbc8.jar'

$sources = @(
    "$repo\modelconvertor.cmd",
    "$repo\target\modelconvertor.jar",
    $ojdbc
)
$missing = $sources | Where-Object { -not (Test-Path -LiteralPath $_) }
if ($missing) {
    throw "Missing installation file: $($missing -join ', ')"
}
```

대상 파일이 이미 있다면 설치를 중단하고 백업 또는 업데이트 여부를 먼저 결정합니다.

```powershell
$targets = @(
    "$installDir\modelconvertor.cmd",
    "$installDir\modelconvertor.jar",
    "$installDir\ojdbc8.jar"
)
$existing = $targets | Where-Object { Test-Path -LiteralPath $_ }
if ($existing) {
    throw "Installation target already exists: $($existing -join ', ')"
}
```

대상 파일이 없다는 것을 확인한 후 세 파일을 같은 폴더에 설치합니다.

```powershell
New-Item -ItemType Directory -Force -Path $installDir | Out-Null
Copy-Item -LiteralPath "$repo\modelconvertor.cmd" -Destination $installDir
Copy-Item -LiteralPath "$repo\target\modelconvertor.jar" -Destination $installDir
Copy-Item -LiteralPath $ojdbc -Destination "$installDir\ojdbc8.jar"
```

`modelconvertor.cmd`는 자신의 폴더에서 `modelconvertor.jar`와 `ojdbc8.jar`를 찾습니다. 대상 Java 프로젝트의 Maven 의존성이나 `lib` 폴더에 있는 드라이버는 자동으로 사용되지 않습니다. 다른 프로젝트에 드라이버가 이미 있다면 사용 권한과 버전을 확인한 뒤 위 `$ojdbc` 경로로 지정해 CLI 폴더에 복사하세요.

## 3. 사용자 PATH 등록

현재 사용자 PATH를 읽고 CLI 설치 폴더가 없을 때만 추가합니다.

```powershell
$installDir = 'C:\tools\modelconvertor'
$userPath = [Environment]::GetEnvironmentVariable('Path', 'User')
$entries = @($userPath -split ';' | Where-Object { $_ })
if ($entries -notcontains $installDir) {
    [Environment]::SetEnvironmentVariable(
        'Path',
        (($entries + $installDir) -join ';'),
        'User'
    )
}
```

기존 PATH 전체를 새 값으로 교체하지 마세요. 등록 후 새 터미널을 열고 어느 디렉터리에서든 CLI가 실행되는지 확인합니다.

```powershell
modelconvertor.cmd --help
```

## 4. Claude Code 전역 스킬 설치

저장소에 포함된 스킬을 사용자 전역 스킬 폴더로 복사합니다. 기존 전역 스킬이 있다면 확인 없이 덮어쓰지 않습니다.

```powershell
$repo = 'C:\study\modelconvertor'
$globalSkill = Join-Path $env:USERPROFILE '.claude\skills\modelconvertor'

if (Test-Path -LiteralPath $globalSkill) {
    throw "Global skill already exists: $globalSkill"
}
New-Item -ItemType Directory -Force -Path $globalSkill | Out-Null
Copy-Item `
    -LiteralPath "$repo\.claude\skills\modelconvertor\SKILL.md" `
    -Destination $globalSkill
```

전역 스킬 위치는 다음과 같습니다.

```text
%USERPROFILE%\.claude\skills\modelconvertor\SKILL.md
```

설치 후 새 Claude Code 세션을 시작하세요. 저장소의 프로젝트 스킬과 전역으로 복사한 스킬은 자동으로 동기화되지 않습니다.

현재 저장소에는 Claude Code 스킬만 제공됩니다. Codex 전역 설치는 Codex용 스킬 파일이 추가된 이후 별도로 안내합니다.

## 5. Oracle 접속 설정

기본 설정 파일은 다음 위치에 둡니다.

```text
%USERPROFILE%\.modelconvertor\oracle.properties
```

설정 예시:

```properties
oracle.url=jdbc:oracle:thin:@DB_HOST:1521/SERVICE_NAME
oracle.username=MY_USER
oracle.password=MY_PASSWORD
oracle.schema=MY_SCHEMA
```

기존 설정 파일이 있으면 덮어쓰지 말고 필요한 환경의 설정인지 먼저 확인하세요. 설정 파일에는 비밀번호가 평문으로 들어가므로 다음 원칙을 지키세요.

- Git에 추가하지 않습니다.
- Claude Code 대화에 내용을 붙여 넣지 않습니다.
- 명령줄 인자로 비밀번호를 전달하지 않습니다.
- 가능한 경우 조회 전용 Oracle 계정을 사용합니다.

## 6. 다른 Java 프로젝트에서 사용

모델을 생성할 대상 Java 프로젝트 루트에서 Claude Code를 시작합니다.

```powershell
Set-Location C:\work\hr-system
modelconvertor.cmd --help
claude
```

Claude Code에서 스킬을 명시적으로 호출할 수 있습니다.

```text
/modelconvertor employee.sql로 EmployeeModel을 만들어줘. 패키지는 com.company.hr.model이야.
```

출력 위치를 지정하지 않으면 현재 디렉터리를 기준으로 다음 파일을 생성합니다.

```text
C:\work\hr-system\src\main\java\com\company\hr\model\EmployeeModel.java
```

처음에는 파일을 생성하지 말고 결과만 확인하는 것을 권장합니다.

```text
/modelconvertor employee.sql로 EmployeeModel을 만들되 파일은 쓰지 말고 결과만 보여줘. 패키지는 com.company.hr.model이야.
```

## 7. 설치 확인

다음 항목을 순서대로 확인합니다.

1. 새 PowerShell에서 `modelconvertor.cmd --help`가 종료 코드 `0`으로 끝납니다.
2. `%USERPROFILE%\.claude\skills\modelconvertor\SKILL.md`가 존재합니다.
3. 대상 Java 프로젝트에서 새 Claude Code 세션을 시작합니다.
4. `/modelconvertor`가 스킬로 인식됩니다.
5. 실제 Oracle 연결 전에 `--stdout` 요청으로 명령 구성과 생성 결과를 확인합니다.
6. 실제 Oracle SELECT로 모델 생성을 확인합니다.

## 8. 업데이트

저장소를 갱신하고 JAR를 다시 빌드한 뒤 CLI 파일과 전역 스킬을 업데이트합니다.

```powershell
$repo = 'C:\study\modelconvertor'
Set-Location $repo
git pull --ff-only
mvn clean package
```

업데이트 전에 현재 설치 파일과 전역 `SKILL.md`를 백업하고 변경 내용을 검토하세요. 확인 후 다음 파일을 새 버전으로 교체합니다.

- `C:\tools\modelconvertor\modelconvertor.cmd`
- `C:\tools\modelconvertor\modelconvertor.jar`
- `%USERPROFILE%\.claude\skills\modelconvertor\SKILL.md`

`ojdbc8.jar`는 JDBC 드라이버 버전을 변경할 때만 교체합니다. 실행 중인 Claude Code 세션이 있다면 스킬 업데이트 후 새 세션을 시작하세요.

## 9. 제거

전역 스킬, PATH, CLI 설치 폴더와 Oracle 설정 파일은 서로 독립적입니다. 필요한 범위만 제거하세요.

### 전역 Claude 스킬

```text
%USERPROFILE%\.claude\skills\modelconvertor
```

### 사용자 PATH 항목

현재 사용자 PATH에서 정확히 `C:\tools\modelconvertor`와 일치하는 항목만 제거합니다. 변경 전에 현재 PATH를 백업하세요.

```powershell
$installDir = 'C:\tools\modelconvertor'
$userPath = [Environment]::GetEnvironmentVariable('Path', 'User')
$entries = @($userPath -split ';' | Where-Object { $_ -and $_ -ne $installDir })
[Environment]::SetEnvironmentVariable('Path', ($entries -join ';'), 'User')
```

### CLI 설치 폴더

```text
C:\tools\modelconvertor
```

폴더 안에 사용자 파일이 없는지 확인한 뒤 제거하세요.

### Oracle 설정

```text
%USERPROFILE%\.modelconvertor\oracle.properties
```

이 파일은 접속 정보를 포함합니다. ModelConvertor를 제거하더라도 사용자가 명시적으로 원할 때만 삭제하세요.

## 오류 확인

| 증상 | 확인 사항 |
|---|---|
| `modelconvertor.cmd`를 찾을 수 없음 | 새 터미널을 열었는지, 사용자 PATH와 CLI 설치 폴더가 맞는지 확인 |
| `Could not find or load main class` | CLI 폴더에 `modelconvertor.jar`가 있는지 확인 |
| `No suitable driver` | CLI 폴더에 올바른 `ojdbc8.jar`가 있는지 확인 |
| Oracle 설정 오류 | `%USERPROFILE%\.modelconvertor\oracle.properties` 경로와 네 필수 키 확인 |
| 모델이 잘못된 위치에 생성됨 | Claude Code를 시작한 현재 디렉터리 또는 `--output` 확인 |
| `/modelconvertor`가 보이지 않음 | 전역 `SKILL.md` 경로를 확인하고 새 Claude Code 세션 시작 |
