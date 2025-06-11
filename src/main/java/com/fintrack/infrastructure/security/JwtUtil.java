package com.fintrack.infrastructure.security;

import java.util.Date;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;

/**
 * Utility class for JWT token operations.
 * Handles token generation, validation, and username extraction.
 */
@Component
public class JwtUtil {

  /** Secret key used for signing JWT tokens. */
  private final SecretKey secret;

  /** Expiration time for JWT tokens in milliseconds. Represents 15 minutes. */
  private final long expirationMs = 900_000;

  /**
   * Default constructor: generates a random secret key.
   */
  public JwtUtil() {
    this.secret = Jwts.SIG.HS512.key().build();
  }

  /**
   * Constructor for injecting a custom secret key (for testing).
   */
  public JwtUtil(SecretKey secret) {
    this.secret = secret;
  }

  /**
   * Generates a JWT token for the given username.
   *
   * @param email the username to include in the token. Cannot be blank.
   *
   * @return the generated JWT token. Never null or blank.
   *
   * @throws IllegalArgumentException if the username is blank.
   */
  public String generateToken(String email) {
    return Jwts.builder()
      .subject(email)
      .issuedAt(new Date())
      .expiration(new Date(System.currentTimeMillis() + expirationMs))
      .signWith(secret)
      .compact();
  }

  /**
   * Extracts the username from a JWT token.
   *
   * @param token the JWT token. Cannot be null or blank.
   *
   * @return the username from the token. Never null or blank.
   */
  public String extractUsername(String token) {
    return extractAllClaims(token).getSubject();
  }

  /**
   * Validates if a JWT token is valid.
   *
   * @param token the JWT token to validate. Cannot be null or blank.
   *
   * @return true if the token is valid, false otherwise.
   */
  public boolean validate(String token) {
    try {
      Jwts.parser()
        .verifyWith(secret)
        .build()
        .parseSignedClaims(token);
      return true;
    } catch (Exception e) {
      return false;
    }
  }

  /**
   * Extracts all claims from a JWT token.
   *
   * @param token the JWT token. Cannot be null or blank.
   *
   * @return the claims from the token. Never null.
   */
  private Claims extractAllClaims(String token) {
    return Jwts.parser()
      .verifyWith(secret)
      .build()
      .parseSignedClaims(token)
      .getPayload();
  }
}