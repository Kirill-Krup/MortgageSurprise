plugins {
    `java-library`
}

group = "com.surprise"
version = "0.0.1-SNAPSHOT"
description = "Shared DTOs for mortgagesurprise and banksimulator"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    api("jakarta.validation:jakarta.validation-api:3.1.1")
    compileOnly("org.projectlombok:lombok:1.18.38")
    annotationProcessor("org.projectlombok:lombok:1.18.38")
}
