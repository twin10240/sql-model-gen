# ModelConvertor 한국어 README 설계

- 작성일: 2026-07-12
- 대상 파일: `README.md`
- 상태: 승인됨

## 목적

처음 사용하는 개발자가 Windows 환경에서 ModelConvertor를 빌드·설치하고 Oracle 연결을 설정한 뒤 DZ 모델을 생성할 수 있도록 한국어 사용 안내서를 제공한다.

## 대상 독자

- Oracle SELECT를 DZ Java 모델로 변환하려는 개발자
- Maven과 Java 명령을 실행할 수 있지만 프로젝트 내부 구현은 모르는 사용자
- 대화형 입력, SQL 파일 또는 PowerShell 파이프로 도구를 사용하려는 사용자

## 문서 구성

README는 사용 절차를 따라 다음 순서로 구성한다.

1. 프로젝트 소개와 핵심 특징
2. 준비 사항
3. Maven 빌드
4. Windows 설치 파일 배치
5. Oracle 접속 설정과 보안 주의
6. 전체 CLI 옵션
7. 대화형·SQL 파일·파이프 실행 예시
8. `--stdout`, `--overwrite`, `--output`, `--config` 사용법
9. 출력 경로와 생성 코드 설명
10. 오류 메시지와 종료 코드
11. 지원 SQL과 제한사항
12. 테스트 실행 방법
13. 상세 처리 흐름 문서 링크

## 정확성 기준

- 실제 `modelconvertor.cmd`의 JAR·드라이버 classpath 구성과 일치해야 한다.
- 기본 설정 경로는 `%USERPROFILE%\.modelconvertor\oracle.properties`로 안내한다.
- Oracle JDBC 드라이버는 프로젝트 JAR에 포함되지 않는다고 명시한다.
- 기본 출력 위치는 현재 디렉터리의 `src/main/java/<package path>/<ClassName>.java`로 설명한다.
- 파이프 입력에서는 `--class-name`과 `--package`가 필수임을 명시한다.
- 실제 결과 행은 소비하지 않고 JDBC 메타데이터를 사용한다고 설명한다.
- 기존 파일은 기본적으로 보호되며 `--overwrite`에서만 교체된다고 안내한다.
- 새 오류 처리 동작인 설정·SQL 파일 경로, Oracle 오류 코드·원인, 비밀번호 마스킹을 반영한다.
- 지원하지 않는 MyBatis 동적 태그, 변경 SQL, 여러 SQL 문을 명시한다.

## 문체와 예시

- 본문은 한국어 중심으로 작성한다.
- 클래스명, 옵션, 경로, 명령, 설정 키는 원문 표기를 유지한다.
- PowerShell 명령은 복사해 실행할 수 있는 코드 블록으로 제공한다.
- 실제 비밀번호나 운영 접속 정보는 사용하지 않고 명확한 예시 값을 사용한다.
- 설치 직후에는 `--stdout`으로 결과를 확인하는 사용 순서를 권장한다.

## 제외 범위

- 내부 클래스별 상세 구현 설명
- 실제 Oracle 드라이버 재배포
- 운영 DB 접속 정보
- 대화형 재입력 기능 설명
- GUI 또는 Oracle 이외 데이터베이스 사용법
