plugins {
    kotlin("jvm") version "2.1.0"
    kotlin("plugin.spring") version "2.1.0"
    id("io.spring.dependency-management") version "1.1.7"
    id("org.jlleitschuh.gradle.ktlint") version "14.0.1"
    id("pl.allegro.tech.build.axion-release") version "1.18.16"
    `maven-publish`
    `java-library`
}

// Workaround for git submodules (https://github.com/allegro/axion-release-plugin/issues/249)
fun resolveGitDirectory(): String {
    val gitFile = rootProject.file(".git")
    return if (gitFile.isFile) {
        val gitDir = gitFile.readText().substringAfter("gitdir:").trim()
        rootProject.file(gitDir).absolutePath
    } else {
        gitFile.absolutePath
    }
}

scmVersion {
    repository {
        directory.set(resolveGitDirectory())
    }
    tag {
        prefix.set("notifier-api-v")
    }
}

group = "com.smhomelab"
version = scmVersion.version
description = "Notifier API client and DTOs"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
    withSourcesJar()
}

repositories {
    mavenLocal()
    mavenCentral()
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.boot:spring-boot-dependencies:3.4.1")
    }
}

dependencies {
    // Spring Boot
    api("org.springframework.boot:spring-boot-starter-webflux")
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")

    // Resilience4j (retry)
    implementation("io.github.resilience4j:resilience4j-reactor:2.2.0")
    implementation("io.github.resilience4j:resilience4j-retry:2.2.0")

    // Kotlin
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("io.github.oshai:kotlin-logging-jvm:7.0.13")

    // Test
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testImplementation("org.junit.jupiter:junit-jupiter-params")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict")
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            artifactId = "notifier-api"

            pom {
                name.set("notifier-api")
                description.set("Notify Bridge API client library")
                url.set("https://github.com/mshykhov/notify-bridge")

                licenses {
                    license {
                        name.set("MIT License")
                        url.set("https://opensource.org/licenses/MIT")
                    }
                }
            }
        }
    }

    repositories {
        mavenLocal()
    }
}
