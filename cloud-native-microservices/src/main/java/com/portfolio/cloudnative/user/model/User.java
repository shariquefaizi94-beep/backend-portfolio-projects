package com.portfolio.cloudnative.user.model;

import java.time.Instant;
import java.util.UUID;

/**
 * User domain model for the user microservice.
 */
public record User(
    UUID id,
    String username,
    String email,
    String firstName,
    String lastName,
    UserStatus status,
    Instant createdAt,
    Instant updatedAt
) {
    
    public User {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Username cannot be null or blank");
        }
        if (email == null || !email.contains("@")) {
            throw new IllegalArgumentException("Invalid email address");
        }
    }
    
    public static User create(String username, String email, String firstName, String lastName) {
        Instant now = Instant.now();
        return new User(
            UUID.randomUUID(),
            username,
            email,
            firstName,
            lastName,
            UserStatus.ACTIVE,
            now,
            now
        );
    }
    
    public User withStatus(UserStatus newStatus) {
        return new User(id, username, email, firstName, lastName, newStatus, createdAt, Instant.now());
    }
    
    public String fullName() {
        if (firstName == null && lastName == null) return username;
        if (firstName == null) return lastName;
        if (lastName == null) return firstName;
        return firstName + " " + lastName;
    }
}
