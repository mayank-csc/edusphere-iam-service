package com.edusphere.iam.service;

import com.edusphere.iam.config.OAuth2Properties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class OAuth2Service {

    private final OAuth2Properties oauth2Properties;
    private final RestClient restClient = RestClient.create();

    public String buildAuthorizationUrl(String provider) {
        OAuth2Properties.ProviderConfig config = getConfig(provider);
        return config.getAuthUri()
                + "?client_id=" + config.getClientId()
                + "&redirect_uri=" + config.getRedirectUri()
                + "&response_type=code"
                + "&scope=" + config.getScope().replace(" ", "%20")
                + "&state=" + provider;
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> getUserInfo(String code, String provider) {
        OAuth2Properties.ProviderConfig config = getConfig(provider);

        MultiValueMap<String, String> tokenRequest = new LinkedMultiValueMap<>();
        tokenRequest.add("grant_type", "authorization_code");
        tokenRequest.add("code", code);
        tokenRequest.add("redirect_uri", config.getRedirectUri());
        tokenRequest.add("client_id", config.getClientId());
        tokenRequest.add("client_secret", config.getClientSecret());

        Map<String, Object> tokenResponse = restClient.post()
                .uri(config.getTokenUri())
                .header("Content-Type", "application/x-www-form-urlencoded")
                .body(tokenRequest)
                .retrieve()
                .body(Map.class);

        String accessToken = (String) tokenResponse.get("access_token");

        return restClient.get()
                .uri(config.getUserinfoUri())
                .header("Authorization", "Bearer " + accessToken)
                .retrieve()
                .body(Map.class);
    }

    public String extractEmail(Map<String, Object> userInfo) {
        return (String) userInfo.get("email");
    }

    public String extractProviderUserId(Map<String, Object> userInfo, String provider) {
        return switch (provider.toLowerCase()) {
            case "google" -> (String) userInfo.get("sub");
            case "microsoft" -> (String) userInfo.getOrDefault("sub", userInfo.get("oid"));
            default -> null;
        };
    }

    public String extractFirstName(Map<String, Object> userInfo) {
        String given = (String) userInfo.get("given_name");
        return given != null ? given : "";
    }

    public String extractLastName(Map<String, Object> userInfo) {
        String family = (String) userInfo.get("family_name");
        return family != null ? family : "";
    }

    private OAuth2Properties.ProviderConfig getConfig(String provider) {
        return switch (provider.toLowerCase()) {
            case "google"    -> oauth2Properties.getGoogle();
            case "microsoft" -> oauth2Properties.getMicrosoft();
            default -> throw new IllegalArgumentException("Unknown OAuth2 provider: " + provider);
        };
    }
}
