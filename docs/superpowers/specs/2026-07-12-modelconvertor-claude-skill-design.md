# ModelConvertor Claude Code 스킬 설계

## 목적

Claude Code 사용자가 자연어로 Oracle SQL 또는 SQL 파일을 전달해 DZ Java 모델을 생성하도록 `modelconvertor` 스킬을 제공한다. 스킬은 기존 `modelconvertor.cmd`를 직접 호출하는 얇은 지시 계층으로 유지한다.

## 범위

첫 구현은 Claude Code 프로젝트 스킬 하나만 만든다.

```text
.claude/skills/modelconvertor/SKILL.md
```

스킬 본문이 실제 사용으로 안정된 뒤 동일한 내용을 Codex의 `.agents/skills/modelconvertor/SKILL.md`에 배치한다. 두 플랫폼의 동작 지시를 별도로 포크하지 않는다.

## 구성

스킬은 `SKILL.md` 한 파일로 구성한다. 별도 parser, 프롬프트 코드, 실행 래퍼, 설치 검사 스크립트, reference 및 asset은 만들지 않는다.

YAML frontmatter에는 다음만 둔다.

- `name: modelconvertor`
- Oracle SELECT, DZ 모델, Java 모델 생성 요청에 반응하는 구체적인 `description`

본문은 200단어 이내의 명령형 지시로 작성한다.

## 실행 흐름

1. 사용자가 존재하는 텍스트 파일 경로를 제공하면 해당 경로를 `--sql-file`로 전달한다.
2. 사용자가 SQL 본문을 제공하면 표준 입력으로 전달한다. 현재 셸에서 멀티라인 전달이 안전하지 않을 때만 임시 UTF-8 파일을 사용하고 처리 후 삭제한다.
3. 클래스명 또는 패키지명이 없으면 CLI를 실행하기 전에 누락된 값만 사용자에게 질문한다.
4. 두 값을 모두 확보한 뒤 `modelconvertor.cmd`에 `--class-name`과 `--package`를 전달한다.
5. 결과 검토만 요청하면 `--stdout`, 출력 위치를 지정하면 `--output`, 명시적 덮어쓰기 요청이 있을 때만 `--overwrite`를 사용한다.
6. 명령을 찾지 못하거나 설치 관련 오류가 발생하면 새 검사 로직을 실행하지 않고 README의 설치 절차를 안내한다.

## 안전 기준

- 사용자의 명시적 요청 없이 기존 파일을 덮어쓰지 않는다.
- Oracle 비밀번호를 대화나 명령 인자로 요청하지 않는다.
- 사용자가 제공한 SQL을 임의로 의미 변경하지 않는다.
- 파일 경로처럼 보인다는 이유만으로 존재하지 않는 입력을 파일로 단정하지 않는다.
- 기존 `modelconvertor.cmd` 위에 추가 실행 래퍼를 만들지 않는다.

## 검증

`superpowers:writing-skills`의 RED-GREEN 절차를 적용한다.

### 기준선

스킬 없이 다음 요청을 주고 에이전트가 `modelconvertor` 고유 동작을 알지 못하거나 필요한 입력·안전 규칙을 빠뜨리는지 기록한다.

- SQL 본문과 클래스·패키지를 모두 제공한 요청
- 파일 경로만 제공하고 클래스·패키지를 생략한 요청
- 결과만 보여 달라는 요청
- 기존 파일을 덮어써 달라는 요청

### 스킬 적용 후

- 자동 호출: description과 일치하는 자연어 요청에서 스킬이 선택되는지 확인한다.
- 명시 호출: `/modelconvertor`로 직접 실행되는지 확인한다.
- 누락된 클래스명·패키지만 질문하는지 확인한다.
- SQL 본문은 표준 입력, 존재하는 파일은 `--sql-file`로 전달하는지 확인한다.
- 명시적 요청 없이는 `--overwrite`를 사용하지 않는지 확인한다.
- `SKILL.md` frontmatter와 이름을 구조 검증한다.

설치된 JAR와 실제 Oracle 접속 환경이 없으면 검증 범위를 올바른 명령 구성과 명확한 오류 안내까지로 제한하고, 실제 모델 생성 E2E를 통과했다고 주장하지 않는다.

## 범위 제외

- Java CLI 수정
- `modelconvertor.cmd` 수정 또는 추가 래퍼 작성
- Oracle JDBC 드라이버 배포
- 자동화된 테스트 스위트 추가
- Claude Code 플러그인 패키징
- 초기 검증 전 Codex용 복사본 생성
