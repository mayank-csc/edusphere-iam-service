package com.edusphere.iam.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.oauth2")
@Data
public class OAuth2Properties {

    private ProviderConfig google = new ProviderConfig();
    private ProviderConfig microsoft = new ProviderConfig();

    @Data
    public static class ProviderConfig {
        private String clientId = "";
        private String clientSecret = "";
        private String redirectUri = "";
        private String tokenUri = "";
        private String userinfoUri = "";
        private String authUri = "";
        private String scope = "openid email profile";
    }
}
