# 배포 (유지보수자용)

실행 JAR은 소스 저장소에 커밋하지 않고 GitHub Release 에셋으로 배포합니다. 새 버전을 낼 때 다음 체크리스트를 따릅니다.

## 배포 체크리스트

1. 빌드: `mvn clean package`
2. 테스트 통과 확인: `Tests run: 59, Failures: 0, Errors: 0`
3. 실행 진입 확인: `java -jar target\modelconvertor.jar --help` (종료 코드 0)
4. 드라이버 포함 확인: 접속 시도 오류가 `No suitable driver`가 아니라 접속 실패여야 함
5. Release 발행: 에셋 이름은 정확히 `modelconvertor.jar`

```powershell
gh release create v1.0.0 target\modelconvertor.jar --title "ModelConvertor v1.0.0" --notes "Fat jar with bundled ojdbc8"
```

`gh`가 없으면 GitHub 웹 Releases에서 태그를 만들고 `target\modelconvertor.jar`를 첨부합니다.

선택: 변조 확인용으로 SHA-256 값을 릴리스 노트에 남기거나 `.sha256` 파일을 함께 첨부할 수 있습니다.

```powershell
(Get-FileHash target\modelconvertor.jar -Algorithm SHA256).Hash
```

설치 측에서는 `install.ps1`이 JAR을 실행하지 않고 zip 구조와 `org/sqlmodel/Main.class` 존재로 무결성을 검사하며, 재현 가능한 설치가 필요하면 `-Version <tag>`로 버전을 고정합니다.

6. 설치 URL 재검증:

```powershell
Invoke-WebRequest -Uri 'https://github.com/twin10240/sql-model-gen/releases/latest/download/modelconvertor.jar' -OutFile "$env:TEMP\mc-verify.jar" -UseBasicParsing
(Get-Item "$env:TEMP\mc-verify.jar").Length -gt 0
```

## 향후 자동화 (선택)

태그 푸시 시 위 절차(빌드·테스트·Release 업로드)를 수행하는 GitHub Actions 워크플로를 `.github/workflows`에 추가할 수 있습니다. 초기에는 수동 배포로 충분하며, 배포 빈도가 늘면 도입합니다.
