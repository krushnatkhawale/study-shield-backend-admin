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

    private final BackendApiProperties properties;
    private final ObjectMapper objectMapper;
    private final RestClient restClient;

    private final Object authLock = new Object();
    private volatile String bearerToken;
    private volatile long lastAuthAttempt = 0L;

    public BackendDataService(BackendApiProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(properties.getConnectTimeoutMs());
        requestFactory.setReadTimeout(properties.getReadTimeoutMs());
        this.restClient = RestClient.builder()
                .baseUrl(properties.getBaseUrl())
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
                        .uri("/api/auth/signin")
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
    }

    public List<Map<String, Object>> list(String collection) {
        try {
            String body = restClient.get()
                    .uri("/api/v1/" + collection)
                    .retrieve()
                    .body(String.class);
            if (body == null || body.isBlank()) {
                return List.of();
            }
            return objectMapper.readValue(body, new TypeReference<List<Map<String, Object>>>() {});
        } catch (RestClientResponseException ex) {
            invalidateToken();
            log.warn("GET /api/v1/{} failed ({} {})", collection,
                    ex.getStatusCode().value(), ex.getStatusCode());
            return List.of();
        } catch (Exception ex) {
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
            invalidateToken();
            log.warn("GET /api/v1/{}/{} failed ({} {})", collection, id,
                    ex.getStatusCode().value(), ex.getStatusCode());
            return new LinkedHashMap<>();
        } catch (Exception ex) {
            return new LinkedHashMap<>();
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
            invalidateToken();
            log.warn("GET /api/v1/{}/{}/{} failed ({} {})", collection, childPath, parentId,
                    ex.getStatusCode().value(), ex.getStatusCode());
            return List.of();
        } catch (Exception ex) {
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
            invalidateToken();
            log.warn("GET /api/v1/{}/{}/{} failed ({} {})", path, childPath, parentId,
                    ex.getStatusCode().value(), ex.getStatusCode());
            return List.of();
        } catch (Exception ex) {
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
            invalidateToken();
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
            invalidateToken();
            log.warn("PUT /api/v1/{}/{} failed ({} {})", collection, id,
                    ex.getStatusCode().value(), ex.getStatusCode());
            throw new IllegalStateException("Could not update " + collection + " item (HTTP "
                    + ex.getStatusCode().value() + ")", ex);
        } catch (Exception ex) {
            throw new IllegalStateException("Could not update " + collection + " item", ex);
        }
    }

    public void delete(String collection, Long id) {
        try {
            restClient.delete()
                    .uri("/api/v1/{collection}/{id}", collection, id)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException ex) {
            invalidateToken();
            log.warn("DELETE /api/v1/{}/{} failed ({} {})", collection, id,
                    ex.getStatusCode().value(), ex.getStatusCode());
            throw new IllegalStateException("Could not delete " + collection + " item (HTTP "
                    + ex.getStatusCode().value() + ")", ex);
        }
    }
}
