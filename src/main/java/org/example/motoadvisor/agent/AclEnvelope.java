package org.example.motoadvisor.agent;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.*;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Standard ACL JSON contract used between JADE agents.
 * Envelope fields: requestId, type, payload, errors.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AclEnvelope implements Serializable {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private String requestId;
    private String type;

    @Builder.Default
    private Map<String, Object> payload = new HashMap<>();

    @Builder.Default
    private List<String> errors = List.of();

    public String toJson() {
        try {
            return MAPPER.writeValueAsString(this);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Cannot serialize ACL envelope", e);
        }
    }

    public static AclEnvelope fromJson(String json) {
        try {
            return MAPPER.readValue(json, AclEnvelope.class);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid ACL envelope JSON", e);
        }
    }

    public static AclEnvelope ok(String requestId, String type, Map<String, Object> payload) {
        return AclEnvelope.builder()
                .requestId(requestId)
                .type(type)
                .payload(payload)
                .errors(List.of())
                .build();
    }

    public static AclEnvelope error(String requestId, String type, String message) {
        return AclEnvelope.builder()
                .requestId(requestId)
                .type(type)
                .payload(Map.of())
                .errors(List.of(message))
                .build();
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> payloadList(String key) {
        Object value = payload.get(key);
        if (value == null) {
            return List.of();
        }
        return MAPPER.convertValue(value, new TypeReference<>() {
        });
    }
}

