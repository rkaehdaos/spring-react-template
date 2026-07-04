# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 저장소 구성

이 저장소는 **백엔드(BE)와 프론트엔드(FE)가 같은 Git 트리 안에 공존하는 분리형 모노리포**다. 두 프로젝트는 서로 다른 빌드 시스템, 런타임, 언어 스택을 사용하며 명령어는 반드시 해당 하위 디렉토리에서 실행해야 한다.

- `BE/` — Kotlin + Spring Boot 4.0.6, Gradle 빌드
- `FE/` — Bun + React 19 + TypeScript (별도 `FE/CLAUDE.md` 존재)

## 백엔드 (BE/)

### 툴체인 / 환경

- **JDK**: Oracle GraalVM 25 (`BE/mise.toml`로 고정). Kotlin 컴파일은 `jvmToolchain(25)`, `jvmTarget = JVM_25` 기준.
- **Kotlin**: `2.4.0` (`languageVersion`/`apiVersion` 미지정 → 컴파일러 기본값 추종, 카탈로그 `kotlin` 버전이 단일 관리 지점), `allWarningsAsErrors = true` — 경고가 곧 빌드 실패다.
- **Gradle**: Wrapper 사용 (`./gradlew`). `configuration-cache`, `parallel`, `caching`, `vfs.watch` 모두 활성화. JVM 인자에 `Asia/Seoul`, UTF-8 인코딩, FFM(`--enable-native-access=ALL-UNNAMED`) 강제.
- **의존성 검증**: `org.gradle.dependency.verification=strict` — 새 라이브러리 추가 시 검증 메타데이터 갱신 필요.

### 자주 쓰는 명령어 (모두 `BE/`에서 실행)

```bash
./gradlew bootRun                        # 앱 실행 (UP-TO-DATE 무시 처리됨)
./gradlew build                          # 전체 빌드 + test (IntegrationTest/E2ETest 제외)
./gradlew test                           # 단위/슬라이스 테스트 (Dev DB 미접근)
./gradlew testUnit                       # 순수 단위 테스트만 (Spring Context 없음, 가장 빠름)
./gradlew testLocal                      # 단위 + H2 통합 (로컬 권장)
./gradlew testIntegration                # @Tag("integration") 만, Dev DB 필요
./gradlew testAll                        # 단위 + 통합 + E2E 모두 (CI 최종 검증)
./gradlew testLocal koverHtmlReport      # 커버리지 HTML 생성
./gradlew -PkoverFull=true koverHtmlReport   # 전체 테스트 포함 커버리지
./gradlew sonar                          # koverXmlReport 의존, SonarCloud 업로드
./gradlew nativeCompile                  # GraalVM 네이티브 이미지 빌드
./gradlew bootBuildImage                 # OCI 컨테이너 이미지
```

특정 테스트 클래스/메서드 실행:

```bash
./gradlew testLocal --tests "com.example.demo260516.Demo260516ApplicationTests"
./gradlew testLocal --tests "*.SomeClass.someMethod"
```

### 테스트 태스크 설계상의 규칙

- `test` 기본 태스크는 **클래스명 패턴**으로 `*IntegrationTest`, `*E2ETest` 제외 — 로컬에 Dev DB가 없기 때문.
- `testUnit`은 추가로 `*ControllerTest`(@WebMvcTest), `*PersistenceAdapterTest`(@DataJpaTest)까지 제외하여 순수 단위 테스트만 남긴다.
- `testIntegration`은 JUnit Tag `integration`을 포함하는 테스트만 실행하며 `spring.profiles.active=integration-test`를 강제한다.
- **AOT 처리 비활성**: 모든 테스트에서 `spring.aot.enabled=false`. `processTestAot` 태스크는 `enabled = false`. MockK/`MockkBean`이 AOT 컴파일과 충돌하기 때문이며, 우회하지 말 것.
- Kover 인스트루멘트는 기본적으로 `testLocal`에만 적용됨. 다른 태스크에도 적용하려면 `-PkoverFull=true`.
- `ignoreFailures = true`로 테스트 실패해도 리포트가 생성된다 — CI에서 "성공"으로 오인하지 말 것.

### 의존성 / 버전 관리 규칙

- 모든 라이브러리/플러그인 버전은 `BE/gradle/libs.versions.toml` (Version Catalog)에서 단일 관리.
- Spring Boot BOM(`spring-boot-dependencies`)이 관리하는 의존성은 카탈로그에 **추가하지 않음**.
- `resolutionStrategy.force`로 강제 고정된 버전이 있음 — 변경 시 반드시 사유 검토:
  - `commons-lang3` 3.20.0 — CVE-2025-48924
  - `classgraph` 4.8.184 — Java 25 `sun.misc.Unsafe` 중단 대응

### 애플리케이션 환경

- 기본 패키지: `com.example.demo260516`, group: `kr.co.businesson`.
- 환경 프로파일: `local`(기본), `dev`, `demo`, `prod`, `integration-test`. `gradle.properties`의 `application.env=local`이 기본값.
- 메인 DB는 MSSQL (dev/demo/prod), 로컬 통합 테스트는 H2, PostgreSQL JDBC도 런타임에 포함.
- 활성화된 Spring 스타터: web, actuator, batch, hateoas, integration, validation, data-rest, data-jpa, **aspectj**(4.0에서 aop 대체), security, mail, amqp. 새 기능 작성 시 이미 있는 스타터 활용.
- `application.yaml`은 거의 비어 있음 — 환경별 설정은 추후 프로파일별로 분리되는 것을 전제로 한다.

## 프론트엔드 (FE/)

### 런타임 / 빌드

- **Bun 전용** — `node`, `npm`, `yarn`, `pnpm`, `npx`, `vite`, `webpack`, `jest`, `vitest` 사용 금지. 자세한 규칙은 `FE/CLAUDE.md` 참조 (이 규칙은 절대적이다).
- React 19 + TypeScript strict. 경로 별칭: `@/*` → `./src/*`.
- 번들링은 Bun이 HTML 직접 import 방식으로 처리 — `Bun.serve({ routes: { "/*": index } })`. 별도 번들러 설정 없음.

### 명령어 (모두 `FE/`에서 실행)

```bash
bun install                              # 의존성 설치
bun dev                                  # 개발 서버 (HMR, src/index.ts)
bun run build                            # 프로덕션 번들 → dist/
bun start                                # 프로덕션 실행
bun test                                 # 테스트 (jest/vitest 아님)
```

### 서버 라우팅

- `FE/src/index.ts`가 Bun 서버 진입점. `Bun.serve()`의 `routes`로 API와 정적 index를 함께 노출.
- 같은 Bun 프로세스가 React 앱과 `/api/*` 엔드포인트를 모두 서빙 — 별도 Express 레이어 없음.
- 환경 변수는 `BUN_PUBLIC_*` 접두사가 클라이언트에 노출됨 (`bunfig.toml`, `package.json`의 build define).

## BE ↔ FE 통합

- 현재 FE는 자체 Bun 서버에서 `/api/hello` 같은 데모 엔드포인트를 직접 제공 — 백엔드와 결합되어 있지 않다. BE 연동을 추가할 때는 CORS/프록시 전략을 먼저 결정해야 한다.
- 루트에 통합 빌드 스크립트는 없음. BE와 FE는 각자의 디렉토리에서 독립적으로 빌드/테스트한다.
