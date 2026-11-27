FROM gradle:9-jdk25 AS builder
WORKDIR /build
COPY common-lib /common-lib
COPY mortgagesurprise/settings.gradle.kts mortgagesurprise/build.gradle.kts ./
RUN gradle --no-daemon dependencies --configuration runtimeClasspath || true
COPY mortgagesurprise/src ./src
RUN gradle --no-daemon build -x test
FROM eclipse-temurin:25-jre
WORKDIR /app
RUN addgroup --system app && adduser --system --ingroup app --no-create-home app
COPY --from=builder --chown=app:app /build/build/libs/*.jar /app/mortgagesurprise.jar
USER app
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/mortgagesurprise.jar"]