package com.edusphere.iam.service;

import com.edusphere.iam.config.JwtProperties;
import com.edusphere.iam.domain.entity.RefreshToken;
import com.edusphere.iam.domain.entity.User;
import com.edusphere.iam.domain.repository.RefreshTokenRepository;
import com.edusphere.iam.domain.repository.UserRepository;
import com.edusphere.iam.dto.AuthResponse;
import com.edusphere.iam.dto.LoginRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;
    private final OAuth2Service oauth2Service;
    private final BCryptPasswordEncoder passwordEncoder;
    private final JwtProperties jwtProperties;

    @Transactional
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("Invalid email or password"));

        if (!user.isActive())
            throw new IllegalStateException("Account is disabled");

        if (!"LOCAL".equals(user.getProvider()))
            throw new IllegalStateException("Please sign in with " + user.getProvider());

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash()))
            throw new IllegalArgumentException("Invalid email or password");

        return issueTokenPair(user);
    }

    @Transactional
    public AuthResponse loginWithOAuth2(String code, String provider) {
        Map<String, Object> userInfo = oauth2Service.getUserInfo(code, provider);
        String email          = oauth2Service.extractEmail(userInfo);
        String providerUserId = oauth2Service.extractProviderUserId(userInfo, provider);

        User user = userRepository.findByProviderAndProviderUserId(provider.toUpperCase(), providerUserId)
                .orElseGet(() -> userRepository.findByEmail(email)
                        .map(existing -> linkOAuthProvider(existing, provider, providerUserId))
                        .orElseThrow(() -> new IllegalStateException(
                                "No account found for " + email + ". Please complete onboarding first.")));

        if (!user.isActive())
            throw new IllegalStateException("Account is disabled");

        return issueTokenPair(user);
    }

    @Transactional
    public AuthResponse refreshToken(String refreshTokenValue) {
        RefreshToken stored = refreshTokenRepository.findByToken(refreshTokenValue)
                .orElseThrow(() -> new IllegalArgumentException("Invalid refresh token"));

        if (stored.isExpired()) {
            refreshTokenRepository.delete(stored);
            throw new IllegalArgumentException("Refresh token expired, please login again");
        }

        User user = userRepository.findById(stored.getUserId())
                .orElseThrow(() -> new IllegalStateException("User not found"));

        // rotate refresh token
        refreshTokenRepository.delete(stored);
        return issueTokenPair(user);
    }

    private User linkOAuthProvider(User user, String provider, String providerUserId) {
        user.setProvider(provider.toUpperCase());
        user.setProviderUserId(providerUserId);
        return userRepository.save(user);
    }

    private AuthResponse issueTokenPair(User user) {
        String accessToken  = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        refreshTokenRepository.deleteByUserId(user.getId());
        refreshTokenRepository.save(RefreshToken.builder()
                .token(refreshToken)
                .userId(user.getId())
                .expiresAt(LocalDateTime.now().plusSeconds(jwtProperties.getRefreshExpiration() / 1000))
                .build());

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtProperties.getExpiration() / 1000)
                .userId(String.valueOf(user.getId()))
                .tenantId(user.getTenantId())
                .role(user.getRole())
                .email(user.getEmail())
                .build();
    }
}
