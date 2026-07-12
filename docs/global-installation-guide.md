# ModelConvertor 전역 설치 가이드

Windows에서 ModelConvertor CLI와 Claude Code 스킬을 전역으로 설치해 여러 Java 프로젝트에서 사용하는 절차입니다. 실행 JAR에는 Oracle JDBC 드라이버(`ojdbc8`)가 포함되어 있어 별도 드라이버 파일이 필요 없습니다.

## 준비 사항

- Java 8 이상(실행 런타임)
- Git
- Claude Code
- [ModelConvertor Git 저장소](https://github.com/twin10240/sql-model-gen)

실행 JAR은 소스 저장소에 커밋하지 않고 GitHub Release 에셋으로 배포합니다. 저장소를 직접 빌드할 때만 Maven 3.x가 추가로 필요합니다(아래 "직접 빌드" 참고).

## 설치 구조

```text
C:\tools\modelconvertor\
├─ modelconvertor.cmd
└─ modelconvertor.jar        (ojdbc8 포함, Release에서 다운로드)

%USERPROFILE%\.claude\skills\modelconvertor\
└─ SKILL.md                  (전역 스킬)

%USERPROFILE%\.modelconvertor\
└─ oracle.properties         (접속 정보, 직접 작성)
```

CLI 설치 폴더, 전역 스킬, Oracle 설정, 소스 저장소는 서로 독립적입니다.

## 빠른 설치

저장소를 clone한 뒤 포함된 `install.ps1`을 실행합니다.

```powershell
git clone https://github.com/twin10240/sql-model-gen.git C:\tools\modelconvertor-repo
Set-Location C:\tools\modelconvertor-repo
.\install.ps1
```

설치 후 **새 Claude Code 세션**을 시작하면 스킬이 인식됩니다.

### install.ps1이 하는 일

- 최신 Release에서 `modelconvertor.jar`를 내려받아 CLI 폴더(`C:\tools\modelconvertor`)에 둡니다.
- 저장소의 `modelconvertor.cmd`를 같은 폴더에 복사합니다.
- CLI 폴더를 사용자 `PATH`에 추가합니다(이미 있으면 건너뜀).
- 저장소의 `SKILL.md`를 전역 스킬 폴더로 복사합니다.
- `oracle.properties`가 없으면 경고합니다(비밀번호가 평문이므로 직접 만들어야 합니다).
- Java 런타임이 PATH에 없으면 경고합니다.

기존 설치나 전역 스킬이 있으면 확인 없이 덮어쓰지 않습니다. 갱신하려면 `-Force`를 사용합니다.

```powershell
.\install.ps1 -Force
```

주요 매개변수: `-Repo <owner/repo>`, `-Version <tag|latest>`, `-InstallDir <path>`, `-Force`.

### 비공개 저장소인 경우

Release 에셋에 인증이 필요하면 익명 HTTP 다운로드가 실패합니다. `gh` CLI로 로그인한 뒤 `-UseGh`로 실행하면 `install.ps1`이 인증된 `gh release download`로 에셋을 받아 그대로 설치합니다.

```powershell
gh auth login
.\install.ps1 -UseGh
```

### 버전 고정

기본값은 최신 Release(`latest`)입니다. 재현 가능한 설치가 필요하면 태그를 고정하세요.

```powershell
.\install.ps1 -Version v1.0.0
```

## 수동 설치 (fallback)

`install.ps1`을 쓰지 않을 때의 최소 절차입니다.

1. 최신 Release에서 `modelconvertor.jar`를 받아 `C:\tools\modelconvertor`에 둔다.
   - 공개: `https://github.com/twin10240/sql-model-gen/releases/latest/download/modelconvertor.jar`
2. 저장소의 `modelconvertor.cmd`를 같은 폴더에 복사한다.
3. `C:\tools\modelconvertor`를 사용자 `PATH`에 추가한다.
4. 저장소의 `.claude\skills\modelconvertor\SKILL.md`를 `%USERPROFILE%\.claude\skills\modelconvertor\`로 복사한다.
5. `%USERPROFILE%\.modelconvertor\oracle.properties`를 작성한다(아래 참고).

## 직접 빌드 (선택)

Release 대신 소스에서 JAR을 만들려면 Maven이 필요합니다.

```powershell
Set-Location C:\tools\modelconvertor-repo
mvn clean package
```

`target\modelconvertor.jar`(ojdbc8 포함)가 생성됩니다. 이후 절차는 수동 설치 2~5와 같습니다.

## Oracle 접속 설정

기본 설정 파일 위치입니다.

```text
%USERPROFILE%\.modelconvertor\oracle.properties
```

```properties
oracle.url=jdbc:oracle:thin:@DB_HOST:1521/SERVICE_NAME
oracle.username=MY_USER
oracle.password=MY_PASSWORD
oracle.schema=MY_SCHEMA
```

기존 설정 파일이 있으면 덮어쓰지 말고 필요한 환경인지 먼저 확인하세요. 비밀번호가 평문으로 들어가므로 다음 원칙을 지키세요.

- Git에 추가하지 않습니다.
- Claude Code 대화에 내용을 붙여 넣지 않습니다.
- 명령줄 인자로 비밀번호를 전달하지 않습니다.
- 가능하면 조회 전용 Oracle 계정을 사용합니다.

다른 설정 파일을 쓰려면 CLI에 `--config <path>`를 지정합니다.

## 다른 Java 프로젝트에서 사용

모델을 생성할 프로젝트 루트에서 Claude Code를 시작합니다.

```powershell
Set-Location C:\work\hr-system
modelconvertor.cmd --help
claude
```

스킬을 명시적으로 호출할 수 있습니다.

```text
/modelconvertor employee.sql로 EmployeeModel을 만들어줘. 패키지는 com.company.hr.model이야.
```

처음에는 파일을 만들지 말고 `--stdout`으로 결과만 확인하는 것을 권장합니다. `--stdout`도 유효한 Oracle 설정과 연결이 필요하며 파일 쓰기만 생략합니다.

## 설치 확인

1. 새 PowerShell에서 `modelconvertor.cmd --help`가 종료 코드 `0`으로 끝난다.
2. `%USERPROFILE%\.claude\skills\modelconvertor\SKILL.md`가 존재한다.
3. 대상 Java 프로젝트에서 새 Claude Code 세션을 시작한다.
4. `/modelconvertor`가 스킬로 인식된다.
5. 유효한 Oracle 설정과 연결을 준비한 뒤 `--stdout`으로 생성 결과를 확인한다.

## 업데이트

새 Release가 올라오면 저장소를 갱신하고 `install.ps1`을 다시 실행합니다.

```powershell
Set-Location C:\tools\modelconvertor-repo
git pull --ff-only
.\install.ps1 -Force
```

`install.ps1`이 최신 Release JAR을 다시 내려받고 `modelconvertor.cmd`와 전역 `SKILL.md`를 갱신합니다. 실행 중인 Claude Code 세션이 있으면 새 세션을 시작하세요.

## 제거

전역 스킬, PATH, CLI 폴더, Oracle 설정은 서로 독립적입니다. 필요한 범위만 제거하세요.

- 전역 스킬: `%USERPROFILE%\.claude\skills\modelconvertor`
- 사용자 PATH: `C:\tools\modelconvertor` 항목만 제거(변경 전 백업 권장)
- CLI 폴더: `C:\tools\modelconvertor`
- Oracle 설정: `%USERPROFILE%\.modelconvertor\oracle.properties` — 접속 정보를 포함하므로 명시적으로 원할 때만 삭제

## 오류 확인

| 증상 | 확인 사항 |
|---|---|
| `modelconvertor.cmd`를 찾을 수 없음 | 새 터미널을 열었는지, 사용자 PATH에 CLI 폴더가 있는지 확인 |
| `install.ps1` 다운로드 실패 | Release가 발행됐는지, 비공개 저장소면 `gh auth login` 후 `-UseGh`로 실행했는지 확인 |
| `Could not find or load main class` | CLI 폴더에 `modelconvertor.jar`가 있는지 확인 |
| Java 관련 오류 또는 `java`를 찾을 수 없음 | Java 8+ 런타임이 설치되어 PATH에 있는지 확인 |
| Oracle 설정 오류 | `%USERPROFILE%\.modelconvertor\oracle.properties` 경로와 네 필수 키 확인 |
| 모델이 잘못된 위치에 생성됨 | Claude Code를 시작한 현재 디렉터리 또는 `--output` 확인 |
| `/modelconvertor`가 보이지 않음 | 전역 `SKILL.md` 경로를 확인하고 새 Claude Code 세션 시작 |
