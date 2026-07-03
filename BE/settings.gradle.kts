rootProject.name = "spring-react-template-BE"

// 플러그인 버전은 gradle/libs.versions.toml (Version Catalog) 에서 일괄 관리.
// 여기서는 플러그인 저장소만 선언한다.
pluginManagement {
    repositories {
        gradlePluginPortal()  // Gradle 공식 플러그인 포털
        mavenCentral()        // Maven Central
        maven { url = uri("https://repo.spring.io/milestone") }  // Spring 마일스톤
    }
}

// 멀티 모듈 자를 때
//include("backend")
//include("common")
//include("api")
