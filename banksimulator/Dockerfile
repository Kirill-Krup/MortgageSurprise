FROM gradle:9-jdk25 AS builder
WORKDIR /build
COPY common-lib /common-lib
COPY banksimulator/settings.gradle.kts banksimulator/build.gradle.kts ./
RUN gradle --no-daemon dependencies --configuration runtimeClasspath || true
COPY banksimulator/src ./src
RUN gradle --no-daemon build -x test

FROM eclipse-temurin:25-jre
WORKDIR /app
RUN addgroup --system app && adduser --system --ingroup app --no-create-home app
COPY --from=builder --chown=app:app /build/build/libs/*.jar /app/banksimulator.jar
USER app
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "/app/banksimulator.jar"]
