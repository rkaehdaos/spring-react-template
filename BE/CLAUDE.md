# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 언어

모든 응답과 문서 작성은 한국어를 사용한다.

## 프로젝트 개요

Spring Boot 4.1 + Kotlin 2.4 백엔드 (모노레포의 `BE/` 디렉터리, 프론트엔드는 `../FE/`의 Bun + React 19).
JVM 툴체인은 Java 25 (mise가 `oracle-graalvm-25.0.3` 관리, `mise.toml` 참조). GraalVM Native Image 빌드 지원.

## 자주 쓰는 명령어

```bash
./gradlew bootRun                # 앱 실행 (항상 재실행되도록 UP-TO-DATE 비활성화됨)
./gradlew build                  # 빌드
./gradlew test                   # 기본 테스트 (*IntegrationTest, *E2ETest 제외)
./gradlew testUnit               # 순수 단위 테스트만 (Spring Context 없음, 가장 빠름)
./gradlew testLocal              # 로컬 개발용 (단위 + H2 통합)
./gradlew testIntegration        # @Tag("integration") 테스트만, integration-test 프로파일 (Dev DB 필요)
./gradlew testAll                # 전체 테스트 (CI/CD 최종 검증용)
./gradlew test --tests "com.example.demo260516.SomeTest"   # 단일 테스트 클래스
./gradlew testLocal koverHtmlReport                         # 커버리지 (전체 포함: -PkoverFull=true)
./gradlew nativeCompile          # GraalVM 네이티브 이미지
```

주의: 모든 Test 태스크에 `ignoreFailures = true`가 설정되어 있어 **테스트가 실패해도 Gradle은 성공으로 끝난다**. 반드시 출력/리포트에서 실패 여부를 직접 확인할 것.

## 테스트 규약

- 테스트 클래스 네이밍이 태스크 필터링을 결정: `*IntegrationTest`(Dev DB 통합), `*E2ETest`, `*ControllerTest`(@WebMvcTest), `*PersistenceAdapterTest`(@DataJpaTest). 이 접미사에 맞춰 클래스명을 지어야 올바른 태스크에서 실행된다.
- 통합 테스트는 클래스명 접미사 외에 JUnit `@Tag("integration")`도 필요 (testIntegration이 태그로 include).
- 테스트 프레임워크: Kotest + MockK/SpringMockK 사용이 목표. mockito-kotlin은 제거 예정(TODO)이므로 새 테스트에 사용하지 말 것.
- MockkBean과 AOT 충돌 때문에 테스트에서 `spring.aot.enabled=false`이며 `processTestAot` 태스크는 비활성화됨.
- ArchUnit(archunit-junit5)으로 아키텍처 테스트 가능.

## 빌드 구조

- 버전 관리 단일 진입점: `gradle/libs.versions.toml` (Version Catalog). Spring Boot BOM이 관리하는 라이브러리는 카탈로그에 두지 않고 버전 없이 선언한다. 의존성 추가/업그레이드 시 이 규칙을 따를 것.
- Kotlin 컴파일: `allWarningsAsErrors = true` — 경고 하나라도 빌드 실패. `-Xjsr305=strict`.
- `kapt`는 spring-boot-configuration-processor 전용 (KSP 미지원이라 유지).
- 프로젝트 버전은 `gradle.properties`의 `releaseVer` + 빌드 타임스탬프로 자동 생성.
- `org.gradle.dependency.verification=strict` — 새 의존성 추가 시 검증 메타데이터 갱신 필요할 수 있음.
- Configuration Cache 활성화 상태.

## 환경 / DB

- 환경: local, dev, demo, prod (`gradle.properties`의 `application.env`).
- 메인 DB는 MSSQL(dev/demo/prod), 로컬 테스트는 H2, PostgreSQL 드라이버도 포함.
- 정적 분석: SonarCloud (`./gradlew sonar`, Kover XML 리포트 자동 연동).
