package com.portfolio.cloudnative.user.controller;

import com.portfolio.cloudnative.user.model.User;
import com.portfolio.cloudnative.user.model.UserStatus;
import com.portfolio.cloudnative.user.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for user operations.
 */
@RestController
@RequestMapping("/api/users")
public class UserController {
    
    private static final Logger log = LoggerFactory.getLogger(UserController.class);
    
    private final UserService userService;
    
    public UserController(UserService userService) {
        this.userService = userService;
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<User> getUser(@PathVariable UUID id) {
        log.debug("Getting user: {}", id);
        return userService.findById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/username/{username}")
    public ResponseEntity<User> getUserByUsername(@PathVariable String username) {
        log.debug("Getting user by username: {}", username);
        return userService.findByUsername(username)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping
    public ResponseEntity<List<User>> getAllUsers() {
        return ResponseEntity.ok(userService.findAll());
    }
    
    @GetMapping("/status/{status}")
    public ResponseEntity<List<User>> getUsersByStatus(@PathVariable UserStatus status) {
        return ResponseEntity.ok(userService.findByStatus(status));
    }
    
    @PostMapping
    public ResponseEntity<User> createUser(@RequestBody CreateUserRequest request) {
        log.info("Creating user: {}", request.username());
        User user = userService.create(
            request.username(),
            request.email(),
            request.firstName(),
            request.lastName()
        );
        return ResponseEntity.ok(user);
    }
    
    @PutMapping("/{id}/status")
    public ResponseEntity<User> updateUserStatus(
            @PathVariable UUID id,
            @RequestParam UserStatus status) {
        log.info("Updating user {} status to: {}", id, status);
        return userService.updateStatus(id, status)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable UUID id) {
        log.info("Deleting user: {}", id);
        userService.delete(id);
        return ResponseEntity.noContent().build();
    }
    
    public record CreateUserRequest(
        String username,
        String email,
        String firstName,
        String lastName
    ) {}
}
