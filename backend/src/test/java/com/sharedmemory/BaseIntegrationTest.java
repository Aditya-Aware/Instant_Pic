package com.sharedmemory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
public abstract class BaseIntegrationTest {

    // Deliberately NOT @Container/@Testcontainers-managed: this field is inherited by every
    // integration test subclass, and per-class @Testcontainers lifecycle would start it before
    // one class's tests and STOP it after — then the next subclass restarts a new container
    // (new port) while Spring's ApplicationContext cache reuses the FIRST class's cached context
    // (and its now-dead connection pool), causing every request after the first test class to
    // hang until HikariCP times out. Singleton-container pattern instead: start once, never
    // stop — Testcontainers' Ryuk reaper cleans it up when the JVM exits.
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16")
            .withDatabaseName("smpn_test")
            .withUsername("smpn_test")
            .withPassword("smpn_test");

    static {
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @LocalServerPort
    protected int port;

    protected final HttpClient httpClient = HttpClient.newHttpClient();
    protected final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    protected String baseUrl() {
        return "http://localhost:" + port;
    }

    protected <T> ApiResponse<T> post(String path, String bearerToken, Object body, Class<T> responseType)
            throws IOException, InterruptedException {
        return send("POST", path, bearerToken, body, responseType);
    }

    protected <T> ApiResponse<T> get(String path, String bearerToken, Class<T> responseType)
            throws IOException, InterruptedException {
        return send("GET", path, bearerToken, null, responseType);
    }

    private <T> ApiResponse<T> send(
            String method, String path, String bearerToken, Object body, Class<T> responseType)
            throws IOException, InterruptedException {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl() + path))
                .header("Content-Type", "application/json");
        if (bearerToken != null) {
            builder.header("Authorization", "Bearer " + bearerToken);
        }
        HttpRequest.BodyPublisher publisher = body == null
                ? BodyPublishers.noBody()
                : BodyPublishers.ofString(objectMapper.writeValueAsString(body));
        builder.method(method, publisher);

        HttpResponse<String> response = httpClient.send(builder.build(), BodyHandlers.ofString());
        T parsed = response.body() == null || response.body().isBlank()
                ? null
                : objectMapper.readValue(response.body(), responseType);
        return new ApiResponse<>(response.statusCode(), parsed);
    }

    protected record ApiResponse<T>(int status, T body) {
    }
}
