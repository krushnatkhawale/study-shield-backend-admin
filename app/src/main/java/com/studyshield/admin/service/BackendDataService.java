package com.studyshield.admin.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.studyshield.admin.config.BackendApiProperties;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class BackendDataService {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public BackendDataService(BackendApiProperties properties, ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.restClient = RestClient.builder()
                .baseUrl(properties.getBaseUrl())
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .build();
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
        } catch (Exception ex) {
            throw new IllegalStateException("Could not update " + collection + " item", ex);
        }
    }

    public void delete(String collection, Long id) {
        restClient.delete()
                .uri("/api/v1/{collection}/{id}", collection, id)
                .retrieve()
                .toBodilessEntity();
    }
}
