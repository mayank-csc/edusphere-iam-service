package com.edusphere.iam.event.producer;

import com.edusphere.iam.event.SagaEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class IdentityEventProducer {

    private final KafkaTemplate<String, SagaEvent> kafkaTemplate;

    @Value("${edusphere.kafka.topics.identity}")
    private String identityTopic;

    public void publishUserCreated(String sagaId, Map<String, Object> payload) {
        publish(sagaId, SagaEvent.USER_CREATED, payload);
    }

    public void publishUserCreationFailed(String sagaId, Map<String, Object> payload) {
        publish(sagaId, SagaEvent.USER_CREATION_FAILED, payload);
    }

    public void publishUserDeleted(String sagaId, Map<String, Object> payload) {
        publish(sagaId, SagaEvent.USER_DELETED, payload);
    }

    private void publish(String sagaId, String eventType, Map<String, Object> payload) {
        SagaEvent event = SagaEvent.of(sagaId, eventType, payload);
        kafkaTemplate.send(identityTopic, sagaId, event);
        log.info("Published {} for saga={}", eventType, sagaId);
    }
}
