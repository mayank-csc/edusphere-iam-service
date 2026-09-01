package com.edusphere.iam.domain.repository;

import com.edusphere.iam.domain.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findBySagaId(String sagaId);
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    Optional<User> findByProviderAndProviderUserId(String provider, String providerUserId);
    boolean existsByEmail(String email);
}
