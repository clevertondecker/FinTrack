package com.fintrack.infrastructure.security;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.fintrack.domain.user.Email;
import com.fintrack.domain.user.Role;
import com.fintrack.domain.user.User;
import com.fintrack.infrastructure.user.UserRepository;

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
    User user = repo.findByEmail(Email.of(email))
      .orElseThrow(() -> new UsernameNotFoundException("User not found."));

    return org.springframework.security.core.userdetails.User
      .withUsername(user.getEmail().getEmail())
      .password(user.getPassword())
      .authorities(
        user.getRoles().stream()
          .map(Role::name)
          .toArray(String[]::new))
      .build();
  }
}