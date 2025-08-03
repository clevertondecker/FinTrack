package com.fintrack.domain.user;

/**
 * Represents the authentication provider for a user account.
 * 
 * This enum defines the different sources of user authentication,
 * allowing the system to handle both traditional email/password
 * authentication and OAuth2 providers.
 * 
 * Each provider may have different security requirements and
 * authentication flows, which are handled appropriately by the
 * security configuration.
 * 
 * @author FinTrack Team
 * @since 1.0.0
 */
public enum AuthProvider {
    
    /**
     * Local authentication using email and password.
     * Users register directly with the application.
     */
    LOCAL,
    
    /**
     * Google OAuth2 authentication.
     * Users authenticate through Google's OAuth2 service.
     */
    GOOGLE;
    
    /**
     * Checks if this provider is an OAuth2 provider.
     * 
     * @return true if this is an OAuth2 provider, false otherwise
     */
    public boolean isOAuth2() {
        return this == GOOGLE;
    }
    
    /**
     * Checks if this provider requires password validation.
     * 
     * @return true if password validation is required, false otherwise
     */
    public boolean requiresPassword() {
        return this == LOCAL;
    }
} 