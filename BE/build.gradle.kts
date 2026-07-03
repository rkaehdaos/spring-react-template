import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.spring)
    alias(libs.plugins.kotlin.jpa)
    // Spring Boot Configuration Processor가 java이므로 kapt 유지 필요
    alias(libs.plugins.kotlin.kapt)
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
    alias(libs.plugins.hibernate.orm)
    alias(libs.plugins.graalvm.native)
    alias(libs.plugins.sonarqube)
    alias(libs.plugins.kover)
}

val group = project.property("group") as String
val releaseVer = project.property("releaseVer") as String

version =
    "$releaseVer-${LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))}"
description = "demo260516"


//Java 컴파일러용 설정 - java library, spring 빌드용
tasks.withType<JavaCompile>().configureEach {
    options.compilerArgs.add("-Xlint:unchecked") // 타입 안전성 검사를 엄격
    options.compilerArgs.add("-Xlint:deprecation") // deprecated API 사용 경고
    options.compilerArgs.add("-parameters") // 파라미터 이름 보존 (Spring 등에서 유용)
}
// AOT 컴파일 태스크에서는 생성된 코드의 경고 완전 제거
tasks.named("compileAotJava", JavaCompile::class) {
    options.compilerArgs.remove("-Xlint:unchecked")
    options.compilerArgs.remove("-Xlint:deprecation")
}
// AOT 테스트 컴파일 태스크에서도 생성된 코드의 경고 제거
tasks.named("compileAotTestJava", JavaCompile::class) {
    options.compilerArgs.remove("-Xlint:unchecked")
    options.compilerArgs.remove("-Xlint:deprecation")
}

kotlin {
    jvmToolchain(25)
    compilerOptions {
        freeCompilerArgs.add("-Xjsr305=strict")             //  JSR-305 애노테이션의 null 안정성 어노테이션을 엄격
        freeCompilerArgs.add("-opt-in=kotlin.RequiresOptIn") // 실험적 API등 API를 사용할 때 해당 옵트인 어노테이션 사용을 허용
        allWarningsAsErrors = true
        jvmTarget.set(JvmTarget.JVM_25)
        languageVersion.set(KotlinVersion.KOTLIN_2_3)
        apiVersion.set(KotlinVersion.KOTLIN_2_3)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(platform(libs.spring.boot.dependencies))

    // spring starter (BOM 관리)
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-batch")
    implementation("org.springframework.boot:spring-boot-starter-hateoas")
    implementation("org.springframework.boot:spring-boot-starter-integration")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-data-rest")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-graphql")
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("org.springframework.boot:spring-boot-starter-aspectj")  // Spring Boot 4.0: aop → aspectj
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-mail")
    implementation("org.springframework.boot:spring-boot-starter-amqp")  // RabbitMQ 메시지 큐
    implementation("org.springframework.data:spring-data-rest-hal-explorer")

    // Kotlin - BOM에서 관리되는 버전 사용
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")

    implementation("io.projectreactor.kotlin:reactor-kotlin-extensions")


    // JWT - 인증 토큰 생성 및 검증
    implementation(libs.jjwt.api)
    runtimeOnly(libs.bundles.jjwt.runtime)

    // Test - Spring Boot BOM이 관리
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-resttestclient")  // Spring Boot 4.0: TestRestTemplate 분리
    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")  // Spring Boot 4.0: @WebMvcTest
    testImplementation("org.springframework.boot:spring-boot-starter-data-jpa-test")  // Spring Boot 4.0: @DataJpaTest
    testImplementation("org.springframework.boot:spring-boot-starter-security-test")  // Spring Boot 4.0: Security 테스트
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    // BOM에서 관리하지 않는 테스트 라이브러리 (Version Catalog)
    testImplementation(libs.archunit.junit5)
    // TODO: AI 테스트 수정 후 mockk만 사용하고 아래 제거 할 것
    testImplementation(libs.mockito.kotlin)

    // Kotlin 테스트 라이브러리
    testImplementation(libs.bundles.kotest)
    testImplementation(libs.mockk)
    testImplementation(libs.springmockk)

    // dev only
    developmentOnly("org.springframework.boot:spring-boot-devtools")

    // DB - BOM에서 버전 관리
    // main DB : mssql (dev, demo, prod)
    runtimeOnly("com.microsoft.sqlserver:mssql-jdbc")
    runtimeOnly("org.postgresql:postgresql")
    runtimeOnly("com.h2database:h2")

    // Configuration Processor : 아직 ksp 없는
    kapt("org.springframework.boot:spring-boot-configuration-processor")
}
kapt {
    correctErrorTypes = true
}

tasks.withType<Test> {
    useJUnitPlatform()

    // 병렬 실행 설정 (CPU 코어 절반 사용, 최소 1)
    maxParallelForks = (Runtime.getRuntime().availableProcessors() / 2).coerceAtLeast(1)

    // JVM 설정
    jvmArgs(
        "-XX:+EnableDynamicAgentLoading"  // Java 25 호환성: 동적 agent 로딩 허용
    )
    minHeapSize = "512m"
    maxHeapSize = "2048m"

    // Kotest 타임아웃 설정
    systemProperty("kotest.framework.timeout", "300000")  // 전체 5분
    systemProperty("kotest.framework.invocation.timeout", "60000")  // 개별 1분

    // AOT 컴파일 비활성화: MockkBean과 AOT 충돌 방지
    systemProperty("spring.aot.enabled", "false")

    // 테스트 로깅
    testLogging {
        events("passed", "skipped", "failed")
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
        showStandardStreams = false
    }

    ignoreFailures = true  // 테스트 실패 시에도 리포트 생성 허용
}

// AOT 테스트 태스크 비활성화: MockkBean 사용으로 인한 AOT 컴파일 오류 방지
tasks.named("processTestAot") {
    enabled = false
}

graalvmNative { binaries { configureEach { verbose = true } } }

// bootRun 태스크가 항상 실행되도록 설정 (AOT 캐시로 인한 UP-TO-DATE 방지)
tasks.named<org.springframework.boot.gradle.tasks.run.BootRun>("bootRun") {
    outputs.upToDateWhen { false }
}

configurations.all {
    resolutionStrategy {
        preferProjectModules()
        // CVE-2025-48924 보안 취약점 해결
        force(libs.commons.lang3)
        // Java 25 호환성: sun.misc.Unsafe 사용 중단 이슈 해결
        force(libs.classgraph)
    }
}
sonar {
    properties {
        property("sonar.projectKey", "bizon-tf_smartwork_backend")
        property("sonar.organization", "bizon-tf")
        property("sonar.host.url", "https://sonarcloud.io")
        property("sonar.coverage.jacoco.xmlReportPaths", "build/reports/kover/report.xml")
    }
}

// sonar 태스크가 Kover XML 생성에 의존하도록 설정
tasks.named("sonar") {
    dependsOn("koverXmlReport")
}

// Kover - Kotlin 전용 커버리지 툴 설정
val koverFull = providers
    .gradleProperty("koverFull")
    .map { it.equals("true", ignoreCase = true) }
    .orElse(false)

kover {
    currentProject {
        instrumentation {
            // 로컬 커버리지 측정은 testLocal만 사용 (Dev DB 의존 테스트 제외)
            if (!koverFull.get()) {
                disabledForTestTasks.addAll(
                    "test",
                    "testUnit",
                    "testIntegration",
                    "testAll"
                )
            }
        }
    }
    reports {
        filters {
            // 제외할 패키지 설정
            excludes {
                classes("*.config.*")
                classes("*Application*")
            }
        }
    }
}

// ========================================
// 커스텀 테스트 태스크 (프로파일별 선택적 실행)
// ========================================

// 기본 test 태스크: 통합 테스트 및 E2E 테스트 제외 (로컬 개발 환경에서는 dev DB 접근 불가)
tasks.named<Test>("test") {
    useJUnitPlatform()
    filter {
        // 클래스 이름이 *IntegrationTest 또는 *E2ETest로 끝나는 테스트 제외
        excludeTestsMatching("*IntegrationTest")
        excludeTestsMatching("*E2ETest")
    }
    // 참고: Kover 리포트는 ./gradlew testLocal koverHtmlReport 로 수동 생성
    // 전체 테스트 포함 시: ./gradlew -PkoverFull=true koverHtmlReport
    // (finalizedBy로 자동 연결하면 Kover가 모든 Test 태스크를 실행함)
}

// 순수 단위 테스트만 실행 (Spring Context 없음, 가장 빠름)
tasks.register<Test>("testUnit") {
    description = "순수 단위 테스트만 실행 (Spring Context 없음)"
    group = "verification"

    testClassesDirs = sourceSets["test"].output.classesDirs
    classpath = sourceSets["test"].runtimeClasspath

    useJUnitPlatform()
    filter {
        excludeTestsMatching("*IntegrationTest")
        excludeTestsMatching("*E2ETest")
        excludeTestsMatching("*ControllerTest")          // @WebMvcTest 제외
        excludeTestsMatching("*PersistenceAdapterTest")  // @DataJpaTest 제외
    }

    // 병렬화 설정 (mock 상태 충돌 방지를 위해 적정 수준 유지)
    maxParallelForks = (Runtime.getRuntime().availableProcessors() / 2).coerceIn(1, 4)

    minHeapSize = "256m"
    maxHeapSize = "512m"

    jvmArgs(
        "-XX:+EnableDynamicAgentLoading",
        "-XX:TieredStopAtLevel=1",  // JIT 빠른 시작
        "-XX:+UseParallelGC"
    )

    systemProperty("kotest.framework.timeout", "60000")  // 1분
    systemProperty("kotest.framework.invocation.timeout", "10000")  // 10초
    systemProperty("spring.aot.enabled", "false")

    testLogging {
        events("failed")
        showStandardStreams = false
    }

    reports {
        html.outputLocation.set(layout.buildDirectory.dir("reports/tests/testUnit"))
        junitXml.outputLocation.set(layout.buildDirectory.dir("test-results/testUnit"))
    }
}

// 로컬 개발용 테스트 (단위 + H2 통합)
tasks.register<Test>("testLocal") {
    description = "로컬 개발용 테스트 (단위 + H2 통합)"
    group = "verification"

    testClassesDirs = sourceSets["test"].output.classesDirs
    classpath = sourceSets["test"].runtimeClasspath

    useJUnitPlatform()
    filter {
        excludeTestsMatching("*IntegrationTest")
        excludeTestsMatching("*E2ETest")
    }

    // Spring Context 테스트 고려한 병렬화 (너무 높으면 Context 캐시 경합)
    maxParallelForks = (Runtime.getRuntime().availableProcessors() / 2).coerceIn(1, 4)
    forkEvery = 100  // 100개 테스트마다 새 JVM (메모리 누수 방지)

    jvmArgs(
        "-XX:+EnableDynamicAgentLoading",
        "-XX:+UseG1GC",
        "-XX:MaxGCPauseMillis=100"
    )
    minHeapSize = "512m"
    maxHeapSize = "1536m"  // 2048m -> 1536m

    systemProperty("kotest.framework.timeout", "180000")  // 3분
    systemProperty("kotest.framework.invocation.timeout", "30000")  // 30초
    systemProperty("spring.aot.enabled", "false")
    systemProperty("spring.test.context.cache.maxSize", "4")  // Context 캐시 크기 제한

    testLogging {
        events("failed")  // passed, skipped 제거 (I/O 감소)
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.SHORT
        showStandardStreams = false
    }

    reports {
        html.outputLocation.set(layout.buildDirectory.dir("reports/tests/testLocal"))
        junitXml.outputLocation.set(layout.buildDirectory.dir("test-results/testLocal"))
    }
}

// 통합 테스트만 실행 (Dev DB 연동, integration-test 프로파일)
tasks.register<Test>("testIntegration") {
    description = "통합 테스트만 실행 (@Tag integration, Dev DB 연동)"
    group = "verification"

    // 테스트 소스 세트 명시적으로 설정
    testClassesDirs = sourceSets["test"].output.classesDirs
    classpath = sourceSets["test"].runtimeClasspath

    useJUnitPlatform {
        includeTags("integration")
    }

    // integration-test 프로파일 활성화
    systemProperty("spring.profiles.active", "integration-test")

    // test 태스크와 동일한 설정 상속
    maxParallelForks = (Runtime.getRuntime().availableProcessors() / 2).coerceAtLeast(1)
    jvmArgs("-XX:+EnableDynamicAgentLoading")
    minHeapSize = "512m"
    maxHeapSize = "2048m"
    systemProperty("kotest.framework.timeout", "300000")
    systemProperty("kotest.framework.invocation.timeout", "60000")
    systemProperty("spring.aot.enabled", "false")

    testLogging {
        events("passed", "skipped", "failed")
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
        showStandardStreams = false
    }

    reports {
        html.outputLocation.set(layout.buildDirectory.dir("reports/tests/testIntegration"))
        junitXml.outputLocation.set(layout.buildDirectory.dir("test-results/testIntegration"))
    }
}

// 모든 테스트 실행 (단위 + 통합 + E2E, CI/CD 최종 검증용)
tasks.register<Test>("testAll") {
    description = "모든 테스트 실행 (단위 + 통합 + E2E)"
    group = "verification"

    // 테스트 소스 세트 명시적으로 설정
    testClassesDirs = sourceSets["test"].output.classesDirs
    classpath = sourceSets["test"].runtimeClasspath

    useJUnitPlatform()
    // 필터 없이 모든 테스트 실행

    // integration-test 프로파일 활성화
    systemProperty("spring.profiles.active", "integration-test")

    // test 태스크와 동일한 설정 상속
    maxParallelForks = (Runtime.getRuntime().availableProcessors() / 2).coerceAtLeast(1)
    jvmArgs("-XX:+EnableDynamicAgentLoading")
    minHeapSize = "512m"
    maxHeapSize = "2048m"
    systemProperty("kotest.framework.timeout", "300000")
    systemProperty("kotest.framework.invocation.timeout", "60000")
    systemProperty("spring.aot.enabled", "false")

    testLogging {
        events("passed", "skipped", "failed")
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
        showStandardStreams = false
    }

    reports {
        html.outputLocation.set(layout.buildDirectory.dir("reports/tests/testAll"))
        junitXml.outputLocation.set(layout.buildDirectory.dir("test-results/testAll"))
    }
}
