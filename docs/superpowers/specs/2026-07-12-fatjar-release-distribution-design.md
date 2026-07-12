# ModelConvertor Fat JAR 및 Release 배포 설계

- 작성일: 2026-07-12
- 상태: 승인됨
- 대상 프로젝트: `modelconvertor`
- 관계: 기존 `2026-07-11-modelconvertor-cli-oracle-design.md`의 3절·15절 일부를 대체한다.

## 1. 목적

여러 PC의 여러 Java 프로젝트에서 마찰 없이(turnkey) ModelConvertor를 쓰기 위해, Oracle JDBC 드라이버를 포함한 단일 실행 JAR(fat jar)을 1회 빌드해 GitHub Release 에셋으로 배포한다. 소스 저장소에는 빌드 산출물(바이너리)을 커밋하지 않는다.

## 2. 배경과 결정 근거

- Oracle JDBC 드라이버 `ojdbc8`은 Maven Central(`com.oracle.database.jdbc:ojdbc8`)에 OFUTC(무료·재배포 허용) 라이선스로 공개되어 있다.
- 따라서 기존 설계가 드라이버를 외부로 둔 근거(재배포 곤란)가 사라졌다.
- fat jar로 드라이버를 포함하면 사용하는 PC는 Maven·인터넷 빌드 없이 JRE만으로 실행할 수 있다.
- 바이너리를 git 소스 트리에 커밋하는 대신 Release 에셋으로 배포해 히스토리 오염과 산출물-소스 불일치를 피한다.

## 3. 결정 요약

- `ojdbc8`을 Maven Central 의존성(`runtime` 스코프)으로 선언한다.
- `maven-shade-plugin`으로 드라이버를 포함한 fat jar를 생성한다.
- 실행은 `java -jar modelconvertor.jar`로 단순화한다.
- fat jar는 소스 트리에 커밋하지 않고 GitHub Release 에셋으로 배포한다.
- 사용하는 PC는 저장소를 clone(가벼운 텍스트)한 뒤 설치 스크립트가 Release에서 jar를 내려받는다.

## 4. 기존 설계 대체 항목

원본 설계에서 다음을 갱신한다.

- 3절: "구현은 `java.sql` API만 사용하고 `ojdbc8.jar`는 실행 시 클래스패스에 추가한다." → 드라이버는 fat jar에 포함한다. 코드는 여전히 `java.sql`만 사용한다(컴파일 의존 없음, 런타임 포함).
- 15절: "외부 `ojdbc8.jar`는 CMD의 런타임 클래스패스로 제공", "Shade 플러그인은 사용하지 않는다." → shade로 fat jar를 생성하고 드라이버를 포함한다.

원본 설계의 나머지(데이터 미조회, SQL 안전성, 타입 매핑, 이름 규칙, 오류 처리 등)는 그대로 유효하다.

## 5. Maven 변경

### 의존성

```xml
<dependency>
  <groupId>com.oracle.database.jdbc</groupId>
  <artifactId>ojdbc8</artifactId>
  <version>21.11.0.0</version>
  <scope>runtime</scope>
</dependency>
```

- 버전은 JDK8 호환 ojdbc8 최신값을 확인해 고정한다.
- 코드가 `oracle.*`를 import하지 않으므로 컴파일 의존이 아니라 `runtime` 스코프가 정확하다.

### Shade 플러그인

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

- `ServicesResourceTransformer`는 필수다. Oracle 드라이버는 `META-INF/services/java.sql.Driver`로 등록되므로, 이 병합이 없으면 실행 시 `No suitable driver`가 발생한다.
- 최종 산출물은 `target/modelconvertor.jar`(fat) 하나다.

## 6. 실행 스크립트 변경

`modelconvertor.cmd`는 fat jar 단일 실행으로 단순화한다.

```bat
@echo off
setlocal
set "APP_DIR=%~dp0"
java -jar "%APP_DIR%modelconvertor.jar" %*
exit /b %ERRORLEVEL%
```

- `-cp`와 외부 `ojdbc8.jar` 참조를 제거한다.
- Main-Class는 shade 매니페스트가 제공한다.

## 7. 배포와 설치

### 배포(개발 PC, 1회/버전)

```powershell
mvn clean package
gh release create v1.0.0 target\modelconvertor.jar --title "ModelConvertor v1.0.0" --notes "Fat jar with bundled ojdbc8"
```

### 설치(사용하는 PC)

- 저장소를 clone해 가벼운 텍스트(`modelconvertor.cmd`, `SKILL.md`, 설치 스크립트)를 확보한다.
- 설치 스크립트가 Release에서 jar를 내려받아 CLI 폴더에 둔다.
  - 공개 저장소: `https://github.com/<owner>/<repo>/releases/latest/download/modelconvertor.jar`를 `Invoke-WebRequest`로 받는다(항상 최신).
  - 비공개 저장소: `gh release download --repo <owner>/<repo> --pattern modelconvertor.jar`를 쓴다(해당 PC에서 `gh auth login` 1회 필요).
- 이후 사용자 PATH 등록, `SKILL.md`의 전역 복사, `oracle.properties` 존재 확인은 기존 전역 설치 절차를 따른다.

## 8. 재조정이 필요한 기존 문서

코드 변경과 로컬 검증이 끝난 뒤 다음을 실제 동작에 맞춰 갱신한다. 검증 전에는 갱신하지 않는다.

- `README.md`: 빌드·설치·드라이버 안내를 fat jar/`java -jar`/Release 다운로드 기준으로 수정한다. 외부 `ojdbc8.jar` 배치 문구를 제거한다.
- `docs/global-installation-guide.md`: "2. CLI와 JDBC 드라이버 설치" 흐름을 Release 다운로드로 교체하고, 세 파일 복사 대신 cmd + 다운로드한 jar 구성으로 바꾼다.
- `docs/superpowers/specs/2026-07-11-modelconvertor-cli-oracle-design.md`: 3절·15절에 본 문서로 대체되었음을 명시한다.
- `docs/skill-usage-guide.md`, `.claude/skills/modelconvertor/SKILL.md`: 변경 불필요(패키징과 무관).

## 9. 검증

실제 Oracle 접속 없이 확인 가능한 범위로 제한한다.

- fat jar 빌드 성공: `mvn clean package`로 `target/modelconvertor.jar` 생성.
- 실행 진입: `java -jar target\modelconvertor.jar --help`가 종료 코드 0과 usage를 출력.
- 드라이버 포함 확인(게이트): 접속을 시도했을 때 오류가 `No suitable driver`가 아니라 접속 실패(예: `Oracle processing failed (code ...)`)여야 한다. 이는 shade의 ServiceLoader 병합이 성공했음을 뜻한다.
- Release 다운로드 URL이 파일을 가져오는지 확인.

기존 단위·통합 테스트(59개)는 그대로 통과해야 한다. shade는 산출물 패키징만 바꾸므로 로직 테스트에 영향이 없어야 한다.

## 10. 범위 제외

- 플러그인화(`bin/`, SessionStart 훅 등)는 하지 않는다. 스킬은 기존 얇은 계층을 유지한다.
- 실제 Oracle 접속 E2E는 본 단계에서 수행하지 않는다.
- Codex 배포는 별도 판단으로 미룬다.
- CLI 동작·SQL 처리·오류 처리 로직은 변경하지 않는다(패키징·배포만 변경).
