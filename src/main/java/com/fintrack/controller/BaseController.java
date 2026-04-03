package com.fintrack.controller;

import com.fintrack.application.user.UserService;
import com.fintrack.domain.user.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * Base class for controllers that need to resolve the authenticated user.
 * Provides a shared {@link #resolveUser(UserDetails)} method, eliminating
 * the repetitive Optional + throw pattern across multiple controllers.
 */
public abstract class BaseController {

    @Autowired
    private UserService userService;

    protected User resolveUser(final UserDetails userDetails) {
        return userService.getCurrentUser(userDetails.getUsername());
    }

    protected User resolveUser(final Authentication authentication) {
        return userService.getCurrentUser(authentication.getName());
    }
}
