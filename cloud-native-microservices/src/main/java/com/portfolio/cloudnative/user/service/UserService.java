package com.portfolio.cloudnative.user.service;

import com.portfolio.cloudnative.user.model.User;
import com.portfolio.cloudnative.user.model.UserStatus;
import com.portfolio.cloudnative.user.repository.UserRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.micrometer.core.annotation.Timed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * User service with circuit breaker and retry patterns.
 */
@Service
public class UserService {
    
    private static final Logger log = LoggerFactory.getLogger(UserService.class);
    
    private final UserRepository userRepository;
    
    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }
    
    @Timed(value = "user.service.findById", description = "Time to find user by ID")
    @CircuitBreaker(name = "userService", fallbackMethod = "findByIdFallback")
    @Retry(name = "userService")
    public Optional<User> findById(UUID id) {
        log.debug("Finding user by ID: {}", id);
        return userRepository.findById(id);
    }
    
    public Optional<User> findByIdFallback(UUID id, Exception e) {
        log.warn("Circuit breaker fallback for findById: {}", e.getMessage());
        return Optional.empty();
    }
    
    @Timed(value = "user.service.findByUsername")
    @CircuitBreaker(name = "userService", fallbackMethod = "findByUsernameFallback")
    public Optional<User> findByUsername(String username) {
        log.debug("Finding user by username: {}", username);
        return userRepository.findByUsername(username);
    }
    
    public Optional<User> findByUsernameFallback(String username, Exception e) {
        log.warn("Circuit breaker fallback for findByUsername: {}", e.getMessage());
        return Optional.empty();
    }
    
    @Timed(value = "user.service.findAll")
    public List<User> findAll() {
        return userRepository.findAll();
    }
    
    @Timed(value = "user.service.findByStatus")
    public List<User> findByStatus(UserStatus status) {
        return userRepository.findByStatus(status);
    }
    
    @Timed(value = "user.service.create")
    public User create(String username, String email, String firstName, String lastName) {
        log.info("Creating user: {}", username);
        
        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("Username already exists: " + username);
        }
        
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email already exists: " + email);
        }
        
        User user = User.create(username, email, firstName, lastName);
        return userRepository.save(user);
    }
    
    @Timed(value = "user.service.updateStatus")
    public Optional<User> updateStatus(UUID id, UserStatus status) {
        log.info("Updating user {} status to: {}", id, status);
        return userRepository.findById(id)
            .map(user -> user.withStatus(status))
            .map(userRepository::save);
    }
    
    public void delete(UUID id) {
        log.info("Deleting user: {}", id);
        userRepository.deleteById(id);
    }
    
    public long count() {
        return userRepository.count();
    }
}
