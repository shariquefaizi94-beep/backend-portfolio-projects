package com.portfolio.cloudnative.user.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("User Domain Tests")
class UserTest {
    
    @Test
    @DisplayName("Should create user with valid inputs")
    void shouldCreateUser() {
        User user = User.create("johndoe", "john@example.com", "John", "Doe");
        
        assertNotNull(user.id());
        assertEquals("johndoe", user.username());
        assertEquals("john@example.com", user.email());
        assertEquals("John", user.firstName());
        assertEquals("Doe", user.lastName());
        assertEquals(UserStatus.ACTIVE, user.status());
    }
    
    @Test
    @DisplayName("Should throw exception for null username")
    void shouldThrowForNullUsername() {
        assertThrows(IllegalArgumentException.class, () ->
            User.create(null, "email@test.com", "First", "Last"));
    }
    
    @Test
    @DisplayName("Should throw exception for blank username")
    void shouldThrowForBlankUsername() {
        assertThrows(IllegalArgumentException.class, () ->
            User.create("  ", "email@test.com", "First", "Last"));
    }
    
    @Test
    @DisplayName("Should throw exception for invalid email")
    void shouldThrowForInvalidEmail() {
        assertThrows(IllegalArgumentException.class, () ->
            User.create("user", "invalid-email", "First", "Last"));
    }
    
    @Test
    @DisplayName("Should update status immutably")
    void shouldUpdateStatus() {
        User original = User.create("johndoe", "john@example.com", "John", "Doe");
        
        User updated = original.withStatus(UserStatus.SUSPENDED);
        
        assertEquals(UserStatus.ACTIVE, original.status());
        assertEquals(UserStatus.SUSPENDED, updated.status());
        assertNotSame(original, updated);
    }
    
    @Test
    @DisplayName("Should return full name correctly")
    void shouldReturnFullName() {
        User user = User.create("johndoe", "john@example.com", "John", "Doe");
        assertEquals("John Doe", user.fullName());
    }
    
    @Test
    @DisplayName("Should return username when names are null")
    void shouldReturnUsernameWhenNamesNull() {
        User user = User.create("johndoe", "john@example.com", null, null);
        assertEquals("johndoe", user.fullName());
    }
}
