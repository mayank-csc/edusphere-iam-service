package com.edusphere.iam.controller;

import com.edusphere.iam.config.JwtProperties;
import com.edusphere.iam.dto.AuthResponse;
import com.edusphere.iam.dto.LoginRequest;
import com.edusphere.iam.service.AuthService;
import com.edusphere.iam.service.OAuth2Service;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final OAuth2Service oauth2Service;
    private final JwtProperties jwtProperties;

    // Step 1: React calls this to get the Google/Microsoft login URL
    @GetMapping("/oauth2/{provider}/authorize")
    public ResponseEntity<Map<String, String>> getAuthorizationUrl(@PathVariable String provider) {
        String authUrl = oauth2Service.buildAuthorizationUrl(provider);
        return ResponseEntity.ok(Map.of("authUrl", authUrl));
    }

    // Step 2: Google/Microsoft redirects here after user authenticates
    @GetMapping("/oauth2/callback")
    public void handleOAuth2Callback(@RequestParam String code,
                                     @RequestParam(defaultValue = "google") String state,
                                     HttpServletResponse response) throws IOException {
        try {
            AuthResponse auth = authService.loginWithOAuth2(code, state);
            setAuthCookies(response, auth);
            // Only non-sensitive routing metadata goes in the URL
            String redirectUrl = jwtProperties.getFrontendRedirectUri()
                    + "?role=" + URLEncoder.encode(auth.getRole() != null ? auth.getRole() : "", StandardCharsets.UTF_8)
                    + "&tenantId=" + URLEncoder.encode(auth.getTenantId() != null ? auth.getTenantId() : "", StandardCharsets.UTF_8);
            response.sendRedirect(redirectUrl);
        } catch (Exception e) {
            log.error("OAuth2 callback failed provider={}: {}", state, e.getMessage());
            String errorUrl = jwtProperties.getFrontendRedirectUri()
                    + "?error=" + URLEncoder.encode(e.getMessage(), StandardCharsets.UTF_8);
            response.sendRedirect(errorUrl);
        }
    }

    // Normal email + password login
    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> login(@Valid @RequestBody LoginRequest request,
                                                      HttpServletResponse response) {
        try {
            AuthResponse auth = authService.login(request);
            setAuthCookies(response, auth);
            return ResponseEntity.ok(Map.of(
                    "role", auth.getRole() != null ? auth.getRole() : "",
                    "tenantId", auth.getTenantId() != null ? auth.getTenantId() : ""
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(401).build();
        } catch (IllegalStateException e) {
            return ResponseEntity.status(403).build();
        }
    }

    // Exchange refresh token cookie for new access token cookie
    @PostMapping("/refresh")
    public ResponseEntity<Map<String, String>> refresh(HttpServletResponse response,
                                                        @CookieValue(name = "refreshToken", required = false) String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank())
            return ResponseEntity.badRequest().build();
        try {
            AuthResponse auth = authService.refreshToken(refreshToken);
            setAuthCookies(response, auth);
            return ResponseEntity.ok(Map.of(
                    "role", auth.getRole() != null ? auth.getRole() : "",
                    "tenantId", auth.getTenantId() != null ? auth.getTenantId() : ""
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(401).build();
        }
    }

    // Clear auth cookies (browser calls this on sign-out)
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletResponse response) {
        clearCookie(response, "accessToken");
        clearCookie(response, "refreshToken");
        return ResponseEntity.noContent().build();
    }

    private void setAuthCookies(HttpServletResponse response, AuthResponse auth) {
        addHttpOnlyCookie(response, "accessToken", auth.getAccessToken(),
                (int) (jwtProperties.getExpiration() / 1000));
        addHttpOnlyCookie(response, "refreshToken", auth.getRefreshToken(),
                (int) (jwtProperties.getRefreshExpiration() / 1000));
    }

    private void addHttpOnlyCookie(HttpServletResponse response, String name, String value, int maxAgeSeconds) {
        Cookie cookie = new Cookie(name, value);
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(maxAgeSeconds);
        // cookie.setSecure(true); // enable when serving over HTTPS
        response.addCookie(cookie);
    }

    private void clearCookie(HttpServletResponse response, String name) {
        Cookie cookie = new Cookie(name, "");
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(0);
        response.addCookie(cookie);
    }
}
