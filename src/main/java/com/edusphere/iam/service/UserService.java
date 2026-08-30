package com.edusphere.iam.service;

import com.edusphere.iam.domain.entity.User;
import com.edusphere.iam.domain.repository.UserRepository;
import com.edusphere.iam.event.SagaEvent;
import com.edusphere.iam.event.producer.IdentityEventProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final IdentityEventProducer eventProducer;
    private final BCryptPasswordEncoder passwordEncoder;

    @Transactional
    public void handleUserCreation(SagaEvent event) {
        Map<String, Object> payload = event.getPayload();
        String sagaId    = event.getSagaId();
        String email     = (String) payload.get("adminEmail");
        String username  = (String) payload.get("username");
        String password  = (String) payload.get("password");
        String firstName = (String) payload.get("adminFirstName");
        String lastName  = (String) payload.get("adminLastName");
        String tenantId  = (String) payload.get("tenantId");

        log.info("Creating user for saga={} email={}", sagaId, email);

        try {
            if (userRepository.existsByEmail(email)) {
                throw new IllegalStateException("User with email already exists: " + email);
            }

            User user = User.builder()
                    .sagaId(sagaId)
                    .tenantId(tenantId)
                    .username(username)
                    .passwordHash(passwordEncoder.encode(password))
                    .firstName(firstName)
                    .lastName(lastName)
                    .email(email)
                    .role("TENANT_ADMIN")
                    .active(true)
                    .build();

            User saved = userRepository.save(user);
            log.info("User created id={} for saga={}", saved.getId(), sagaId);

            eventProducer.publishUserCreated(sagaId, Map.of(
                    "userId", String.valueOf(saved.getId()),
                    "email", email,
                    "tenantId", tenantId
            ));

        } catch (Exception e) {
            log.error("User creation failed for saga={}: {}", sagaId, e.getMessage());
            eventProducer.publishUserCreationFailed(sagaId, Map.of(
                    "reason", e.getMessage(),
                    "email", email
            ));
        }
    }

    @Transactional
    public void handleUserDeletion(SagaEvent event) {
        String sagaId = event.getSagaId();
        log.info("Compensating: deleting user for saga={}", sagaId);
        userRepository.findBySagaId(sagaId).ifPresent(user -> {
            user.setActive(false);
            userRepository.save(user);
            log.info("User deactivated for saga={}", sagaId);
            eventProducer.publishUserDeleted(sagaId, Map.of("userId", String.valueOf(user.getId())));
        });
    }
}
