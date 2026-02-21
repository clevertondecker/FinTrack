package com.fintrack.application.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.fintrack.domain.user.UserRepository;
import com.fintrack.domain.user.UserConnection;
import com.fintrack.infrastructure.persistence.contact.TrustedContactJpaRepository;
import com.fintrack.infrastructure.persistence.creditcard.CreditCardJpaRepository;
import com.fintrack.infrastructure.persistence.user.UserConnectionJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.fintrack.domain.user.Role;
import com.fintrack.domain.user.User;
import com.fintrack.domain.user.Email;
import com.fintrack.infrastructure.security.PasswordService;

/**
 * Unit tests for the UserService application service.
 *
 * These tests verify the business logic, validation, and orchestration
 * of user registration operations following Clean Architecture principles.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("User Service")
public class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordService passwordService;

    @Mock
    private UserConnectionJpaRepository userConnectionRepository;

    @Mock
    private TrustedContactJpaRepository trustedContactRepository;

    @Mock
    private CreditCardJpaRepository creditCardRepository;

    private UserService userService;

    private static final String VALID_NAME = "John Doe";
    private static final String VALID_EMAIL = "john.doe@example.com";
    private static final String VALID_PASSWORD = "securePassword123";
    private static final String ENCODED_PASSWORD = "$2a$10$encodedPasswordHash";
    private static final Set<Role> VALID_ROLES = Set.of(Role.USER);

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository, passwordService, userConnectionRepository,
            trustedContactRepository, creditCardRepository);
    }

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {

        @Test
        @DisplayName("Should create service with valid dependencies")
        void shouldCreateServiceWithValidDependencies() {
            UserService service = new UserService(
                userRepository, passwordService, userConnectionRepository,
                trustedContactRepository, creditCardRepository);

            assertThat(service).isNotNull();
        }

        @Test
        @DisplayName("Should throw exception when userRepository is null")
        void shouldThrowExceptionWhenUserRepositoryIsNull() {
            assertThatThrownBy(() ->
              new UserService(null, passwordService, userConnectionRepository,
                  trustedContactRepository, creditCardRepository))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("The userRepository cannot be null");
        }

        @Test
        @DisplayName("Should throw exception when passwordService is null")
        void shouldThrowExceptionWhenPasswordServiceIsNull() {
            assertThatThrownBy(() ->
              new UserService(userRepository, null, userConnectionRepository,
                  trustedContactRepository, creditCardRepository))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("The passwordService cannot be null");
        }

        @Test
        @DisplayName("Should throw exception when userConnectionRepository is null")
        void shouldThrowExceptionWhenUserConnectionRepositoryIsNull() {
            assertThatThrownBy(() ->
              new UserService(userRepository, passwordService, null,
                  trustedContactRepository, creditCardRepository))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("The userConnectionRepository cannot be null");
        }
    }

    @Nested
    @DisplayName("User Registration Tests")
    class UserRegistrationTests {

        @Test
        @DisplayName("Should register user successfully")
        void shouldRegisterUserSuccessfully() {
            User expectedUser = User.createLocalUser(VALID_NAME, VALID_EMAIL, ENCODED_PASSWORD, VALID_ROLES);
            when(passwordService.encodePassword(VALID_PASSWORD)).thenReturn(ENCODED_PASSWORD);
            when(userRepository.save(any(User.class))).thenReturn(expectedUser);

            User result =
              userService.registerUser(VALID_NAME, VALID_EMAIL, VALID_PASSWORD, VALID_ROLES);

            assertThat(result).isNotNull();
            assertThat(result.getName()).isEqualTo(VALID_NAME);
            assertThat(result.getEmail().getEmail()).isEqualTo(VALID_EMAIL);
            assertThat(result.getPassword()).isEqualTo(ENCODED_PASSWORD);
            assertThat(result.getRoles()).isEqualTo(VALID_ROLES);

            verify(passwordService).encodePassword(VALID_PASSWORD);
            verify(userRepository).save(any(User.class));
        }

        @Test
        @DisplayName("Should register user with multiple roles")
        void shouldRegisterUserWithMultipleRoles() {
            Set<Role> multipleRoles = Set.of(Role.USER, Role.ADMIN);
            User expectedUser =
              User.createLocalUser(VALID_NAME, VALID_EMAIL, ENCODED_PASSWORD, multipleRoles);

            when(passwordService.encodePassword(VALID_PASSWORD)).thenReturn(ENCODED_PASSWORD);
            when(userRepository.save(any(User.class))).thenReturn(expectedUser);

            User result =
              userService.registerUser(VALID_NAME, VALID_EMAIL, VALID_PASSWORD, multipleRoles);

            assertThat(result).isNotNull();
            assertThat(result.getRoles()).isEqualTo(multipleRoles);
            verify(passwordService).encodePassword(VALID_PASSWORD);
            verify(userRepository).save(any(User.class));
        }

        @Test
        @DisplayName("Should register user with admin role only")
        void shouldRegisterUserWithAdminRoleOnly() {
            Set<Role> adminRole = Set.of(Role.ADMIN);
            User expectedUser = User.createLocalUser(VALID_NAME, VALID_EMAIL, ENCODED_PASSWORD, adminRole);
            when(passwordService.encodePassword(VALID_PASSWORD)).thenReturn(ENCODED_PASSWORD);
            when(userRepository.save(any(User.class))).thenReturn(expectedUser);

            User result =
              userService.registerUser(VALID_NAME, VALID_EMAIL, VALID_PASSWORD, adminRole);

            assertThat(result).isNotNull();
            assertThat(result.getRoles()).isEqualTo(adminRole);
            verify(passwordService).encodePassword(VALID_PASSWORD);
            verify(userRepository).save(any(User.class));
        }

        @Test
        @DisplayName("Should normalize email to lowercase during registration")
        void shouldNormalizeEmailToLowercaseDuringRegistration() {
            String mixedCaseEmail = "John.Doe@EXAMPLE.COM";
            User expectedUser =
              User.createLocalUser(VALID_NAME, mixedCaseEmail.toLowerCase(), ENCODED_PASSWORD, VALID_ROLES);

            when(passwordService.encodePassword(VALID_PASSWORD)).thenReturn(ENCODED_PASSWORD);
            when(userRepository.save(any(User.class))).thenReturn(expectedUser);

            User result =
              userService.registerUser(VALID_NAME, mixedCaseEmail, VALID_PASSWORD, VALID_ROLES);

            assertThat(result).isNotNull();
            assertThat(result.getEmail().getEmail()).isEqualTo("john.doe@example.com");
            verify(passwordService).encodePassword(VALID_PASSWORD);
            verify(userRepository).save(any(User.class));
        }
    }

    @Nested
    @DisplayName("Validation Tests")
    class ValidationTests {

        @Test
        @DisplayName("Should throw exception when name is null")
        void shouldThrowExceptionWhenNameIsNull() {
            assertThatThrownBy(() ->
              userService.registerUser(null, VALID_EMAIL, VALID_PASSWORD, VALID_ROLES))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Name must not be null or blank");
        }

        @Test
        @DisplayName("Should throw exception when name is blank")
        void shouldThrowExceptionWhenNameIsBlank() {
            assertThatThrownBy(() ->
              userService.registerUser("", VALID_EMAIL, VALID_PASSWORD, VALID_ROLES))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Name must not be null or blank");
        }

        @Test
        @DisplayName("Should throw exception when email is null")
        void shouldThrowExceptionWhenEmailIsNull() {
            assertThatThrownBy(() ->
              userService.registerUser(VALID_NAME, null, VALID_PASSWORD, VALID_ROLES))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Email cannot be null or blank");
        }

        @Test
        @DisplayName("Should throw exception when email is blank")
        void shouldThrowExceptionWhenEmailIsBlank() {
            assertThatThrownBy(() ->
              userService.registerUser(VALID_NAME, "", VALID_PASSWORD, VALID_ROLES))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Email cannot be null or blank");
        }

        @Test
        @DisplayName("Should throw exception when password is null")
        void shouldThrowExceptionWhenPasswordIsNull() {
            assertThatThrownBy(() ->
              userService.registerUser(VALID_NAME, VALID_EMAIL, null, VALID_ROLES))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Password must not be null or blank");
        }

        @Test
        @DisplayName("Should throw exception when password is blank")
        void shouldThrowExceptionWhenPasswordIsBlank() {
            assertThatThrownBy(() ->
              userService.registerUser(VALID_NAME, VALID_EMAIL, "", VALID_ROLES))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Password must not be null or blank");
        }

        @Test
        @DisplayName("Should throw exception when email format is invalid")
        void shouldThrowExceptionWhenEmailFormatIsInvalid() {
            assertThatThrownBy(() ->
              userService.registerUser(VALID_NAME, "invalid-email", VALID_PASSWORD, VALID_ROLES))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid email format");
        }
    }

    @Nested
    @DisplayName("Integration Tests")
    class IntegrationTests {

        @Test
        @DisplayName("Should handle password encoding integration")
        void shouldHandlePasswordEncodingIntegration() {
            String differentPassword = "differentPassword123";
            String differentEncodedPassword = "$2a$10$differentEncodedHash";
            User expectedUser = User.createLocalUser(VALID_NAME, VALID_EMAIL, differentEncodedPassword, VALID_ROLES);

            when(passwordService.encodePassword(differentPassword)).thenReturn(differentEncodedPassword);
            when(userRepository.save(any(User.class))).thenReturn(expectedUser);

            User result = userService.registerUser(VALID_NAME, VALID_EMAIL, differentPassword, VALID_ROLES);

            assertThat(result).isNotNull();
            assertThat(result.getPassword()).isEqualTo(differentEncodedPassword);
            verify(passwordService).encodePassword(differentPassword);
            verify(userRepository).save(any(User.class));
        }

        @Test
        @DisplayName("Should handle repository save integration")
        void shouldHandleRepositorySaveIntegration() {
            User savedUser = User.createLocalUser(VALID_NAME, VALID_EMAIL, ENCODED_PASSWORD, VALID_ROLES);
            when(passwordService.encodePassword(VALID_PASSWORD)).thenReturn(ENCODED_PASSWORD);
            when(userRepository.save(any(User.class))).thenReturn(savedUser);

            User result =
              userService.registerUser(VALID_NAME, VALID_EMAIL, VALID_PASSWORD, VALID_ROLES);

            assertThat(result).isEqualTo(savedUser);
            verify(userRepository).save(any(User.class));
        }
    }

    @Nested
    @DisplayName("Circle of Trust Tests")
    class CircleOfTrustTests {

        @Test
        @DisplayName("Should return connected users")
        void shouldReturnConnectedUsers() {
            User current = User.createLocalUser("Alice", "alice@example.com", "p", VALID_ROLES);
            User connected = User.createLocalUser("Bob", "bob@example.com", "p", VALID_ROLES);
            ReflectionTestUtils.setField(current, "id", 1L);
            ReflectionTestUtils.setField(connected, "id", 2L);
            when(userConnectionRepository.findConnectedUsers(current)).thenReturn(List.of(connected));

            List<User> result = userService.getConnectedUsers(current);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getEmail().getEmail()).isEqualTo("bob@example.com");
        }

        @Test
        @DisplayName("Should connect two users bidirectionally")
        void shouldConnectTwoUsers() {
            User alice = User.createLocalUser("Alice", "alice@example.com", "p", VALID_ROLES);
            User bob = User.createLocalUser("Bob", "bob@example.com", "p", VALID_ROLES);
            ReflectionTestUtils.setField(alice, "id", 1L);
            ReflectionTestUtils.setField(bob, "id", 2L);
            when(userRepository.findByEmail(any(Email.class))).thenReturn(Optional.of(bob));
            when(userConnectionRepository.findByUserAndConnectedUser(eq(alice), eq(bob)))
                .thenReturn(Optional.empty());
            when(userConnectionRepository.save(any(UserConnection.class))).thenAnswer(inv -> inv.getArgument(0));

            userService.connectUsers(alice, "bob@example.com");

            verify(userConnectionRepository, times(2)).save(any(UserConnection.class));
            verify(userConnectionRepository).findByUserAndConnectedUser(alice, bob);
        }

        @Test
        @DisplayName("Should throw when connecting to non-existent email")
        void shouldThrowWhenConnectingToNonExistentEmail() {
            User alice = User.createLocalUser("Alice", "alice@example.com", "p", VALID_ROLES);
            when(userRepository.findByEmail(any(Email.class))).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.connectUsers(alice, "nobody@example.com"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("User not found");
        }

        @Test
        @DisplayName("Should throw when connecting to self")
        void shouldThrowWhenConnectingToSelf() {
            User alice = User.createLocalUser("Alice", "alice@example.com", "p", VALID_ROLES);
            ReflectionTestUtils.setField(alice, "id", 1L);
            when(userRepository.findByEmail(any(Email.class))).thenReturn(Optional.of(alice));

            assertThatThrownBy(() -> userService.connectUsers(alice, "alice@example.com"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("You cannot connect to yourself");
        }

        @Test
        @DisplayName("Should throw when users already connected")
        void shouldThrowWhenAlreadyConnected() {
            User alice = User.createLocalUser("Alice", "alice@example.com", "p", VALID_ROLES);
            User bob = User.createLocalUser("Bob", "bob@example.com", "p", VALID_ROLES);
            ReflectionTestUtils.setField(alice, "id", 1L);
            ReflectionTestUtils.setField(bob, "id", 2L);
            UserConnection existing = new UserConnection(alice, bob);
            when(userRepository.findByEmail(any(Email.class))).thenReturn(Optional.of(bob));
            when(userConnectionRepository.findByUserAndConnectedUser(eq(alice), eq(bob)))
                .thenReturn(Optional.of(existing));

            assertThatThrownBy(() -> userService.connectUsers(alice, "bob@example.com"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already connected");
        }
    }
}