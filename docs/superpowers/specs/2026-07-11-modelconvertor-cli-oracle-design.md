# ModelConvertor CLI 및 Oracle 메타데이터 연동 설계

- 작성일: 2026-07-11
- 상태: 승인됨
- 대상 프로젝트: `modelconvertor`
- 실행 환경: Java 8, Oracle, Windows

## 1. 목적

DZ Java 8 환경에서 사용할 모델 클래스를 SQL로 생성하는 개인용 CLI 도구를 만든다. DZ 의존성을 직접 참조하지 않고 Oracle JDBC 결과 메타데이터로 필드 타입을 결정하며, 원본 테이블 컬럼을 명확히 추적할 수 있을 때 Oracle 컬럼 설명을 모델에 반영한다.

핵심 개선 목표는 다음과 같다.

- 컬럼명 접미사 추측 대신 JDBC 결과 메타데이터를 우선한다.
- 실제 데이터는 조회하지 않는다.
- 계산식의 원본 설명을 임의로 추론하지 않는다.
- 직접 입력, SQL 파일, 표준 입력을 모두 지원한다.
- 생성 파일 충돌과 인코딩을 안전하게 처리한다.

## 2. 범위

### 포함

- Java 8 호환 CLI
- Oracle 전용 JDBC 메타데이터 조회
- SQL 직접 붙여넣기, 파일, 표준 입력
- `SELECT` 및 `WITH ... SELECT` 쿼리
- DZ 모델 필드, 어노테이션, getter/setter 생성
- 원본 컬럼의 `ALL_COL_COMMENTS` 설명 조회
- UTF-8 Java 파일 출력
- 기존 파일 보호와 명시적 덮어쓰기

### 제외

- Oracle 이외 DB
- MyBatis XML 동적 태그와 `#{...}`, `${...}` 처리
- 실제 조회 결과 데이터 사용
- GUI, 자동 업데이트, 팀 배포
- Codex 및 Claude Code 스킬·플러그인 구현
- 완전한 SQL 문법 파서
- 계산식 설명의 임의 추론

AI 스킬은 CLI 검증이 끝난 후 동일 CLI를 호출하는 얇은 계층으로 별도 설계한다.

## 3. 배포 및 설정

개인용 배포 구조는 다음과 같다.

```text
C:\tools\modelconvertor\
├── modelconvertor.jar
├── modelconvertor.cmd
└── ojdbc8.jar
```

Oracle 접속 정보는 저장소 밖 사용자 홈에 둔다.

```text
C:\Users\<사용자>\.modelconvertor\oracle.properties
```

```properties
oracle.url=jdbc:oracle:thin:@127.0.0.1:1521/ORCL
oracle.username=MY_USER
oracle.password=MY_PASSWORD
oracle.schema=MY_SCHEMA
```

규칙:

- 실제 설정 파일은 Git에 저장하지 않는다.
- 접속 정보와 비밀번호를 로그에 출력하지 않는다.
- 가능하면 조회 전용 DB 계정을 사용한다.
- 기본 경로 외 설정은 `--config <경로>`로 지정한다.
- 구현은 `java.sql` API만 사용한다. (드라이버 제공 방식은 [2026-07-12-fatjar-release-distribution-design.md](2026-07-12-fatjar-release-distribution-design.md)로 대체되었다. `ojdbc8`을 fat jar에 포함한다.)

## 4. CLI 입력

### 4.1 대화형 입력

인자 없이 실행하면 클래스명, 패키지명, 출력 경로와 SQL을 입력받는다. SQL 마지막 단독 줄의 `:end`를 종료 표식으로 사용한다.

```text
클래스명: EmployeeModel
패키지명: com.company.hr.model
SQL을 붙여 넣으세요. 마지막 줄에 :end를 입력하세요.

SELECT EMP.EMP_NO,
       EMP.EMP_NM AS employeeName
  FROM HR_EMP EMP
:end
```

### 4.2 SQL 파일

```powershell
modelconvertor --sql-file C:\queries\employee.sql `
  --class-name EmployeeModel `
  --package com.company.hr.model
```

### 4.3 표준 입력

```powershell
Get-Content C:\queries\employee.sql |
  modelconvertor --class-name EmployeeModel --package com.company.hr.model
```

입력 우선순위는 `--sql-file`, 표준 입력, 대화형 입력 순이다. 긴 SQL의 셸 이스케이프 문제를 피하기 위해 `--sql "..."` 옵션은 제공하지 않는다.

### 4.4 CLI 옵션

```text
--sql-file <경로>
--class-name <클래스명>
--package <패키지명>
--output <소스 루트>
--config <DB 설정 파일>
--overwrite
--stdout
--help
```

누락된 필수 값은 대화형으로 입력받는다.

## 5. 출력

기본 출력 위치는 다음과 같다.

```text
{현재 작업 디렉터리}/src/main/java/{패키지 경로}/{클래스명}.java
```

예:

```text
C:\work\dz-project\src\main\java\com\company\hr\model\EmployeeModel.java
```

규칙:

- `--output`은 `src/main/java`에 해당하는 소스 루트를 변경한다.
- 패키지 디렉터리는 `Files.createDirectories()`로 생성한다.
- 기존 파일이 있으면 기본적으로 실패한다.
- `--overwrite`에서만 기존 파일을 교체한다. 이어쓰기는 하지 않는다.
- 모든 생성 파일은 UTF-8로 기록한다.
- `--stdout`은 파일을 생성하지 않고 코드를 표준 출력으로 보낸다.
- 대화형 모드는 쓰기 전에 최종 절대 경로와 덮어쓰기 여부를 보여준다.
- 성공 시 생성된 절대 경로를 출력한다.

Java 8 NIO 옵션은 기본 생성에 `CREATE_NEW`, 덮어쓰기에 `CREATE`, `TRUNCATE_EXISTING`을 사용한다.

## 6. 처리 흐름

```text
SQL 입력
  → 주석 및 마지막 세미콜론 정리
  → SELECT/WITH 안전성 검사
  → 최소 SQL 분석(결과 항목, 테이블 별칭, 직접 컬럼 참조)
  → Oracle 결과 메타데이터 조회
  → 중복 결과 라벨 검사
  → 직접 컬럼의 Oracle 설명 조회
  → 타입 및 Java 이름 결정
  → DZ 모델 코드 생성
  → stdout 또는 UTF-8 파일 출력
```

SQL 분석은 기존 `findSelectList`, `splitSelectItems`, `stripComments`, `extractAliasOrName`의 아이디어를 재사용하되, 새 구조에 맞게 검증하고 정리한다.

## 7. SQL 안전성 및 정규화

- 블록 주석과 한 줄 주석을 제거한 후 첫 키워드를 검사한다.
- `SELECT`와 `WITH`로 시작하는 조회문만 허용한다.
- 입력 끝의 세미콜론은 제거한다.
- 여러 SQL 문은 거부한다.
- INSERT, UPDATE, DELETE, MERGE, DDL은 거부한다.
- 실패 시 모델 파일을 생성하지 않고 0이 아닌 종료 코드를 반환한다.

이 검사는 완전한 SQL 보안 파서가 아니므로 DB 계정 권한도 조회 전용으로 제한하는 것을 권장한다.

## 8. JDBC 결과 메타데이터

1차 방식은 쿼리를 실행하지 않는 `PreparedStatement.getMetaData()`다.

```java
PreparedStatement statement = connection.prepareStatement(sql);
ResultSetMetaData metadata = statement.getMetaData();
```

드라이버가 `null`을 반환하거나 메타데이터를 제공하지 못하면 다음 0건 쿼리를 fallback으로 사용한다.

```sql
SELECT *
  FROM (
       -- 원본 SELECT
  )
 WHERE 1 = 0
```

fallback도 행을 가져오지 않으며 메타데이터만 사용한다. 대상 Oracle 및 `ojdbc8.jar` 조합에서 두 방식을 통합 테스트한다.

사용 정보:

- 결과 컬럼 라벨과 순서
- JDBC 타입
- precision 및 scale
- 길이
- NULL 허용 여부

## 9. 이름 및 설명 생성 규칙

### 9.1 AS 별칭이 있는 직접 컬럼

```sql
SELECT EMP.EMP_NM AS employeeName
  FROM HR_EMP EMP
```

| 생성 항목 | 값 |
|---|---|
| `SerializedName` | `employeeName` |
| `DzModelField.name` | `employeeName` |
| `DzModelField.colName` | `employeeName` |
| Java 변수명 | `employeeName` |
| getter/setter | `getEmployeeName` / `setEmployeeName` |
| `desc` | `HR_EMP.EMP_NM`의 컬럼 설명 |
| Java 타입 | JDBC 결과 메타데이터 |

```java
@SerializedName("employeeName")
@DzModelField(name = "employeeName", desc = "사원명", colName = "employeeName")
private String employeeName;
```

### 9.2 AS 별칭이 없는 직접 컬럼

```sql
SELECT EMP.EMP_NM
  FROM HR_EMP EMP
```

문자열 기반 어노테이션에는 원본 결과 라벨 `EMP_NM`을 유지하고 Java 식별자만 camelCase로 변환한다.

```java
@SerializedName("EMP_NM")
@DzModelField(name = "EMP_NM", desc = "사원명", colName = "EMP_NM")
private String empNm;
```

### 9.3 계산 컬럼

```sql
COALESCE(A.EMP_NM, B.USER_NM) AS displayName
```

- 결과 이름과 Java 이름은 별칭을 사용한다.
- 타입은 JDBC 결과 메타데이터를 사용한다.
- 원본 컬럼이 하나로 확정되지 않으므로 `desc`는 빈 문자열로 둔다.
- 첫 번째 참조 컬럼의 설명을 임의 적용하지 않는다.

### 9.4 설명 조회

테이블 별칭을 실제 테이블에 연결하고 직접 컬럼 하나가 확정된 경우에만 `ALL_COL_COMMENTS`에서 설명을 조회한다. 필요하면 `oracle.schema`를 소유자 조건으로 사용한다. 설명이 없거나 권한이 없으면 `desc = ""`로 생성은 계속한다.

## 10. 이름 충돌과 식별자

- JDBC 결과 라벨이 중복되면 생성하지 않는다.
- 오류에 중복 라벨을 표시하고 사용자가 `AS`로 고유한 이름을 지정하도록 안내한다.
- 숫자로 시작하거나 Java 예약어와 충돌하는 결과 라벨은 어노테이션 값에는 그대로 유지한다.
- Java 필드 및 메서드 이름만 유효한 camelCase 식별자로 정규화한다.
- 정규화 후 Java 식별자가 중복되어도 오류로 중단한다.

## 11. 타입 매핑

JDBC 결과 타입을 우선 사용한다.

| Oracle/JDBC 타입 | Java 타입 |
|---|---|
| `VARCHAR`, `VARCHAR2`, `NVARCHAR2`, `CHAR`, `NCHAR`, `CLOB`, `NCLOB` | `String` |
| 정수형 `NUMBER` | precision에 따라 `Integer` 또는 `Long` |
| 소수형 또는 범위가 큰 `NUMBER` | `BigDecimal` |
| `FLOAT`, `BINARY_FLOAT`, `BINARY_DOUBLE` | `Double` |
| `DATE`, `TIMESTAMP` | `LocalDateTime` |
| `TIMESTAMP WITH TIME ZONE`, `TIMESTAMP WITH LOCAL TIME ZONE` | 프로토타입에서 `LocalDateTime` |
| 미지원 타입 | `String` fallback 및 경고 |

필요한 타입만 import한다.

```java
import java.math.BigDecimal;
import java.time.LocalDateTime;
```

DZ MyBatis 환경의 `LocalDateTime` 타입 핸들러 호환성은 실제 생성 모델로 검증한다. JDBC 메타데이터 조회 자체가 실패한 경우에만 기존 컬럼명 접미사 규칙을 fallback으로 사용한다.

## 12. RN 및 ROWNUM

JDBC 결과 컬럼은 모두 모델에 포함한다. 기존의 `RN`/`ROWNUM` 자동 제외 기능은 제거한다. 불필요한 페이징 컬럼은 사용자가 최종 SELECT 목록에서 제외하거나 명시적으로 별칭을 지정한다.

## 13. 오류 처리

- 설정 파일 없음 또는 필수 속성 누락: 설정 경로와 누락 키 출력
- DB 연결 실패: 비밀번호를 제외한 원인 출력, 파일 미생성
- SQL 파싱 또는 Oracle 분석 실패: SQL 위치와 원인 출력
- JDBC 메타데이터 없음: fallback 후에도 실패하면 중단
- 컬럼 설명 없음: 경고 없이 빈 설명으로 계속
- 중복 결과 라벨 또는 Java 식별자: 중단하고 `AS` 사용 안내
- 잘못된 클래스명·패키지명: DB 접속 전에 중단
- 기존 출력 파일: `--overwrite` 안내 후 중단

## 14. 코드 구조

구체적인 클래스 이름은 구현 계획에서 확정하되 책임은 다음처럼 분리한다.

- CLI: 인자 및 대화형 입력 처리
- Config: Oracle properties 로드와 검증
- SQL analysis: SQL 정리, 결과 항목과 직접 원본 컬럼 추적
- Metadata: JDBC 결과 타입 및 Oracle 컬럼 설명 조회
- Model generation: 필드 사양을 DZ Java 코드로 변환
- Output: stdout 또는 안전한 UTF-8 파일 쓰기

기존 `FileCreator`, main 기반 `SqlToModelTest`, 하드코딩 SQL인 `SqlContents`는 새 CLI와 테스트 구조로 대체한다.

## 15. Maven 빌드

- Java 8 source/target 또는 release 호환 설정
- 프로젝트 인코딩 UTF-8
- `maven-jar-plugin`으로 `Main-Class` 지정
- ~~외부 `ojdbc8.jar`는 CMD의 런타임 클래스패스로 제공~~ → [2026-07-12-fatjar-release-distribution-design.md](2026-07-12-fatjar-release-distribution-design.md)로 대체: `ojdbc8`을 fat jar에 포함
- ~~Shade 플러그인은 사용하지 않는다.~~ → 대체: `maven-shade-plugin`으로 fat jar 생성
- Java 8 호환 JUnit 테스트 의존성을 추가한다.

## 16. 검증

### 단위 테스트

- 직접 컬럼과 AS 별칭
- AS 없는 컬럼
- 테이블 별칭 추적
- JOIN의 동일 컬럼명
- 계산식 판별
- SQL 주석과 마지막 세미콜론
- `SELECT`/`WITH` 허용 및 변경문 거부
- camelCase와 예약어 처리
- 결과 라벨 및 정규화 식별자 중복
- Oracle/JDBC 타입 매핑
- 출력 경로와 UTF-8
- 기존 파일 보호와 `--overwrite`

### Oracle 통합 테스트

- `PreparedStatement.getMetaData()` 지원 여부
- 0건 래핑 fallback
- `ALL_COL_COMMENTS` 한글 설명
- DATE/TIMESTAMP의 `LocalDateTime` 생성
- CASE, COALESCE, JOIN, 서브쿼리, WITH
- 콘솔의 한글 SQL 입력과 출력
- DB 연결 및 권한 실패 시 비밀정보 미출력

### 완료 기준

- 단순 및 복합 SELECT의 결과 컬럼 순서와 Java 타입이 JDBC 메타데이터와 일치한다.
- 직접 컬럼만 정확한 DB 설명을 사용하고 계산식은 빈 설명을 사용한다.
- 실제 조회 행은 발생하지 않는다.
- 생성 코드는 Java 8에서 컴파일된다.
- 기존 파일은 명시적 옵션 없이 변경되지 않는다.

## 17. 구현 순서

1. 순수 타입·이름·SQL 분석 단위 테스트
2. 안전한 UTF-8 출력과 CLI
3. 설정 로더와 Oracle 연결
4. JDBC 결과 메타데이터와 0건 fallback
5. 원본 컬럼 설명 조회
6. 모델 생성 통합
7. Oracle 통합 검증
8. CLI 안정화 후 Codex·Claude Code 스킬 별도 설계
