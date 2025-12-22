plugins {
    kotlin("jvm") version "2.1.0"
    kotlin("plugin.spring") version "2.1.0"
    id("io.spring.dependency-management") version "1.1.7"
    id("org.jlleitschuh.gradle.ktlint") version "14.0.1"
    id("pl.allegro.tech.build.axion-release") version "1.18.16"
    `maven-publish`
    `java-library`
}

scmVersion {
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
    maven {
        url = uri("https://repo.repsy.io/mvn/smhomelub/smhomelab")
        credentials {
            username = System.getenv("REPSY_USERNAME")
                ?: providers.gradleProperty("repoUsername").orNull
                ?: ""
            password = System.getenv("REPSY_TOKEN")
                ?: providers.gradleProperty("repoPassword").orNull
                ?: ""
        }
    }
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.boot:spring-boot-dependencies:3.4.1")
    }
}

dependencies {
    // REST Client
    api("com.smhomelab:rest-client:0.1.3")

    // Spring Boot
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")

    // Kotlin
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("io.github.oshai:kotlin-logging-jvm:7.0.13")

    // Test
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
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
                description.set("Notifier API client and DTOs for smhomelab")
                url.set("https://github.com/mshykhov/smhomelab-notifier")

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
        maven {
            name = "Repsy"
            url = uri("https://repo.repsy.io/mvn/smhomelub/smhomelab")
            credentials {
                username = System.getenv("REPSY_USERNAME")
                    ?: providers.gradleProperty("repoUsername").orNull
                    ?: ""
                password = System.getenv("REPSY_TOKEN")
                    ?: providers.gradleProperty("repoPassword").orNull
                    ?: ""
            }
        }
    }
}
