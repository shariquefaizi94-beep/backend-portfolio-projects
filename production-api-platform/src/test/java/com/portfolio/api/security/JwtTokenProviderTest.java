package com.portfolio.api.security;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("JWT Token Provider Tests")
class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider();
        ReflectionTestUtils.setField(jwtTokenProvider, "jwtSecret", 
            "mySecretKeyForJWTTokenGenerationThatIsAtLeast256BitsLong");
        ReflectionTestUtils.setField(jwtTokenProvider, "expirationHours", 24);
        jwtTokenProvider.init();
    }

    @Test
    @DisplayName("Should generate valid token")
    void shouldGenerateValidToken() {
        String token = jwtTokenProvider.generateToken("testuser", Set.of("USER", "ADMIN"));

        assertNotNull(token);
        assertFalse(token.isEmpty());
        assertTrue(token.contains(".")); // JWT format
    }

    @Test
    @DisplayName("Should extract username from token")
    void shouldExtractUsernameFromToken() {
        String token = jwtTokenProvider.generateToken("testuser", Set.of("USER"));

        String username = jwtTokenProvider.getUsername(token);

        assertEquals("testuser", username);
    }

    @Test
    @DisplayName("Should validate token and return claims")
    void shouldValidateTokenAndReturnClaims() {
        String token = jwtTokenProvider.generateToken("testuser", Set.of("USER", "ADMIN"));

        var claims = jwtTokenProvider.validateToken(token);

        assertNotNull(claims);
        assertEquals("testuser", claims.getSubject());
        assertNotNull(claims.get("roles"));
    }

    @Test
    @DisplayName("Should detect non-expired token")
    void shouldDetectNonExpiredToken() {
        String token = jwtTokenProvider.generateToken("testuser", Set.of("USER"));

        assertFalse(jwtTokenProvider.isTokenExpired(token));
    }

    @Test
    @DisplayName("Should get token expiration date")
    void shouldGetTokenExpirationDate() {
        String token = jwtTokenProvider.generateToken("testuser", Set.of("USER"));

        var expiration = jwtTokenProvider.getExpiration(token);

        assertNotNull(expiration);
        assertTrue(expiration.after(new java.util.Date()));
    }

    @Test
    @DisplayName("Should generate token with custom claims")
    void shouldGenerateTokenWithCustomClaims() {
        Map<String, Object> customClaims = Map.of(
            "tier", "PREMIUM",
            "apiVersion", "v2"
        );

        String token = jwtTokenProvider.generateToken("testuser", Set.of("USER"), customClaims);
        var claims = jwtTokenProvider.validateToken(token);

        assertEquals("PREMIUM", claims.get("tier"));
        assertEquals("v2", claims.get("apiVersion"));
    }

    @Test
    @DisplayName("Should throw exception for malformed token")
    void shouldThrowExceptionForMalformedToken() {
        String malformedToken = "not.a.valid.jwt.token";

        assertThrows(MalformedJwtException.class, () ->
            jwtTokenProvider.validateToken(malformedToken));
    }

    @Test
    @DisplayName("Should throw exception for empty token")
    void shouldThrowExceptionForEmptyToken() {
        assertThrows(IllegalArgumentException.class, () ->
            jwtTokenProvider.validateToken(""));
    }

    @Test
    @DisplayName("Should detect expired token")
    void shouldDetectExpiredToken() {
        // Create a provider with very short expiration
        JwtTokenProvider shortExpiry = new JwtTokenProvider();
        ReflectionTestUtils.setField(shortExpiry, "jwtSecret", 
            "mySecretKeyForJWTTokenGenerationThatIsAtLeast256BitsLong");
        ReflectionTestUtils.setField(shortExpiry, "expirationHours", 0); // 0 hours = expired immediately
        shortExpiry.init();

        String token = shortExpiry.generateToken("testuser", Set.of("USER"));

        // The token should be expired or about to expire
        assertThrows(ExpiredJwtException.class, () -> {
            // Sleep briefly to ensure token expires
            Thread.sleep(100);
            shortExpiry.validateToken(token);
        });
    }
}
