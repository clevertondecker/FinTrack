package com.fintrack.application.user;

import com.fintrack.domain.user.User;
import com.fintrack.domain.user.Email;
import com.fintrack.infrastructure.user.UserRepository;

import org.apache.commons.lang3.Validate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordService passwordService;

    public UserService(final UserRepository theUserRepository,
                       final PasswordService thePasswordService) {
        Validate.notNull(theUserRepository, "The userRepository cannot be null.");
        Validate.notNull(thePasswordService, "The passwordService cannot be null.");

        userRepository = theUserRepository;
        passwordService = thePasswordService ;
    }

    @Transactional
    public User registerUser(String name, String email, String rawPassword) {
        String encodedPassword = passwordService.encodePassword(rawPassword);
        User user = User.of(name, email, encodedPassword);

        return userRepository.save(user);
    }
}