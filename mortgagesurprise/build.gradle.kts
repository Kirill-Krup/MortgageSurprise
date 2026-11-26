plugins {
	java
	id("org.springframework.boot") version "4.1.2"
	id("io.spring.dependency-management") version "1.1.7"
    kotlin("jvm") version "2.3.0"
    id("io.qameta.allure") version "3.0.0"
}

group = "com.surprise"
version = "0.0.1-SNAPSHOT"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(25)
	}
}

repositories {
	mavenLocal()
	mavenCentral()
}

dependencies {
	implementation("com.surprise:common-lib")
	implementation("org.springframework.boot:spring-boot-starter-amqp")
	implementation("org.springframework.boot:spring-boot-starter-data-mongodb")
	implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-security-oauth2-client")
	implementation("org.springframework.boot:spring-boot-starter-webmvc")
	implementation("org.springframework.boot:spring-boot-starter-data-jpa")
	implementation("org.springframework.boot:spring-boot-starter-data-redis")
    implementation("com.github.ben-manes.caffeine:caffeine")
	implementation("org.springframework.boot:spring-boot-starter-mail")
	implementation("org.springframework.boot:spring-boot-starter-validation")
	implementation("org.springframework.boot:spring-boot-starter-actuator")
	implementation("org.springframework.amqp:spring-rabbit-stream")
	implementation("org.springframework.boot:spring-boot-starter-flyway")
	implementation("org.flywaydb:flyway-core")
	implementation("org.flywaydb:flyway-database-postgresql")
	implementation("org.springframework.cloud:spring-cloud-starter-openfeign:5.0.3")
    implementation("io.jsonwebtoken:jjwt-api:0.12.6")
    implementation("com.github.librepdf:openpdf:2.0.3")
    implementation(platform("net.ttddyy.observation:datasource-micrometer-bom:2.3.0"))
    implementation("org.springframework.boot:spring-boot-starter-opentelemetry")
    implementation("io.opentelemetry:opentelemetry-exporter-otlp")
    implementation("io.opentelemetry:opentelemetry-api-incubator:1.62.0-alpha")
    implementation("net.ttddyy.observation:datasource-micrometer-opentelemetry")
    implementation("net.ttddyy.observation:datasource-micrometer-spring-boot")
    implementation("io.opentelemetry.instrumentation:opentelemetry-logback-appender-1.0:2.21.0-alpha")
    runtimeOnly("io.micrometer:micrometer-registry-prometheus")
	runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.6")
	runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.6")
	runtimeOnly("org.postgresql:postgresql")
	testImplementation("org.springframework.boot:spring-boot-starter-mongodb-test")
	testImplementation("org.springframework.boot:spring-boot-starter-security-test")
	testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
	testImplementation("org.testcontainers:junit-jupiter:1.20.4")
    testImplementation("redis.clients:jedis")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("org.testcontainers:testcontainers-mongodb")
    testImplementation("org.testcontainers:testcontainers-postgresql")
    testImplementation("org.testcontainers:testcontainers-rabbitmq")
    testImplementation("com.redis:testcontainers-redis:2.2.4")
    testImplementation("org.springframework.boot:spring-boot-starter-mail-test")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testCompileOnly("org.projectlombok:lombok")
    testAnnotationProcessor("org.projectlombok:lombok")
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")
    testImplementation("ch.martinelli.oss:testcontainers-mailpit:1.3.0")
    testImplementation("org.wiremock:wiremock-standalone:3.12.1")

    testImplementation("org.assertj:assertj-core:3.26.0")
    testImplementation("com.codeborne:selenide:7.3.2")
    testImplementation("com.squareup.retrofit2:retrofit:2.11.0")
    testImplementation("com.squareup.retrofit2:converter-jackson:2.11.0")
    testImplementation("com.squareup.okhttp3:okhttp:4.12.0")
    testImplementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    testImplementation("io.qameta.allure:allure-junit5:2.27.0")
    testImplementation("io.qameta.allure:allure-selenide:2.27.0")
    testImplementation("io.qameta.allure:allure-okhttp3:2.27.0")
}

tasks.withType<Test> {
	useJUnitPlatform()
}

tasks.register<Test>("unitTest") {
    description = "Runs unit tests."
    group = "verification"
    testClassesDirs = sourceSets["test"].output.classesDirs
    classpath = sourceSets["test"].runtimeClasspath
    useJUnitPlatform {
        excludeTags("integration")
    }
}

tasks.register<Test>("integrationTest") {
    description = "Runs integration tests with Testcontainers."
    group = "verification"
    testClassesDirs = sourceSets["test"].output.classesDirs
    classpath = sourceSets["test"].runtimeClasspath
    useJUnitPlatform {
        includeTags("integration")
    }
}

tasks.withType<Test>().configureEach {
    testLogging {
        events("failed")
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
        showExceptions = true
        showStackTraces = true
    }
}
