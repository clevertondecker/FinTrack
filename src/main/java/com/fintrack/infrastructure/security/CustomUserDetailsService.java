package com.fintrack.infrastructure.security;

import com.fintrack.domain.user.Email;
import com.fintrack.domain.user.Role;
import com.fintrack.domain.user.User;
import com.fintrack.domain.user.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Custom implementation of UserDetailsService for Spring Security.
 * Loads user details from the database for authentication.
 */
@Service
public class CustomUserDetailsService implements UserDetailsService {

  /** Repository for user data access. Never null. */
  private final UserRepository repo;

  /** Constructor for CustomUserDetailsService.
   *
   * @param theUserRepository the user repository to use for data access. Cannot be null.
   */
  public CustomUserDetailsService(final UserRepository theUserRepository) {
    repo = theUserRepository;
  }

  /**
   * Loads user details by username (email in this case).
   *
   * @param email the email address to search for. Can be used as the username.
   *
   * @return UserDetails object containing user information.
   *
   * @throws UsernameNotFoundException if user is not found.
   */
    @Override
  public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
    User user = findUserByEmail(email);
    String encodedPassword = encodePasswordForUser(user);
    
    return createUserDetails(user, encodedPassword);
  }

  /**
   * Finds a user by email address.
   *
   * @param email the email address to search for. Cannot be blank or null.
   *
   * @return the user entity. Cannot be null.
   *
   * @throws UsernameNotFoundException if user is not found.
   */
  private User findUserByEmail(String email) throws UsernameNotFoundException {
    return repo.findByEmail(Email.of(email))
      .orElseThrow(() ->
              new UsernameNotFoundException("User not found with email: " + email));
  }

  /**
   * Encodes the password based on the user's authentication provider.
   *
   * @param user the user entity. Cannot be null.
   *
   * @return the encoded password string. Never blank.
   */
  private String encodePasswordForUser(User user) {
    if (user.isOAuth2User()) {
      return "{noop}"; // No-op password encoder for OAuth2 users
    }
    return user.getPassword();
  }

  /**
   * Creates UserDetails from user entity and encoded password.
   *
   * @param user the user entity
   *
   * @param encodedPassword the encoded password
   *
   * @return UserDetails object
   */
  private UserDetails createUserDetails(User user, String encodedPassword) {
    String[] authorities = user.getRoles().stream()
      .map(Role::name)
      .toArray(String[]::new);

    return org.springframework.security.core.userdetails.User
      .withUsername(user.getEmail().getEmail())
      .password(encodedPassword)
      .authorities(authorities)
      .build();
  }
}