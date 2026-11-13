plugins {
	java
	id("org.springframework.boot") version "4.1.2"
	id("io.spring.dependency-management") version "1.1.7"
}

group = "com.surprise"
version = "0.0.1-SNAPSHOT"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(25)
	}
}

repositories {
	mavenCentral()
}

dependencies {
	implementation("org.springframework.boot:spring-boot-starter-amqp")
	implementation("org.springframework.boot:spring-boot-starter-data-mongodb")
	implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-security-oauth2-client")
	implementation("org.springframework.boot:spring-boot-starter-webmvc")
	implementation("org.springframework.boot:spring-boot-starter-data-jpa")
	implementation("org.springframework.boot:spring-boot-starter-data-redis")
	implementation("org.springframework.boot:spring-boot-starter-mail")
	implementation("org.springframework.boot:spring-boot-starter-validation")
	implementation("org.springframework.boot:spring-boot-starter-actuator")
	implementation("org.springframework.amqp:spring-rabbit-stream")
	implementation("org.springframework.boot:spring-boot-starter-flyway")
	implementation("org.flywaydb:flyway-core")
	implementation("org.flywaydb:flyway-database-postgresql")
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
	testImplementation("org.testcontainers:postgresql:1.20.4")
	testImplementation("org.testcontainers:mongodb:1.20.4")
	testImplementation("org.testcontainers:rabbitmq:1.20.4")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test> {
	useJUnitPlatform()
}
