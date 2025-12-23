package com.fintrack.infrastructure.security;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Service responsible for password encoding and validation operations.
 * Uses BCrypt for secure password hashing.
 */
@Service
public class PasswordService {

    /** The password encoder. */
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    /**
     * Encodes a raw password using BCrypt.
     *
     * @param rawPassword the raw password to encode
     * @return the encoded password
     */
    public String encodePassword(String rawPassword) {
        return passwordEncoder.encode(rawPassword);
    }

    /**
     * Validates a raw password against an encoded password.
     *
     * @param rawPassword the raw password to validate.
     * This is the password input by the user.
     *
     * @param encodedPassword the encoded password to validate against.
     *
     * @return true if the passwords match, false otherwise.
     */
    public boolean matches(String rawPassword, String encodedPassword) {
        if (rawPassword == null || rawPassword.isEmpty()) {
            return false;
        }
        return passwordEncoder.matches(rawPassword, encodedPassword);
    }
}