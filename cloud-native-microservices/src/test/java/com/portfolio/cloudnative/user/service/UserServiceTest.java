package com.portfolio.cloudnative.user.service;

import com.portfolio.cloudnative.user.model.User;
import com.portfolio.cloudnative.user.model.UserStatus;
import com.portfolio.cloudnative.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("User Service Tests")
class UserServiceTest {
    
    @Mock
    private UserRepository userRepository;
    
    private UserService userService;
    
    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository);
    }
    
    @Test
    @DisplayName("Should find user by ID")
    void shouldFindUserById() {
        UUID userId = UUID.randomUUID();
        User user = User.create("johndoe", "john@example.com", "John", "Doe");
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        
        Optional<User> result = userService.findById(userId);
        
        assertTrue(result.isPresent());
        assertEquals("johndoe", result.get().username());
        verify(userRepository).findById(userId);
    }
    
    @Test
    @DisplayName("Should find user by username")
    void shouldFindUserByUsername() {
        User user = User.create("johndoe", "john@example.com", "John", "Doe");
        when(userRepository.findByUsername("johndoe")).thenReturn(Optional.of(user));
        
        Optional<User> result = userService.findByUsername("johndoe");
        
        assertTrue(result.isPresent());
        assertEquals("john@example.com", result.get().email());
    }
    
    @Test
    @DisplayName("Should create user successfully")
    void shouldCreateUser() {
        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        
        User result = userService.create("newuser", "new@example.com", "New", "User");
        
        assertNotNull(result);
        assertEquals("newuser", result.username());
        assertEquals(UserStatus.ACTIVE, result.status());
        verify(userRepository).save(any(User.class));
    }
    
    @Test
    @DisplayName("Should throw exception when username exists")
    void shouldThrowWhenUsernameExists() {
        when(userRepository.existsByUsername("existing")).thenReturn(true);
        
        assertThrows(IllegalArgumentException.class, () ->
            userService.create("existing", "email@test.com", "First", "Last"));
    }
    
    @Test
    @DisplayName("Should throw exception when email exists")
    void shouldThrowWhenEmailExists() {
        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("existing@test.com")).thenReturn(true);
        
        assertThrows(IllegalArgumentException.class, () ->
            userService.create("newuser", "existing@test.com", "First", "Last"));
    }
    
    @Test
    @DisplayName("Should update user status")
    void shouldUpdateUserStatus() {
        UUID userId = UUID.randomUUID();
        User user = User.create("johndoe", "john@example.com", "John", "Doe");
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        
        Optional<User> result = userService.updateStatus(userId, UserStatus.SUSPENDED);
        
        assertTrue(result.isPresent());
        assertEquals(UserStatus.SUSPENDED, result.get().status());
    }
    
    @Test
    @DisplayName("Should delete user")
    void shouldDeleteUser() {
        UUID userId = UUID.randomUUID();
        doNothing().when(userRepository).deleteById(userId);
        
        userService.delete(userId);
        
        verify(userRepository).deleteById(userId);
    }
}
