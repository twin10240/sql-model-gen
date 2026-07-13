# CLAUDE.md

ModelConvertor는 Oracle `SELECT`로 Java DZ 모델을 생성하는 Windows CLI 도구이며, 이를 호출하는 Claude Code 스킬(`.claude/skills/modelconvertor/SKILL.md`)을 함께 배포한다. 실행 JAR은 소스에 커밋하지 않고 GitHub Release 에셋으로 배포한다.

## 이 도구를 "설치"해 달라고 요청받으면

사용자가 "스킬 설치", "설치해줘" 등으로 요청하면 `SKILL.md`만 복사하지 말고 전역 설치 스크립트를 실행한다.

1. 저장소가 아직 clone되어 있지 않으면 clone한다.
2. 저장소 루트에서 `.\install.ps1`을 실행한다. 비공개 저장소면 `.\install.ps1 -UseGh`(사전에 `gh auth login` 필요).
   - 최신 Release에서 `modelconvertor.jar`를 내려받아 CLI 폴더·사용자 PATH·전역 스킬(`~/.claude/skills/modelconvertor/SKILL.md`)을 구성한다.
   - install.ps1은 다운로드한 JAR을 실행하지 않고 구조만 검증한다.
3. `C:\Douzone\dews-web\config\modelconvertor\oracle.properties`가 없으면 사용자에게 작성을 안내한다. **Oracle 비밀번호를 채팅이나 명령 인자로 요청하지 않는다.**
4. 설치 후 **새 Claude Code 세션**과, PATH 반영을 위한 새 터미널이 필요함을 안내한다.

업데이트: `git pull --ff-only` 후 `.\install.ps1 -Force`. 자세한 절차는 `docs/global-installation-guide.md`.

## 개발 시

- Java 8 타깃(`maven.compiler.release=8`). 빌드: `mvn clean package` → `target/modelconvertor.jar`(fat jar, `ojdbc8` 포함).
- 배포 절차는 `docs/releasing.md`. 빌드 산출물(jar)을 소스 트리에 커밋하지 않는다.
- 설계·계획 문서는 `docs/superpowers/`. 최신 배포 방식은 `docs/superpowers/specs/2026-07-12-fatjar-release-distribution-design.md`.
