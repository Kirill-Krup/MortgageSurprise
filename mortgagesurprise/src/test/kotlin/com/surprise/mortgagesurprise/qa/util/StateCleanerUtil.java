package com.surprise.mortgagesurprise.qa.util;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import org.testcontainers.containers.ComposeContainer;
import redis.clients.jedis.Jedis;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StateCleanerUtil {

    public static void clean(HttpClient httpClient, ComposeContainer environment) throws Exception {
        cleanPostgres(environment);
        cleanMongo(environment);
        cleanRedis(environment);
        cleanRabbitMq(environment, httpClient);
        cleanMailpit(environment, httpClient);
    }

    private static void cleanPostgres(ComposeContainer environment) throws Exception {
        String host = environment.getServiceHost("postgres", 5432);
        int port = environment.getServicePort("postgres", 5432);
        String db = envOrDefault("POSTGRES_DB", "mortgagesurprise");
        String user = envOrDefault("POSTGRES_USER", "postgres");
        String password = envOrDefault("POSTGRES_PASSWORD", "postgres");
        String url = "jdbc:postgresql://" + host + ":" + port + "/" + db;

        try (Connection conn = DriverManager.getConnection(url, user, password);
             Statement stmt = conn.createStatement()) {

            List<String> tables = new ArrayList<>();
            try (ResultSet rs = stmt.executeQuery(
                    "SELECT tablename FROM pg_tables WHERE schemaname = 'public' " +
                            "AND tablename NOT IN ('flyway_schema_history', 'databasechangelog', 'databasechangeloglock')"
            )) {
                while (rs.next()) {
                    tables.add("\"" + rs.getString("tablename").replace("\"", "\"\"") + "\"");
                }
            }

            if (!tables.isEmpty()) {
                stmt.execute("TRUNCATE TABLE " + String.join(", ", tables) + " RESTART IDENTITY CASCADE");
            }
        }
    }

    protected static void cleanMongo(ComposeContainer environment) {
        String host = environment.getServiceHost("mongodb", 27017);
        int port = environment.getServicePort("mongodb", 27017);
        String dbName = envOrDefault("MONGO_DB", "mortgagesurprise");
        String uri = "mongodb://" + host + ":" + port;

        try (MongoClient client = MongoClients.create(uri)) {
            MongoDatabase db = client.getDatabase(dbName);
            for (String collectionName : db.listCollectionNames()) {
                db.getCollection(collectionName).deleteMany(new org.bson.Document());
            }
        }
    }

    protected static void cleanRedis(ComposeContainer environment) {
        String host = environment.getServiceHost("redis", 6379);
        int port = environment.getServicePort("redis", 6379);
        String password = System.getenv("REDIS_PASSWORD");

        try (Jedis jedis = new Jedis(host, port)) {
            if (password != null && !password.isBlank()) {
                jedis.auth(password);
            }
            jedis.flushAll();
        }
    }

    protected static void cleanRabbitMq(ComposeContainer environment, HttpClient httpClient) throws Exception {
        String host = environment.getServiceHost("rabbitmq", 15672);
        int port = environment.getServicePort("rabbitmq", 15672);
        String user = envOrDefault("RABBITMQ_USER", "guest");
        String password = envOrDefault("RABBITMQ_PASSWORD", "guest");
        String auth = Base64.getEncoder()
                .encodeToString((user + ":" + password).getBytes(StandardCharsets.UTF_8));

        String baseUrl = "http://" + host + ":" + port;
        HttpRequest listRequest = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/queues"))
                .header("Authorization", "Basic " + auth)
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(listRequest, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IllegalStateException("Failed to list RabbitMQ queues: HTTP " + response.statusCode());
        }

        List<String[]> queues = extractVhostAndQueueNames(response.body());
        for (String[] vhostAndName : queues) {
            String encodedVhost = urlEncodePathSegment(vhostAndName[0]);
            String encodedQueue = urlEncodePathSegment(vhostAndName[1]);
            HttpRequest purgeRequest = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/queues/" + encodedVhost + "/" + encodedQueue + "/contents"))
                    .header("Authorization", "Basic " + auth)
                    .method("DELETE", HttpRequest.BodyPublishers.noBody())
                    .build();
            httpClient.send(purgeRequest, HttpResponse.BodyHandlers.ofString());
        }
    }

    protected static void cleanMailpit(ComposeContainer environment, HttpClient httpClient) throws Exception {
        String host = environment.getServiceHost("mailpit", 8025);
        int port = environment.getServicePort("mailpit", 8025);
        String url = "http://" + host + ":" + port + "/api/v1/messages";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .method("DELETE", HttpRequest.BodyPublishers.ofString("{\"ids\":[]}"))
                .header("Content-Type", "application/json")
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException("Failed to clean Mailpit messages: HTTP " + response.statusCode());
        }
    }

    private static List<String[]> extractVhostAndQueueNames(String json) {
        List<String[]> result = new ArrayList<>();
        Pattern pairPattern = Pattern.compile("\"name\"\\s*:\\s*\"([^\"]+)\"[^}]*?\"vhost\"\\s*:\\s*\"([^\"]+)\"|\"vhost\"\\s*:\\s*\"([^\"]+)\"[^}]*?\"name\"\\s*:\\s*\"([^\"]+)\"");
        Matcher matcher = pairPattern.matcher(json);
        while (matcher.find()) {
            String name = matcher.group(1) != null ? matcher.group(1) : matcher.group(4);
            String vhost = matcher.group(2) != null ? matcher.group(2) : matcher.group(3);
            if (name != null && vhost != null) {
                result.add(new String[]{vhost, name});
            }
        }
        return result;
    }

    private static String urlEncodePathSegment(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private static String envOrDefault(String key, String fallback) {
        String value = System.getenv(key);
        return (value == null || value.isBlank()) ? fallback : value;
    }

    private StateCleanerUtil() {
    }
}