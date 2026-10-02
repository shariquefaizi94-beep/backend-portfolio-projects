package com.portfolio.api.repository;

import com.portfolio.api.domain.model.User;
import com.portfolio.api.domain.model.UserStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for User persistence operations.
 */
public interface UserRepository {
    
    Optional<User> findById(UUID id);
    
    Optional<User> findByUsername(String username);
    
    Optional<User> findByEmail(String email);
    
    List<User> findByStatus(UserStatus status);
    
    User save(User user);
    
    void deleteById(UUID id);
    
    boolean existsById(UUID id);
    
    boolean existsByUsername(String username);
    
    boolean existsByEmail(String email);
    
    long count();
}
