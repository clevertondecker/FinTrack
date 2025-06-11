package com.fintrack.controller.auth;

import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fintrack.controller.auth.dtos.LoginRequest;
import com.fintrack.controller.auth.dtos.LoginResponse;
import com.fintrack.infrastructure.security.JwtUtil;

import jakarta.validation.Valid;

/** Controller for handling authentication requests.
 * Provides endpoints for user login and token generation.
 */
@RestController
@RequestMapping("/api/auth")
@Validated
public class AuthController {

  private static final Logger logger =
    LoggerFactory.getLogger(AuthController.class);

  /** Authentication manager for processing login requests.
   * Used to authenticate users based on email and password. */
  private final AuthenticationManager authManager;

  /** JWT utility service for generating and validating JWT tokens.
   * Used to create tokens upon successful login. */
  private final JwtUtil jwtUtil;

  public AuthController(final AuthenticationManager theAuthManager,
                        final JwtUtil theJwtUtil) {
    Validate.notNull(theAuthManager, "The authManager cannot be null.");
    Validate.notNull(theJwtUtil, "The jwtUtil cannot be null.");

    authManager = theAuthManager;
    jwtUtil = theJwtUtil;
  }

  /** Endpoint for user login.
   * Authenticates the user using email and password, and returns a JWT token.
   *
   * @param request the login request containing email and password. Cannot be null.
   *
   * @return ResponseEntity containing the login response with JWT token. Never null.
   *
   * @throws BadCredentialsException if authentication fails due to invalid credentials.
   */
  @PostMapping("/login")
  public ResponseEntity<LoginResponse> login(@Valid @RequestBody final LoginRequest request) {
    try {
      authManager.authenticate(new UsernamePasswordAuthenticationToken
        (request.email(), request.password()));

      String token = jwtUtil.generateToken(request.email());
      return ResponseEntity.ok(new LoginResponse(token, "Bearer"));

    } catch (BadCredentialsException e) {
      throw e;
    } catch (Exception e) {
      logger.error("Error during login: ", e);
      throw e;
    }
  }
}
