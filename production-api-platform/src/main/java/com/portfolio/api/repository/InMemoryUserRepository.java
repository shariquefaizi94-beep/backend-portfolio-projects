package com.portfolio.api.repository;

import com.portfolio.api.domain.model.RateLimitTier;
import com.portfolio.api.domain.model.Role;
import com.portfolio.api.domain.model.User;
import com.portfolio.api.domain.model.UserStatus;
import org.springframework.stereotype.Repository;

import jakarta.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * In-memory implementation of UserRepository.
 * Provides thread-safe operations with sample data for demonstration.
 */
@Repository
public class InMemoryUserRepository implements UserRepository {
    
    private final Map<UUID, User> users = new ConcurrentHashMap<>();
    
    @PostConstruct
    public void initializeSampleData() {
        // Admin user
        saveUser("admin", "admin@portfolio.com", "hashedPassword123", 
                Set.of(Role.ADMIN, Role.USER), RateLimitTier.UNLIMITED);
        
        // Premium users
        saveUser("premium_user", "premium@example.com", "hashedPassword456", 
                Set.of(Role.PREMIUM, Role.USER), RateLimitTier.PREMIUM);
        
        // Basic users
        saveUser("basic_user", "basic@example.com", "hashedPassword789", 
                Set.of(Role.USER), RateLimitTier.BASIC);
        
        // Free tier users
        saveUser("free_user", "free@example.com", "hashedPasswordABC", 
                Set.of(Role.USER), RateLimitTier.FREE);
        
        // API client
        saveUser("api_service", "api@portfolio.com", "hashedServiceKey", 
                Set.of(Role.API_CLIENT, Role.SERVICE_ACCOUNT), RateLimitTier.ENTERPRISE);
    }
    
    private void saveUser(String username, String email, String passwordHash, 
                           Set<Role> roles, RateLimitTier tier) {
        User user = User.create(username, email, passwordHash, roles, tier);
        users.put(user.id(), user);
    }
    
    @Override
    public Optional<User> findById(UUID id) {
        return Optional.ofNullable(users.get(id));
    }
    
    @Override
    public Optional<User> findByUsername(String username) {
        return users.values().stream()
            .filter(u -> u.username().equalsIgnoreCase(username))
            .findFirst();
    }
    
    @Override
    public Optional<User> findByEmail(String email) {
        return users.values().stream()
            .filter(u -> u.email().equalsIgnoreCase(email))
            .findFirst();
    }
    
    @Override
    public List<User> findByStatus(UserStatus status) {
        return users.values().stream()
            .filter(u -> u.status() == status)
            .collect(Collectors.toList());
    }
    
    @Override
    public User save(User user) {
        users.put(user.id(), user);
        return user;
    }
    
    @Override
    public void deleteById(UUID id) {
        users.remove(id);
    }
    
    @Override
    public boolean existsById(UUID id) {
        return users.containsKey(id);
    }
    
    @Override
    public boolean existsByUsername(String username) {
        return users.values().stream()
            .anyMatch(u -> u.username().equalsIgnoreCase(username));
    }
    
    @Override
    public boolean existsByEmail(String email) {
        return users.values().stream()
            .anyMatch(u -> u.email().equalsIgnoreCase(email));
    }
    
    @Override
    public long count() {
        return users.size();
    }
}
