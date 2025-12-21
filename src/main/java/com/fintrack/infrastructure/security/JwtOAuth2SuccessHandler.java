package com.fintrack.infrastructure.security;

import com.fintrack.domain.user.AuthProvider;
import com.fintrack.domain.user.Role;
import com.fintrack.domain.user.User;
import com.fintrack.domain.user.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Set;

/**
 * Handles successful OAuth2 authentication by creating/updating user and generating JWT token.
 * Redirects to frontend with the JWT token for seamless authentication flow.
 * 
 * This handler is responsible for:
 * - Extracting user information from OAuth2 authentication
 * - Creating or updating user in the database
 * - Generating JWT token for the authenticated user
 * - Redirecting to frontend with the token
 */
@Component
public class JwtOAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private static final Logger logger = LoggerFactory.getLogger(JwtOAuth2SuccessHandler.class);
    private static final String EMAIL_ATTRIBUTE = "email";
    private static final String NAME_ATTRIBUTE = "name";

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;
    private final String redirectUri;

    public JwtOAuth2SuccessHandler(JwtUtil jwtUtil,
                                   UserRepository userRepository,
                                   @Value("${app.oauth2.redirect-uri}") String redirectUri) {
        this.jwtUtil = jwtUtil;
        this.userRepository = userRepository;
        this.redirectUri = redirectUri;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        try {
            OAuth2User oauthUser = extractOAuth2User(authentication);
            OAuth2UserInfo userInfo = extractUserInfo(oauthUser);
            
            logger.info("OAuth2 authentication successful for user: {}", userInfo.email());

            findOrCreateOAuthUser(userInfo.name(), userInfo.email());
            String jwt = jwtUtil.generateToken(userInfo.email());

            redirectToFrontend(response, jwt);

        } catch (Exception e) {
            logger.error("Error processing OAuth2 authentication success", e);
            handleAuthenticationError(response);
        }
    }

    /**
     * Extracts OAuth2User from an authentication object.
     *
     * @param authentication the authentication object
     * @return OAuth2User object
     * @throws ClassCastException if authentication is not OAuth2AuthenticationToken
     */
    private OAuth2User extractOAuth2User(Authentication authentication) {
        if (!(authentication instanceof OAuth2AuthenticationToken)) {
            throw new IllegalArgumentException("Authentication must be OAuth2AuthenticationToken");
        }
        return ((OAuth2AuthenticationToken) authentication).getPrincipal();
    }

    /**
     * Extracts user information from OAuth2User.
     *
     * @param oauthUser the OAuth2User object
     * @return OAuth2UserInfo containing email and name
     */
    private OAuth2UserInfo extractUserInfo(OAuth2User oauthUser) {
        String email = oauthUser.getAttribute(EMAIL_ATTRIBUTE);
        String name = oauthUser.getAttribute(NAME_ATTRIBUTE);
        
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("Email is required from OAuth2 provider");
        }
        
        return new OAuth2UserInfo(email, name != null ? name : email);
    }

    /**
     * Redirects to frontend with JWT token.
     *
     * @param response the HTTP response
     * @param jwt the JWT token
     * @throws IOException if redirect fails
     */
    private void redirectToFrontend(HttpServletResponse response, String jwt) throws IOException {
        String redirectUrl = redirectUri + "?token=" + jwt;
        logger.info("Redirecting to: {}", redirectUrl);
        response.sendRedirect(redirectUrl);
    }

    /**
     * Handles authentication error by sending error response.
     *
     * @param response the HTTP response
     * @throws IOException if error response fails
     */
    private void handleAuthenticationError(HttpServletResponse response) throws IOException {
        response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Authentication failed");
    }

    /**
     * Finds an existing user or creates new one for OAuth2 authentication.
     *
     * @param name  the user's name from OAuth2 provider
     * @param email the user's email from OAuth2 provider
     */
    private void findOrCreateOAuthUser(String name, String email) {
        userRepository.findByEmail(com.fintrack.domain.user.Email.of(email))
            .orElseGet(() -> {
                logger.info("Creating new OAuth2 user: {}", email);
                User newUser = User.createOAuth2User(name, email, Set.of(Role.USER), AuthProvider.GOOGLE);
                return userRepository.save(newUser);
            });
    }
} 