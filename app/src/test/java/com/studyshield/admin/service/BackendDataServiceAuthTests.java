package com.studyshield.admin.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.studyshield.admin.config.BackendApiProperties;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;

class BackendDataServiceAuthTests {

    private HttpServer server;
    private BackendApiProperties properties;
    private final Map<String, String> lastAuthorization = new ConcurrentHashMap<>();
    private final AtomicInteger signInCalls = new AtomicInteger();

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.setExecutor(Executors.newSingleThreadExecutor());
        server.createContext("/api/auth/signin", exchange -> {
            exchange.getRequestBody().readAllBytes();
            signInCalls.incrementAndGet();
            respond(exchange, 200, "{\"sessionId\":\"tok-live-123\",\"loginId\":\"good@example.com\"}");
        });
        server.createContext("/api/v1/boards", exchange -> {
            exchange.getRequestBody().readAllBytes();
            lastAuthorization.put("/api/v1/boards",
                    exchange.getRequestHeaders().getFirst("Authorization"));
            respond(exchange, 200, "[{\"id\":1,\"name\":\"Std 1\"}]");
        });
        server.start();

        properties = new BackendApiProperties();
        properties.setBaseUrl("http://localhost:" + server.getAddress().getPort());
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    private BackendDataService service(BackendApiProperties props) {
        return new BackendDataService(props, new ObjectMapper(), RestClient.builder());
    }

    private static void respond(com.sun.net.httpserver.HttpExchange exchange, int status, String json)
            throws IOException {
        byte[] body = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.getResponseHeaders().add("Connection", "close");
        exchange.sendResponseHeaders(status, body.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(body);
        }
    }

    @Test
    void signsInOnceAndAttachesBearerTokenWhenCredentialsConfigured() {
        properties.setUsername("good@example.com");
        properties.setPassword("secret");
        BackendDataService service = service(properties);

        List<Map<String, Object>> boards = service.list("boards");

        assertThat(boards).hasSize(1);
        assertThat(signInCalls.get()).isEqualTo(1);
        assertThat(lastAuthorization.get("/api/v1/boards")).isEqualTo("Bearer tok-live-123");
    }

    @Test
    void sendsNoTokenAndSkipsSignInWhenCredentialsMissing() {
        BackendDataService service = service(properties);

        List<Map<String, Object>> boards = service.list("boards");

        assertThat(signInCalls.get()).isZero();
        assertThat(lastAuthorization.get("/api/v1/boards")).isNull();
    }

    @Test
    void reSignsInAfterTokenInvalidation() {
        properties.setUsername("good@example.com");
        properties.setPassword("secret");
        BackendDataService service = service(properties);

        service.list("boards");
        service.invalidateToken();
        service.list("boards");

        assertThat(signInCalls.get()).isEqualTo(2);
        assertThat(lastAuthorization.get("/api/v1/boards")).isEqualTo("Bearer tok-live-123");
    }
}