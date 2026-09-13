package com.studyshield.admin.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.studyshield.admin.config.BackendApiProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class BackendDataService {

    private static final Logger log = LoggerFactory.getLogger(BackendDataService.class);
    private static final long AUTH_RETRY_AFTER_MS = 60_000L;

    /** Collections whose list endpoints are legacy mobile resources not under /api/v1. */
    private static final List<String> NON_V1_COLLECTIONS = List.of("quiz-results", "students");

    private String collectionPath(String collection) {
        return (NON_V1_COLLECTIONS.contains(collection) ? "/api/" : "/api/v1/") + collection;
    }

    /**
     * Clears the shared token only when the failure is an authentication failure (401),
     * so a missing or broken data endpoint (404/405/500) cannot silently kill the session.
     */
    private void invalidateIfAuthFailure(RestClientResponseException ex) {
        if (ex.getStatusCode().value() == 401) {
            invalidateToken();
        }
    }

    private final BackendApiProperties properties;
    private final ObjectMapper objectMapper;
    private final RestClient.Builder restClientBuilder;
    private volatile RestClient restClient;
    private volatile String environment;

    private final Object authLock = new Object();
    private volatile String bearerToken;
    private volatile long lastAuthAttempt = 0L;

    public BackendDataService(BackendApiProperties properties, ObjectMapper objectMapper, RestClient.Builder builder) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.restClientBuilder = builder;
        this.environment = BackendApiProperties.ENV_DEV;
        this.restClient = buildRestClient(properties.baseUrlFor(environment));
    }

    private RestClient buildRestClient(String baseUrl) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(properties.getConnectTimeoutMs());
        requestFactory.setReadTimeout(properties.getReadTimeoutMs());
        return restClientBuilder
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .requestFactory(requestFactory)
                .requestInterceptor((request, body, execution) -> {
                    String token = getBearerToken();
                    if (token != null) {
                        request.getHeaders().setBearerAuth(token);
                    }
                    return execution.execute(request, body);
                })
                .build();
    }

    /** The environments the admin console can operate against (e.g. dev, prod). */
    public java.util.List<String> getEnvironments() {
        return properties.environments();
    }

    /** The currently selected environment (dev or prod). */
    public String getEnvironment() {
        return environment;
    }

    /** Backend base URL for the currently selected environment. */
    public String getActiveBaseUrl() {
        return properties.baseUrlFor(environment);
    }

    /**
     * Switches the environment for all subsequent admin console operations.
     * Each environment maps to its own backend base URL; because the backend
     * resolves its schema from its runtime profile (default=ss-dev, prod=ss-prod),
     * switching here moves all CRUD onto the selected environment's schema.
     * The cached bearer token is cleared so the new backend re-authenticates.
     */
    public void setEnvironment(String env) {
        String baseUrl = properties.baseUrlFor(env);
        this.environment = env;
        this.restClient = buildRestClient(baseUrl);
        invalidateToken();
    }

    private String getBearerToken() {
        String token = bearerToken;
        if (token != null) {
            return token;
        }
        synchronized (authLock) {
            if (bearerToken != null) {
                return bearerToken;
            }
            if (!properties.isAuthConfigured()) {
                return null;
            }
            long now = System.currentTimeMillis();
            if (now - lastAuthAttempt < AUTH_RETRY_AFTER_MS) {
                return null;
            }
            lastAuthAttempt = now;
            try {
                String json = objectMapper.writeValueAsString(Map.of(
                        "loginId", properties.getUsername(),
                        "password", properties.getPassword()));
                String body = restClient.post()
                        .uri("/api/auth/admin-signin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(json)
                        .retrieve()
                        .body(String.class);
                JsonNode node = objectMapper.readTree(body);
                if (node.hasNonNull("sessionId") && !node.path("sessionId").asText().isBlank()) {
                    bearerToken = node.path("sessionId").asText();
                    log.info("Backend JWT sign-in ok (loginId={})", properties.getUsername());
                } else {
                    log.warn("Backend sign-in rejected: {}",
                            node.path("message").asText(node.path("errorCode").asText("unknown error")));
                }
            } catch (Exception ex) {
                log.warn("Backend sign-in failed: {}", ex.getMessage());
            }
            return bearerToken;
        }
    }

    public void invalidateToken() {
        bearerToken = null;
        lastAuthAttempt = 0L;
    }

    /**
     * Verifies admin credentials against the backend and adopts the admin session token
     * for all subsequent API calls. Used by the login flow (replaces local in-memory users).
     */
    public AuthenticatedAdmin authenticateAdmin(String loginId, String password) {
        try {
            String json = objectMapper.writeValueAsString(Map.of(
                    "loginId", loginId,
                    "password", password));
            String body = restClient.post()
                    .uri("/api/auth/admin-signin")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(json)
                    .retrieve()
                    .body(String.class);
            JsonNode node = objectMapper.readTree(body);
            if (node.hasNonNull("sessionId") && !node.path("sessionId").asText().isBlank()) {
                bearerToken = node.path("sessionId").asText();
                lastAuthAttempt = 0L;
                log.info("Admin sign-in ok (loginId={})", loginId);
                return new AuthenticatedAdmin(
                        node.path("accountId").asText(), loginId, bearerToken);
            }
            String reason = node.path("message").asText(
                    node.path("errorCode").asText("Sign in rejected"));
            throw new BadCredentialsException(reason);
        } catch (BadCredentialsException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BadCredentialsException("Could not reach the backend sign-in endpoint", ex);
        }
    }

    public record AuthenticatedAdmin(String accountId, String loginId, String sessionToken) {}

    public List<Map<String, Object>> list(String collection) {
        String path = collectionPath(collection);
        try {
            String body = restClient.get()
                    .uri(path)
                    .retrieve()
                    .body(String.class);
            if (body == null || body.isBlank()) {
                return List.of();
            }
            return objectMapper.readValue(body, new TypeReference<List<Map<String, Object>>>() {});
        } catch (RestClientResponseException ex) {
            invalidateIfAuthFailure(ex);
            log.warn("GET {} failed ({} {})", path,
                    ex.getStatusCode().value(), ex.getStatusCode());
            return List.of();
        } catch (Exception ex) {
            log.warn("GET {} failed: {}", path, ex.toString());
            return List.of();
        }
    }

    public Map<String, Object> get(String collection, Long id) {
        try {
            String body = restClient.get()
                    .uri("/api/v1/{collection}/{id}", collection, id)
                    .retrieve()
                    .body(String.class);
            if (body == null || body.isBlank()) {
                return new LinkedHashMap<>();
            }
            return objectMapper.readValue(body, new TypeReference<Map<String, Object>>() {});
        } catch (RestClientResponseException ex) {
            invalidateIfAuthFailure(ex);
            log.warn("GET /api/v1/{}/{} failed ({} {})", collection, id,
                    ex.getStatusCode().value(), ex.getStatusCode());
            return new LinkedHashMap<>();
        } catch (Exception ex) {
            log.warn("GET /api/v1/{}/{} failed: {}", collection, id, ex.toString());
            return new LinkedHashMap<>();
        }
    }

    /** Full revision history of a question, oldest first. */
    public List<Map<String, Object>> listRevisions(Long questionId) {
        try {
            String body = restClient.get()
                    .uri("/api/v1/questions/{id}/revisions", questionId)
                    .retrieve()
                    .body(String.class);
            if (body == null || body.isBlank()) {
                return List.of();
            }
            return objectMapper.readValue(body, new TypeReference<List<Map<String, Object>>>() {});
        } catch (RestClientResponseException ex) {
            invalidateIfAuthFailure(ex);
            log.warn("GET /api/v1/questions/{}/revisions failed ({} {})", questionId,
                    ex.getStatusCode().value(), ex.getStatusCode());
            return List.of();
        } catch (Exception ex) {
            log.warn("GET /api/v1/questions/{}/revisions failed: {}", questionId, ex.toString());
            return List.of();
        }
    }

    /** Fetch resources that belong to a parent resource, e.g. subjects for a class grade. */
    public List<Map<String, Object>> listBy(String collection, String childPath, Long parentId) {
        try {
            String body = restClient.get()
                    .uri("/api/v1/{collection}/{childPath}/{id}", collection, childPath, parentId)
                    .retrieve()
                    .body(String.class);
            if (body == null || body.isBlank()) {
                return List.of();
            }
            return objectMapper.readValue(body, new TypeReference<List<Map<String, Object>>>() {});
        } catch (RestClientResponseException ex) {
            invalidateIfAuthFailure(ex);
            log.warn("GET /api/v1/{}/{}/{} failed ({} {})", collection, childPath, parentId,
                    ex.getStatusCode().value(), ex.getStatusCode());
            return List.of();
        } catch (Exception ex) {
            log.warn("GET /api/v1/{}/{}/{} failed: {}", collection, childPath, parentId, ex.toString());
            return List.of();
        }
    }

    /** Fetch a single resource that hangs off a parent path, e.g. quizzes for a content pack. */
    public List<Map<String, Object>> listByPath(String path, String childPath, Long parentId) {
        try {
            String body = restClient.get()
                    .uri("/api/v1/{path}/{childPath}/{id}", path, childPath, parentId)
                    .retrieve()
                    .body(String.class);
            if (body == null || body.isBlank()) {
                return List.of();
            }
            return objectMapper.readValue(body, new TypeReference<List<Map<String, Object>>>() {});
        } catch (RestClientResponseException ex) {
            invalidateIfAuthFailure(ex);
            log.warn("GET /api/v1/{}/{}/{} failed ({} {})", path, childPath, parentId,
                    ex.getStatusCode().value(), ex.getStatusCode());
            return List.of();
        } catch (Exception ex) {
            log.warn("GET /api/v1/{}/{}/{} failed: {}", path, childPath, parentId, ex.toString());
            return List.of();
        }
    }

    public Map<String, Object> create(String collection, Map<String, Object> payload) {
        try {
            String json = objectMapper.writeValueAsString(payload);
            String response = restClient.post()
                    .uri("/api/v1/" + collection)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(json)
                    .retrieve()
                    .body(String.class);
            if (response == null || response.isBlank()) {
                return payload;
            }
            return objectMapper.readValue(response, new TypeReference<>() {});
        } catch (RestClientResponseException ex) {
            invalidateIfAuthFailure(ex);
            log.warn("POST /api/v1/{} failed ({} {})", collection,
                    ex.getStatusCode().value(), ex.getStatusCode());
            throw new IllegalStateException("Could not create " + collection + " item (HTTP "
                    + ex.getStatusCode().value() + ")", ex);
        } catch (Exception ex) {
            throw new IllegalStateException("Could not create " + collection + " item", ex);
        }
    }

    public Map<String, Object> update(String collection, Long id, Map<String, Object> payload) {
        try {
            String json = objectMapper.writeValueAsString(payload);
            String response = restClient.put()
                    .uri("/api/v1/{collection}/{id}", collection, id)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(json)
                    .retrieve()
                    .body(String.class);
            if (response == null || response.isBlank()) {
                return payload;
            }
            return objectMapper.readValue(response, new TypeReference<Map<String, Object>>() {});
        } catch (RestClientResponseException ex) {
            invalidateIfAuthFailure(ex);
            log.warn("PUT /api/v1/{}/{} failed ({} {})", collection, id,
                    ex.getStatusCode().value(), ex.getStatusCode());
            throw new IllegalStateException("Could not update " + collection + " item (HTTP "
                    + ex.getStatusCode().value() + ")", ex);
        } catch (Exception ex) {
            throw new IllegalStateException("Could not update " + collection + " item", ex);
        }
    }

    public Map<String, Object> postPath(String relativePath, Map<String, Object> payload) {
        try {
            String json = payload == null ? "{}" : objectMapper.writeValueAsString(payload);
            String response = restClient.post()
                    .uri("/api/v1/" + relativePath)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(json)
                    .retrieve()
                    .body(String.class);
            if (response == null || response.isBlank()) {
                return new LinkedHashMap<>();
            }
            return objectMapper.readValue(response, new TypeReference<Map<String, Object>>() {});
        } catch (RestClientResponseException ex) {
            invalidateIfAuthFailure(ex);
            throw new IllegalStateException("POST /api/v1/" + relativePath + " failed (HTTP "
                    + ex.getStatusCode().value() + ")", ex);
        } catch (Exception ex) {
            throw new IllegalStateException("POST /api/v1/" + relativePath + " failed", ex);
        }
    }

    public void delete(String collection, Long id) {
        try {
            restClient.delete()
                    .uri("/api/v1/{collection}/{id}", collection, id)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException ex) {
            invalidateIfAuthFailure(ex);
            log.warn("DELETE /api/v1/{}/{} failed ({} {})", collection, id,
                    ex.getStatusCode().value(), ex.getStatusCode());
            throw new IllegalStateException("Could not delete " + collection + " item (HTTP "
                    + ex.getStatusCode().value() + ")", ex);
        }
    }
}
