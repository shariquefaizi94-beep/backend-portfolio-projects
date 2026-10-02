package com.portfolio.api.service;

import com.portfolio.api.domain.model.RateLimitTier;
import com.portfolio.api.domain.model.Role;
import com.portfolio.api.domain.model.User;
import com.portfolio.api.domain.model.UserStatus;
import com.portfolio.api.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Service layer for User operations.
 */
@Service
public class UserService {
    
    private static final Logger log = LoggerFactory.getLogger(UserService.class);
    
    private final UserRepository userRepository;
    
    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }
    
    @Cacheable(value = "users", key = "#id")
    public Optional<User> findById(UUID id) {
        log.debug("Finding user by id: {}", id);
        return userRepository.findById(id);
    }
    
    @Cacheable(value = "usersByUsername", key = "#username")
    public Optional<User> findByUsername(String username) {
        log.debug("Finding user by username: {}", username);
        return userRepository.findByUsername(username);
    }
    
    public Optional<User> findByEmail(String email) {
        log.debug("Finding user by email: {}", email);
        return userRepository.findByEmail(email);
    }
    
    @CacheEvict(value = {"users", "usersByUsername"}, allEntries = true)
    public User create(String username, String email, String passwordHash, 
                       Set<Role> roles, RateLimitTier tier) {
        log.info("Creating new user: {}", username);
        
        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("Username already exists: " + username);
        }
        
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email already exists: " + email);
        }
        
        User user = User.create(username, email, passwordHash, roles, tier);
        return userRepository.save(user);
    }
    
    @CacheEvict(value = {"users", "usersByUsername"}, allEntries = true)
    public Optional<User> recordLogin(UUID userId) {
        log.info("Recording login for user: {}", userId);
        return userRepository.findById(userId)
            .map(User::withLastLogin)
            .map(userRepository::save);
    }
    
    @CacheEvict(value = {"users", "usersByUsername"}, allEntries = true)
    public void delete(UUID id) {
        log.info("Deleting user: {}", id);
        userRepository.deleteById(id);
    }
    
    public RateLimitTier getRateLimitTier(String username) {
        return findByUsername(username)
            .map(User::rateLimitTier)
            .orElse(RateLimitTier.FREE);
    }
    
    public long count() {
        return userRepository.count();
    }
    
    public boolean isValidCredentials(String username, String passwordHash) {
        return findByUsername(username)
            .filter(u -> u.status() == UserStatus.ACTIVE)
            .filter(u -> u.passwordHash().equals(passwordHash))
            .isPresent();
    }
}
