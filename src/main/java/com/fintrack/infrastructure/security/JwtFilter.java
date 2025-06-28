package com.fintrack.infrastructure.security;

import java.io.IOException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * JWT authentication filter that processes JWT tokens in HTTP requests.
 * Extracts and validates JWT tokens from the Authorization header.
 * Uses email as the principal for authentication.
 */
public class JwtFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(JwtFilter.class);
    private static final String BEARER_PREFIX = "Bearer ";
    private static final int BEARER_PREFIX_LENGTH = BEARER_PREFIX.length();

    private final JwtUtil jwtUtil;
    private final UserDetailsService userDetailsService;

    public JwtFilter(JwtUtil jwtUtil, UserDetailsService userDetailsService) {
        this.jwtUtil = jwtUtil;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                  HttpServletResponse response,
                                  FilterChain filterChain)
            throws ServletException, IOException {
        try {
            String authorizationHeader = request.getHeader("Authorization");
            
            if (isValidAuthorizationHeader(authorizationHeader)) {
                String token = extractToken(authorizationHeader);
                
                if (jwtUtil.validate(token)) {
                    String email = jwtUtil.extractUsername(token);
                    UserDetails userDetails = userDetailsService.loadUserByUsername(email);

                    UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            }
        } catch (Exception e) {
            logger.error("Error processing JWT token", e);
            // Continue with the filter chain even if JWT processing fails
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Checks if the authorization header is valid and contains a Bearer token.
     *
     * @param authorizationHeader the authorization header to validate
     * @return true if the header is valid, false otherwise
     */
    private boolean isValidAuthorizationHeader(String authorizationHeader) {
        return authorizationHeader != null && authorizationHeader.startsWith(BEARER_PREFIX);
    }

    /**
     * Extracts the token from the authorization header by removing the Bearer prefix.
     *
     * @param authorizationHeader the authorization header
     * @return the extracted token
     */
    private String extractToken(String authorizationHeader) {
        return authorizationHeader.substring(BEARER_PREFIX_LENGTH);
    }
}
