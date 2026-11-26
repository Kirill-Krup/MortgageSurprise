package com.surprise.mortgagesurprise.qa.util;

import org.junit.jupiter.api.BeforeEach;
import org.testcontainers.containers.ComposeContainer;
import org.testcontainers.containers.wait.strategy.Wait;

import java.net.http.HttpClient;
import java.nio.file.Path;
import java.time.Duration;

public abstract class BaseUiTest {

    private static final Path COMPOSE_FILE = Path.of(
            "..", "..", "..", "..", "..", "..", "..", "..", "..",
            "docker-compose.e2e.yml"
    );

    private static final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public static final ComposeContainer COMPOSE_CONTAINER = new ComposeContainer(COMPOSE_FILE.toFile())
            .withExposedService("frontend", 80, Wait.forHttp("/").withStartupTimeout(Duration.ofMinutes(2)))
            .withExposedService("app", 8080, Wait.forHttp("/actuator/health").withStartupTimeout(Duration.ofMinutes(3)))
            .withExposedService("postgres", 5432, Wait.forListeningPort())
            .withExposedService("mongodb", 27017, Wait.forListeningPort())
            .withExposedService("redis", 6379, Wait.forListeningPort())
            .withExposedService("rabbitmq", 15672, Wait.forHttp("/api/overview").withStartupTimeout(Duration.ofMinutes(2)))
            .withExposedService("mailpit", 8025, Wait.forHttp("/api/v1/info").withStartupTimeout(Duration.ofMinutes(2)));

    @BeforeEach
    void cleanAllState() throws Exception {
        StateCleanerUtil.clean(httpClient, COMPOSE_CONTAINER);
    }
}