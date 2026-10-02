package com.portfolio.cloudnative.user.repository;

import com.portfolio.cloudnative.user.model.User;
import com.portfolio.cloudnative.user.model.UserStatus;
import org.springframework.stereotype.Repository;

import jakarta.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * In-memory repository for users with sample data.
 */
@Repository
public class UserRepository {
    
    private final Map<UUID, User> users = new ConcurrentHashMap<>();
    
    @PostConstruct
    public void initializeSampleData() {
        saveUser("john.doe", "john.doe@example.com", "John", "Doe");
        saveUser("jane.smith", "jane.smith@example.com", "Jane", "Smith");
        saveUser("admin", "admin@example.com", "Admin", "User");
        saveUser("service-account", "service@internal.com", "Service", "Account");
    }
    
    private void saveUser(String username, String email, String firstName, String lastName) {
        User user = User.create(username, email, firstName, lastName);
        users.put(user.id(), user);
    }
    
    public Optional<User> findById(UUID id) {
        return Optional.ofNullable(users.get(id));
    }
    
    public Optional<User> findByUsername(String username) {
        return users.values().stream()
            .filter(u -> u.username().equalsIgnoreCase(username))
            .findFirst();
    }
    
    public Optional<User> findByEmail(String email) {
        return users.values().stream()
            .filter(u -> u.email().equalsIgnoreCase(email))
            .findFirst();
    }
    
    public List<User> findAll() {
        return new ArrayList<>(users.values());
    }
    
    public List<User> findByStatus(UserStatus status) {
        return users.values().stream()
            .filter(u -> u.status() == status)
            .collect(Collectors.toList());
    }
    
    public User save(User user) {
        users.put(user.id(), user);
        return user;
    }
    
    public void deleteById(UUID id) {
        users.remove(id);
    }
    
    public boolean existsByUsername(String username) {
        return users.values().stream()
            .anyMatch(u -> u.username().equalsIgnoreCase(username));
    }
    
    public boolean existsByEmail(String email) {
        return users.values().stream()
            .anyMatch(u -> u.email().equalsIgnoreCase(email));
    }
    
    public long count() {
        return users.size();
    }
}
