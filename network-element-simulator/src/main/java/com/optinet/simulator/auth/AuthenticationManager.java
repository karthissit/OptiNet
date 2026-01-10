package com.optinet.simulator.auth;

import java.util.*;

/**
 * Simple authentication manager for the network element simulator.
 * In production, this would integrate with:
 * - RADIUS/TACACS+ for centralized authentication
 * - Local user database with encrypted passwords
 * - Certificate-based authentication (SSH keys)
 * For simulation, we use simple username/password pairs.
 */
public class AuthenticationManager {
    private static final String DEFAULT_USERNAME = "admin";
    private static final String DEFAULT_PASSWORD = "admin";
    
    private final Map<String, String> credentials;
    
    public AuthenticationManager() {
        this.credentials = new HashMap<>();
        initializeDefaultCredentials();
    }
    
    /**
     * Initialize default test credentials
     */
    private void initializeDefaultCredentials() {
        // In production, passwords would be hashed
        credentials.put(DEFAULT_USERNAME, DEFAULT_PASSWORD);
        credentials.put("operator", "operator123");
        credentials.put("monitor", "monitor123");
    }
    
    /**
     * Authenticate username and password
     */
    public boolean authenticate(String username, String password) {
        if (username == null || password == null) {
            return false;
        }
        
        String storedPassword = credentials.get(username);
        if (storedPassword == null) {
            return false;
        }
        
        // Simple comparison (in production, use bcrypt or similar)
        return storedPassword.equals(password);
    }
    
    /**
     * Add a new user (admin operation)
     */
    public void addUser(String username, String password) {
        credentials.put(username, password);
    }
    
    /**
     * Check if user exists
     */
    public boolean userExists(String username) {
        return credentials.containsKey(username);
    }
    
    /**
     * Get role for user (simplified: all authenticated users get same role)
     * In production, this would return USER, ADMIN, MONITOR, etc.
     */
    public String getUserRole(String username) {
        if (!userExists(username)) {
            return "GUEST";
        }
        
        // Simple role mapping
        return switch (username) {
            case "admin" -> "ADMIN";
            case "operator" -> "OPERATOR";
            case "monitor" -> "MONITOR";
            default -> "USER";
        };
    }
}
