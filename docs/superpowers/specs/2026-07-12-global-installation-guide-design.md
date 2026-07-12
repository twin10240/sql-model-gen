# ModelConvertor 전역 설치 가이드 설계

## 목적

다른 Windows PC와 여러 Java 프로젝트에서 Claude Code의 `modelconvertor` 스킬을 전역으로 사용할 수 있도록 스킬, CLI, Oracle JDBC 드라이버, PATH, Oracle 설정의 설치 관계를 한 문서에서 설명한다.

## 문서 구성

### 상세 가이드

새 문서 `docs/global-installation-guide.md`를 전역 설치 절차의 단일 원본으로 작성한다. 다음 내용을 순서대로 포함한다.

1. 전역 설치 후의 디렉터리 구조와 각 위치의 역할
2. Git 저장소 clone 또는 갱신
3. Maven으로 `modelconvertor.jar` 빌드
4. `modelconvertor.cmd`, `modelconvertor.jar`, `ojdbc8.jar`를 하나의 CLI 폴더에 배치
5. JDBC 드라이버가 다른 Java 프로젝트에 있을 때 CLI 폴더로 복사하는 방법
6. CLI 폴더를 사용자 PATH에 추가하는 방법
7. `.claude/skills/modelconvertor/SKILL.md`를 `%USERPROFILE%\.claude\skills\modelconvertor`에 복사하는 방법
8. `%USERPROFILE%\.modelconvertor\oracle.properties` 작성과 보안 주의사항
9. `modelconvertor.cmd --help` 및 새 Claude Code 세션에서 설치 확인
10. 대상 Java 프로젝트 루트에서 실행하는 사용 예시
11. 업데이트와 제거 방법
12. 흔한 설치 오류와 확인 지점

문서는 PowerShell 명령을 기준으로 작성한다. 사용자의 홈, 설치 폴더 및 저장소 경로는 변수로 정의해 반복 입력을 줄인다.

### README 안내

`README.md`의 설치 섹션에는 다음만 추가한다.

- 여러 프로젝트에서 사용하려면 전역 스킬과 PATH 기반 CLI 설치가 필요하다는 한 문단
- `docs/global-installation-guide.md` 링크

`## 상세 문서` 목록에도 같은 문서 링크를 추가한다. 상세 설치 명령은 README에 복제하지 않는다.

## 기본 설치 구조

가이드는 다음 구조를 기본값으로 사용한다.

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

소스 저장소 위치는 사용자가 선택할 수 있으며 실행 폴더와 분리한다.

## 정확성 및 안전 기준

- 현재 `modelconvertor.cmd`가 자신의 폴더에서 JAR와 `ojdbc8.jar`를 찾는다고 명시한다.
- 대상 Java 프로젝트의 Maven 의존성이나 `lib` 폴더에 있는 JDBC 드라이버는 자동으로 사용되지 않는다고 명시한다.
- JDBC 드라이버가 다른 위치에 있으면 사용자 확인 후 CLI 폴더로 복사하도록 안내한다.
- PATH는 사용자 범위에 추가하고 기존 사용자 PATH를 보존한다.
- 이미 존재하는 스킬, CLI 파일 또는 Oracle 설정 파일을 확인 없이 덮어쓰지 않는다.
- Oracle 비밀번호를 명령줄, Git 또는 문서 예시의 실제 값으로 노출하지 않는다.
- 전역 스킬 설치 후 새 Claude Code 세션을 시작하도록 안내한다.
- 기본 출력은 Claude Code를 시작한 현재 Java 프로젝트를 기준으로 한다고 명시한다.
- Codex 전역 스킬은 실제 `.agents/skills/modelconvertor/SKILL.md`가 아직 없으므로 완료된 기능처럼 안내하지 않는다.

## 업데이트와 제거

업데이트는 저장소를 갱신하고 JAR·CMD·전역 `SKILL.md`를 다시 복사하는 절차로 설명한다. 각 대상 파일이 존재하면 덮어쓰기 전에 백업 또는 사용자 확인을 요구한다.

제거는 다음 대상을 서로 구분해 안내한다.

- 전역 Claude 스킬 디렉터리
- PATH의 CLI 폴더 항목
- CLI 설치 폴더
- Oracle 설정 파일

Oracle 설정 파일은 접속 정보를 포함하므로 사용자가 명시적으로 원할 때만 제거하도록 경고한다.

## 범위 제외

- 자동 설치·업데이트 스크립트 작성
- Java CLI 또는 `modelconvertor.cmd` 수정
- `ojdbc8.jar` 재배포 또는 다운로드 자동화
- Codex 전역 스킬 설치 구현
- Windows 외 운영체제 설치 절차

## 검증

- 문서 링크가 실제 파일을 가리키는지 확인한다.
- PowerShell 예시가 기존 PATH를 보존하고 경로에 공백이 있어도 동작하도록 작성됐는지 검토한다.
- 설치 구조를 `modelconvertor.cmd`, Maven 출력 이름, Claude Code 전역 스킬 경로와 대조한다.
- `git diff --check`로 문서 형식을 확인한다.
- `TODO`, `TBD` 및 실제 비밀번호·환경 고유값이 없는지 확인한다.
