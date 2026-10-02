package com.portfolio.api.graphql;

import com.portfolio.api.domain.model.RateLimitTier;
import com.portfolio.api.domain.model.Role;
import com.portfolio.api.domain.model.User;
import com.portfolio.api.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * GraphQL resolver for User operations.
 */
@Controller
public class UserResolver {
    
    private static final Logger log = LoggerFactory.getLogger(UserResolver.class);
    
    private final UserService userService;
    
    public UserResolver(UserService userService) {
        this.userService = userService;
    }
    
    // ============================================
    // Queries
    // ============================================
    
    @QueryMapping
    public Optional<User> user(@Argument UUID id) {
        log.debug("GraphQL query: user(id={})", id);
        return userService.findById(id);
    }
    
    @QueryMapping
    public Optional<User> userByUsername(@Argument String username) {
        log.debug("GraphQL query: userByUsername(username={})", username);
        return userService.findByUsername(username);
    }
    
    @QueryMapping
    public Optional<User> userByEmail(@Argument String email) {
        log.debug("GraphQL query: userByEmail(email={})", email);
        return userService.findByEmail(email);
    }
    
    // ============================================
    // Mutations
    // ============================================
    
    @MutationMapping
    public User createUser(@Argument CreateUserInput input) {
        log.info("GraphQL mutation: createUser(username={})", input.username());
        
        Set<Role> roles = input.roles() != null && !input.roles().isEmpty() 
            ? Set.copyOf(input.roles()) 
            : Set.of(Role.USER);
        
        RateLimitTier tier = input.rateLimitTier() != null 
            ? input.rateLimitTier() 
            : RateLimitTier.FREE;
        
        return userService.create(
            input.username(),
            input.email(),
            hashPassword(input.password()),
            roles,
            tier
        );
    }
    
    @MutationMapping
    public boolean deleteUser(@Argument UUID id) {
        log.info("GraphQL mutation: deleteUser(id={})", id);
        userService.delete(id);
        return true;
    }
    
    // ============================================
    // Helper methods
    // ============================================
    
    private String hashPassword(String password) {
        // In production, use BCrypt or similar
        // This is a simple hash for demonstration
        return "hashed_" + password.hashCode();
    }
    
    // ============================================
    // DTOs
    // ============================================
    
    public record CreateUserInput(
        String username,
        String email,
        String password,
        Set<Role> roles,
        RateLimitTier rateLimitTier
    ) {}
}
