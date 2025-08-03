package com.fintrack.infrastructure.security;

/**
 * Encapsulates user information received from OAuth2 provider.
 * 
 * This record contains the essential user information that is
 * extracted from the OAuth2 authentication response.
 * 
 * @param email the user's email address (required)
 * @param name the user's display name (optional, falls back to email)
 */
public record OAuth2UserInfo(String email, String name) {
    
    /**
     * Creates a new OAuth2UserInfo with validation.
     *
     * @param email the user's email address
     * @param name the user's display name
     * @throws IllegalArgumentException if email is null or empty
     */
    public OAuth2UserInfo {
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("Email cannot be null or empty");
        }
        
        // Use email as fallback if name is null or empty
        if (name == null || name.trim().isEmpty()) {
            name = email;
        }
    }
} 