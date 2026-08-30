package com.edusphere.iam.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class SagaEvent {

    private String sagaId;
    private String eventType;
    private Map<String, Object> payload;
    private String timestamp;

    // Event types this service produces/consumes
    public static final String USER_CREATION_REQUESTED = "USER_CREATION_REQUESTED";
    public static final String USER_CREATED            = "USER_CREATED";
    public static final String USER_CREATION_FAILED    = "USER_CREATION_FAILED";
    public static final String USER_DELETION_REQUESTED = "USER_DELETION_REQUESTED";
    public static final String USER_DELETED            = "USER_DELETED";

    public static SagaEvent of(String sagaId, String eventType, Map<String, Object> payload) {
        return SagaEvent.builder()
                .sagaId(sagaId)
                .eventType(eventType)
                .payload(payload != null ? payload : new HashMap<>())
                .timestamp(LocalDateTime.now().toString())
                .build();
    }
}
