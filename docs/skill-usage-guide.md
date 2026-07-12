# Codex·Claude Code 스킬 사용 가이드

ModelConvertor 스킬이 등록된 Codex 또는 Claude Code 환경에서는 사용자가 `modelconvertor` 명령 옵션을 직접 조합할 필요가 없습니다. SQL과 원하는 모델 정보를 자연어로 전달하면 스킬이 필요한 값을 확인한 뒤 CLI를 실행합니다.

## 사용자, 스킬, CLI의 역할

```text
사용자 ↔ Codex·Claude Code 스킬 → ModelConvertor CLI → Java 모델 파일
```

- 사용자는 SQL 본문 또는 SQL이 저장된 UTF-8 텍스트 파일 경로를 전달합니다.
- 스킬은 클래스명과 패키지명 등 누락된 정보만 사용자에게 질문합니다.
- CLI는 스킬이 전달한 완성된 옵션을 검증하고 Oracle 메타데이터를 읽어 모델을 생성합니다.

스킬과 사용자의 대화는 대화형이지만, 스킬은 필요한 값을 모두 확보한 뒤 CLI를 비대화형으로 실행합니다.

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
