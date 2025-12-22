plugins {
	kotlin("jvm") version "2.1.0"
	kotlin("plugin.spring") version "2.1.0"
	kotlin("plugin.jpa") version "2.1.0"
	id("org.springframework.boot") version "3.4.1"
	id("io.spring.dependency-management") version "1.1.7"
	id("org.jlleitschuh.gradle.ktlint") version "14.0.1"
	id("pl.allegro.tech.build.axion-release") version "1.18.16"
}

// Workaround for git submodules (https://github.com/allegro/axion-release-plugin/issues/249)
fun resolveGitDirectory(): String {
	val gitFile = file(".git")
	return if (gitFile.isFile) {
		// Submodule: .git is a file containing "gitdir: path/to/git"
		val gitDir = gitFile.readText().substringAfter("gitdir:").trim()
		file(gitDir).absolutePath
	} else {
		gitFile.absolutePath
	}
}

scmVersion {
	repository {
		directory.set(resolveGitDirectory())
	}
	tag {
		prefix.set("v")
	}
}

group = "com.smhomelab"
version = scmVersion.version
description = "Notification service with Telegram and Pushover"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(21)
	}
}

repositories {
    mavenCentral()
    mavenLocal()
    maven {
        url = uri("https://repo.repsy.io/mvn/smhomelub/smhomelab")
        credentials {
            username = findProperty("repoUsername") as String? ?: System.getenv("REPSY_USERNAME")
            password = findProperty("repoPassword") as String? ?: System.getenv("REPSY_PASSWORD")
        }
    }
}

val telegramBotVersion = "0.13.4"

dependencies {
	// Notifier API
	implementation(project(":notifier-api"))

	// Spring Boot
	implementation("org.springframework.boot:spring-boot-starter-actuator")
	implementation("org.springframework.boot:spring-boot-starter-web")
	implementation("org.springframework.boot:spring-boot-starter-data-jpa")
	implementation("org.springframework.boot:spring-boot-starter-security")
	implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
	implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
	implementation("org.jetbrains.kotlin:kotlin-reflect")
	implementation("io.github.oshai:kotlin-logging-jvm:7.0.13")

	// Telegram Bot
	implementation("io.github.dehuckakpyt.telegrambot:telegram-bot-core:$telegramBotVersion")
	implementation("io.github.dehuckakpyt.telegrambot:telegram-bot-spring:$telegramBotVersion")
	implementation("io.github.dehuckakpyt.telegrambot:telegram-bot-source-jpa:$telegramBotVersion")

	// Database
	runtimeOnly("org.postgresql:postgresql")
	implementation("org.flywaydb:flyway-core")
	implementation("org.flywaydb:flyway-database-postgresql")

	// Test
	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
	testImplementation("org.testcontainers:postgresql:1.20.4")
	testImplementation("org.springframework.boot:spring-boot-testcontainers")
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

springBoot {
	buildInfo()
}
