package com.edusphere.iam.event.consumer;

import com.edusphere.iam.event.SagaEvent;
import com.edusphere.iam.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class IdentityEventConsumer {

    private final UserService userService;

    @KafkaListener(
        topics = "${edusphere.kafka.topics.identity}",
        groupId = "iam-service-group",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void consume(SagaEvent event) {
        log.info("Received identity event: type={} sagaId={}", event.getEventType(), event.getSagaId());

        switch (event.getEventType()) {
            case SagaEvent.USER_CREATION_REQUESTED -> userService.handleUserCreation(event);
            case SagaEvent.USER_DELETION_REQUESTED -> userService.handleUserDeletion(event);
            default -> log.debug("Ignoring event type={}", event.getEventType());
        }
    }
}
