package com.portfolio.api.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("User Domain Model Tests")
class UserTest {

    @Test
    @DisplayName("Should create user with valid inputs")
    void shouldCreateUserWithValidInputs() {
        User user = User.create(
            "testuser",
            "test@example.com",
            "hashedPassword",
            Set.of(Role.USER),
            RateLimitTier.FREE
        );

        assertNotNull(user.id());
        assertEquals("testuser", user.username());
        assertEquals("test@example.com", user.email());
        assertEquals(UserStatus.ACTIVE, user.status());
        assertEquals(RateLimitTier.FREE, user.rateLimitTier());
        assertTrue(user.hasRole(Role.USER));
        assertFalse(user.isAdmin());
    }

    @Test
    @DisplayName("Should throw exception for null username")
    void shouldThrowExceptionForNullUsername() {
        assertThrows(IllegalArgumentException.class, () ->
            User.create(null, "email@test.com", "hash", Set.of(Role.USER), RateLimitTier.FREE));
    }

    @Test
    @DisplayName("Should throw exception for blank username")
    void shouldThrowExceptionForBlankUsername() {
        assertThrows(IllegalArgumentException.class, () ->
            User.create("  ", "email@test.com", "hash", Set.of(Role.USER), RateLimitTier.FREE));
    }

    @Test
    @DisplayName("Should throw exception for invalid email")
    void shouldThrowExceptionForInvalidEmail() {
        assertThrows(IllegalArgumentException.class, () ->
            User.create("user", "invalidemail", "hash", Set.of(Role.USER), RateLimitTier.FREE));
    }

    @Test
    @DisplayName("Should throw exception for empty roles")
    void shouldThrowExceptionForEmptyRoles() {
        assertThrows(IllegalArgumentException.class, () ->
            User.create("user", "email@test.com", "hash", Set.of(), RateLimitTier.FREE));
    }

    @Test
    @DisplayName("Should identify admin user correctly")
    void shouldIdentifyAdminUserCorrectly() {
        User adminUser = User.create(
            "admin",
            "admin@example.com",
            "hashedPassword",
            Set.of(Role.ADMIN, Role.USER),
            RateLimitTier.UNLIMITED
        );

        assertTrue(adminUser.isAdmin());
        assertTrue(adminUser.hasRole(Role.ADMIN));
        assertTrue(adminUser.hasRole(Role.USER));
    }

    @Test
    @DisplayName("Should update last login time")
    void shouldUpdateLastLoginTime() {
        User user = User.create(
            "testuser",
            "test@example.com",
            "hashedPassword",
            Set.of(Role.USER),
            RateLimitTier.FREE
        );

        assertNull(user.lastLoginAt());

        User loggedInUser = user.withLastLogin();

        assertNotNull(loggedInUser.lastLoginAt());
        assertNull(user.lastLoginAt()); // Original unchanged
    }

    @Test
    @DisplayName("Should check active status correctly")
    void shouldCheckActiveStatusCorrectly() {
        User activeUser = User.create(
            "testuser",
            "test@example.com",
            "hashedPassword",
            Set.of(Role.USER),
            RateLimitTier.FREE
        );

        assertTrue(activeUser.isActive());
    }
}
