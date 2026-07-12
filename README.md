# ModelConvertor

Oracle `SELECT` SQL의 JDBC 결과 메타데이터를 이용해 Java 8용 DZ 모델 소스를 생성하는 Windows 명령줄 도구입니다. 직접 원본 컬럼으로 확인되는 항목은 Oracle의 컬럼 설명도 모델에 반영합니다.

## 주요 기능

- Oracle 결과 컬럼의 순서와 JDBC 타입을 기반으로 Java 필드 타입 결정
- `ALL_COL_COMMENTS`에서 직접 참조 컬럼의 설명 조회
- 대화형 입력, UTF-8 SQL 파일, PowerShell 파이프 입력 지원
- UTF-8 Java 파일 생성 또는 `--stdout` 출력
- 기존 파일 보호와 명시적 `--overwrite` 처리
- DB 결과 행을 사용하지 않는 메타데이터 중심 처리

## 준비 사항

- Java 8 이상
- Maven 3.x (JAR 빌드 시)
- Oracle JDBC 드라이버 `ojdbc8.jar`
- Oracle 접속 정보와 모델 생성 대상 SQL

Oracle JDBC 드라이버는 JAR에 포함하지 않습니다. SQL 검증은 보조 안전장치일 뿐이므로 Oracle 계정은 가능한 한 조회 전용 계정을 사용하세요.

## 빌드

프로젝트 루트에서 실행합니다.

```powershell
mvn clean package
```

성공하면 다음 JAR이 생성됩니다.

```text
target\modelconvertor.jar
```

테스트만 실행하려면 다음 명령을 사용합니다.

```powershell
mvn test
```

## 설치

빌드한 JAR, 프로젝트의 `modelconvertor.cmd`, Oracle JDBC 드라이버를 같은 폴더에 둡니다.

```text
C:\tools\modelconvertor\
├─ modelconvertor.jar
├─ modelconvertor.cmd
└─ ojdbc8.jar
```

`modelconvertor.cmd`는 위 폴더의 JAR와 `ojdbc8.jar`를 classpath에 추가해 실행합니다. `C:\tools\modelconvertor`를 `PATH`에 추가하거나 CMD 파일의 전체 경로를 사용하세요.

```powershell
C:\tools\modelconvertor\modelconvertor.cmd --help
```

## Oracle 접속 설정

기본 설정 파일 위치는 다음과 같습니다.

```text
%USERPROFILE%\.modelconvertor\oracle.properties
```

PowerShell에서는 다음 경로입니다.

```powershell
$env:USERPROFILE\.modelconvertor\oracle.properties
```

설정 파일 예시입니다.

```properties
oracle.url=jdbc:oracle:thin:@127.0.0.1:1521/ORCL
oracle.username=MY_USER
oracle.password=MY_PASSWORD
oracle.schema=MY_SCHEMA
```

`oracle.url`은 Oracle 환경에 맞게 변경합니다.

```properties
# 서비스명 방식
oracle.url=jdbc:oracle:thin:@DB_HOST:1521/SERVICE_NAME

# SID 방식
oracle.url=jdbc:oracle:thin:@DB_HOST:1521:SID
```

비밀번호는 평문으로 저장되므로 설정 파일을 Git에 추가하거나 공유하지 마세요. 다른 설정 파일을 사용하려면 `--config <path>`를 지정합니다.

## 사용법

```text
modelconvertor [--sql-file <path>] [--class-name <name>] [--package <name>]
               [--output <source-root>] [--config <path>] [--overwrite]
               [--stdout] [--help]
```

### 대화형 입력

모델을 생성할 Java 프로젝트 폴더에서 실행하면 기본 출력 경로를 바로 사용할 수 있습니다.

```powershell
cd C:\work\my-dz-project
C:\tools\modelconvertor\modelconvertor.cmd
```

클래스명과 패키지명을 입력한 뒤 SQL을 붙여넣고, 마지막 줄에 `:end`를 입력합니다.

```text
Class name: EmployeeModel
Package name: com.company.hr.model
Paste SQL; enter :end on its own line to finish.
SELECT EMP.EMP_NO,
       EMP.EMP_NM AS employeeName
  FROM HR_EMP EMP
:end
```

### SQL 파일 입력

SQL 파일은 UTF-8로 저장합니다.

```powershell
C:\tools\modelconvertor\modelconvertor.cmd `
  --sql-file C:\queries\employee.sql `
  --class-name EmployeeModel `
  --package com.company.hr.model
```

### PowerShell 파이프 입력

파이프 입력에서는 대화형 질문을 할 수 없으므로 `--class-name`과 `--package`가 필수입니다.

```powershell
Get-Content C:\queries\employee.sql |
  C:\tools\modelconvertor\modelconvertor.cmd `
    --class-name EmployeeModel `
    --package com.company.hr.model
```

### 생성 결과만 확인

`--stdout`은 파일을 만들지 않고 Java 소스만 출력합니다. 처음에는 이 옵션으로 생성 결과를 검토하는 것을 권장합니다.

```powershell
C:\tools\modelconvertor\modelconvertor.cmd `
  --sql-file C:\queries\employee.sql `
  --class-name EmployeeModel `
  --package com.company.hr.model `
  --stdout
```

### 출력 위치 변경

기본 출력 위치는 현재 디렉터리 기준 다음과 같습니다.

```text
src/main/java/<패키지 경로>/<클래스명>.java
```

`--output`에는 `src/main/java`에 해당하는 소스 루트를 지정합니다.

```powershell
C:\tools\modelconvertor\modelconvertor.cmd `
  --sql-file C:\queries\employee.sql `
  --class-name EmployeeModel `
  --package com.company.hr.model `
  --output C:\work\my-dz-project\src\main\java
```

패키지 디렉터리는 자동으로 생성됩니다.

### 기존 파일 덮어쓰기

같은 출력 파일이 이미 있으면 기본적으로 생성에 실패합니다. 덮어쓰려면 반드시 `--overwrite`를 지정해야 합니다.

```powershell
C:\tools\modelconvertor\modelconvertor.cmd `
  --sql-file C:\queries\employee.sql `
  --class-name EmployeeModel `
  --package com.company.hr.model `
  --overwrite
```

### 별도 설정 파일 사용

```powershell
C:\tools\modelconvertor\modelconvertor.cmd `
  --sql-file C:\queries\employee.sql `
  --class-name EmployeeModel `
  --package com.company.hr.model `
  --config C:\secure\dev-oracle.properties
```

## 생성 코드

생성되는 모델은 다음 요소를 포함합니다.

- `DzAbstractModel` 상속
- `@DzModel`, `@DzModelField`, `@SerializedName` 애너테이션
- JDBC 타입 기반 Java 필드
- getter와 setter
- 필요한 경우 `BigDecimal`, `LocalDateTime` import

결과 컬럼 라벨은 애너테이션 값에 유지하고, Java 필드명만 유효한 camelCase 식별자로 바꿉니다. 생성 소스를 컴파일하는 대상 프로젝트에는 DZ와 Gson 라이브러리가 있어야 합니다.

## 오류 메시지와 종료 코드

| 종료 코드 | 상황 |
|---|---|
| `0` | 정상 처리 또는 `--help` 출력 |
| `1` | 설정·SQL 파일 I/O, Oracle 연결·메타데이터, 출력 파일 충돌 등 처리 실패 |
| `2` | 잘못된 옵션, SQL, 클래스명 또는 패키지명 |

설정 파일 또는 SQL 파일을 읽지 못하면 해당 경로와 원인을 출력합니다. Oracle 처리 실패 시 Oracle 오류 코드와 원인을 출력하되, 설정된 비밀번호가 오류 메시지에 포함되어도 `***`로 마스킹합니다.

## SQL 지원 범위와 제한사항

- 단일 `SELECT` 또는 `WITH ... SELECT`만 지원합니다.
- SQL 주석과 마지막 세미콜론 하나는 정리합니다.
- `INSERT`, `UPDATE`, `DELETE`, `MERGE`, DDL, 여러 SQL 문은 거부합니다.
- MyBatis 동적 태그와 `#{...}`, `${...}`는 지원하지 않습니다.
- 완전한 Oracle SQL 파서가 아닙니다. 복잡한 식은 고유한 `AS` 별칭을 지정하세요.
- `PreparedStatement.getMetaData()`를 우선 사용합니다. 드라이버가 메타데이터를 제공하지 않으면 `WHERE 1 = 0` 조건의 래퍼 쿼리로 메타데이터를 얻습니다.
- 모델 생성에 실제 결과 행을 소비하지 않습니다.
- `RN`, `ROWNUM`도 결과 컬럼이면 생성됩니다. 필요 없으면 최종 SELECT 목록에서 제외하세요.
- 중복 결과 라벨 또는 변환 후 중복 Java 필드명은 오류가 됩니다. 고유한 `AS` 별칭을 사용하세요.
- 직접 원본 컬럼으로 판별되는 경우에만 `ALL_COL_COMMENTS` 설명을 사용합니다. 계산식 또는 모호한 컬럼의 설명은 빈 문자열입니다.
- Oracle만 지원합니다.

## 상세 문서

- [프로젝트 처리 흐름](docs/modelconvertor-flow.md)
- [대화형 재입력 추후 검토](docs/future-improvements.md)
