package com.fintrack.domain.user;

import org.springframework.security.core.GrantedAuthority;

/**
 * Enumeration representing user roles in the system.
 * Defines the different levels of access and permissions.
 */
public enum Role implements GrantedAuthority {
  /**
   * Standard user role with basic access.
   */
  USER,
  
  /**
   * Administrator role with full system access.
   */
  ADMIN;

  @Override
  public String getAuthority() {
    return "ROLE_" + name();
  }
}
