package com.edusphere.iam.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.jwt")
@Data
public class JwtProperties {
    private String secret = "change-this-secret-in-production-must-be-256-bits";
    private long expiration = 86400000L;
    private long refreshExpiration = 604800000L;
    private String frontendRedirectUri = "http://localhost:3001/auth/callback";
}
