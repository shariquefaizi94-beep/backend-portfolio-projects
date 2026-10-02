package com.portfolio.api.domain.model;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

/**
 * User domain model for authentication and authorization.
 */
public record User(
    UUID id,
    String username,
    String email,
    String passwordHash,
    Set<Role> roles,
    UserStatus status,
    RateLimitTier rateLimitTier,
    Instant createdAt,
    Instant lastLoginAt
) {
    
    public User {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Username cannot be null or blank");
        }
        if (email == null || !email.contains("@")) {
            throw new IllegalArgumentException("Invalid email address");
        }
        if (roles == null || roles.isEmpty()) {
            throw new IllegalArgumentException("User must have at least one role");
        }
    }
    
    public static User create(String username, String email, String passwordHash, 
                               Set<Role> roles, RateLimitTier tier) {
        return new User(
            UUID.randomUUID(),
            username,
            email,
            passwordHash,
            roles,
            UserStatus.ACTIVE,
            tier,
            Instant.now(),
            null
        );
    }
    
    public User withLastLogin() {
        return new User(
            this.id,
            this.username,
            this.email,
            this.passwordHash,
            this.roles,
            this.status,
            this.rateLimitTier,
            this.createdAt,
            Instant.now()
        );
    }
    
    public boolean hasRole(Role role) {
        return roles.contains(role);
    }
    
    public boolean isAdmin() {
        return hasRole(Role.ADMIN);
    }
    
    public boolean isActive() {
        return status == UserStatus.ACTIVE;
    }
}
