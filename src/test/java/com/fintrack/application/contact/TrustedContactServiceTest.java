package com.fintrack.application.contact;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.fintrack.domain.contact.TrustedContact;
import com.fintrack.domain.user.Email;
import com.fintrack.domain.user.Role;
import com.fintrack.domain.user.User;
import com.fintrack.domain.user.UserConnection;
import com.fintrack.domain.user.UserRepository;
import com.fintrack.infrastructure.persistence.contact.TrustedContactJpaRepository;
import com.fintrack.infrastructure.persistence.user.UserConnectionJpaRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
@DisplayName("TrustedContact Service")
class TrustedContactServiceTest {

    @Mock
    private TrustedContactJpaRepository repository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserConnectionJpaRepository userConnectionRepository;

    @Captor
    private ArgumentCaptor<UserConnection> connectionCaptor;

    private TrustedContactService service;

    private User owner;
    private User sabrina;
    private User thirdUser;

    private static final Set<Role> ROLES = Set.of(Role.USER);

    @BeforeEach
    void setUp() {
        service = new TrustedContactService(repository, userRepository, userConnectionRepository);

        owner = User.createLocalUser("Cleverton", "cleverton@example.com", "pwd", ROLES);
        ReflectionTestUtils.setField(owner, "id", 1L);

        sabrina = User.createLocalUser("Sabrina", "sabrina@example.com", "pwd", ROLES);
        ReflectionTestUtils.setField(sabrina, "id", 2L);

        thirdUser = User.createLocalUser("Carlos", "carlos@example.com", "pwd", ROLES);
        ReflectionTestUtils.setField(thirdUser, "id", 3L);
    }

    @Nested
    @DisplayName("Auto-connect on create")
    class AutoConnectOnCreateTests {

        @Test
        @DisplayName("Should create bidirectional UserConnection when contact email matches a registered user")
        void shouldAutoConnectBidirectionallyWhenEmailMatchesRegisteredUser() {
            when(repository.existsByOwnerAndEmail(owner, "sabrina@example.com")).thenReturn(false);
            when(repository.save(any(TrustedContact.class))).thenAnswer(inv -> inv.getArgument(0));
            when(userRepository.findByEmail(Email.of("sabrina@example.com"))).thenReturn(Optional.of(sabrina));
            when(userConnectionRepository.findByUserAndConnectedUser(owner, sabrina)).thenReturn(Optional.empty());
            when(userConnectionRepository.save(any(UserConnection.class))).thenAnswer(inv -> inv.getArgument(0));

            service.create(owner, "Sabrina", "sabrina@example.com", null, null);

            verify(userConnectionRepository, times(2)).save(connectionCaptor.capture());
            List<UserConnection> saved = connectionCaptor.getAllValues();
            assertThat(saved.get(0).getUser()).isEqualTo(owner);
            assertThat(saved.get(0).getConnectedUser()).isEqualTo(sabrina);
            assertThat(saved.get(1).getUser()).isEqualTo(sabrina);
            assertThat(saved.get(1).getConnectedUser()).isEqualTo(owner);
        }

        @Test
        @DisplayName("Should not create duplicate connection if users are already connected")
        void shouldNotCreateDuplicateConnection() {
            when(repository.existsByOwnerAndEmail(owner, "sabrina@example.com")).thenReturn(false);
            when(repository.save(any(TrustedContact.class))).thenAnswer(inv -> inv.getArgument(0));
            when(userRepository.findByEmail(Email.of("sabrina@example.com"))).thenReturn(Optional.of(sabrina));
            when(userConnectionRepository.findByUserAndConnectedUser(owner, sabrina))
                .thenReturn(Optional.of(new UserConnection(owner, sabrina)));

            service.create(owner, "Sabrina", "sabrina@example.com", null, null);

            verify(userConnectionRepository, never()).save(any(UserConnection.class));
        }

        @Test
        @DisplayName("Should not auto-connect when email does not match any registered user")
        void shouldNotAutoConnectWhenEmailNotRegistered() {
            when(repository.existsByOwnerAndEmail(owner, "stranger@example.com")).thenReturn(false);
            when(repository.save(any(TrustedContact.class))).thenAnswer(inv -> inv.getArgument(0));
            when(userRepository.findByEmail(Email.of("stranger@example.com"))).thenReturn(Optional.empty());

            service.create(owner, "Stranger", "stranger@example.com", null, null);

            verify(userConnectionRepository, never()).save(any(UserConnection.class));
        }

        @Test
        @DisplayName("Should not auto-connect when contact email is owner's own email")
        void shouldNotAutoConnectToSelf() {
            when(repository.existsByOwnerAndEmail(owner, "cleverton@example.com")).thenReturn(false);
            when(repository.save(any(TrustedContact.class))).thenAnswer(inv -> inv.getArgument(0));
            when(userRepository.findByEmail(Email.of("cleverton@example.com"))).thenReturn(Optional.of(owner));

            service.create(owner, "Myself", "cleverton@example.com", null, null);

            verify(userConnectionRepository, never()).save(any(UserConnection.class));
        }

        @Test
        @DisplayName("Should normalize email before checking for auto-connect")
        void shouldNormalizeEmailBeforeAutoConnect() {
            when(repository.existsByOwnerAndEmail(owner, "sabrina@example.com")).thenReturn(false);
            when(repository.save(any(TrustedContact.class))).thenAnswer(inv -> inv.getArgument(0));
            when(userRepository.findByEmail(Email.of("sabrina@example.com"))).thenReturn(Optional.of(sabrina));
            when(userConnectionRepository.findByUserAndConnectedUser(owner, sabrina)).thenReturn(Optional.empty());
            when(userConnectionRepository.save(any(UserConnection.class))).thenAnswer(inv -> inv.getArgument(0));

            service.create(owner, "Sabrina", "  SABRINA@Example.COM  ", null, null);

            verify(userRepository).findByEmail(Email.of("sabrina@example.com"));
            verify(userConnectionRepository, times(2)).save(any(UserConnection.class));
        }

        @Test
        @DisplayName("Should still save the TrustedContact even when auto-connect happens")
        void shouldSaveContactAndAutoConnect() {
            when(repository.existsByOwnerAndEmail(owner, "sabrina@example.com")).thenReturn(false);
            when(repository.save(any(TrustedContact.class))).thenAnswer(inv -> inv.getArgument(0));
            when(userRepository.findByEmail(Email.of("sabrina@example.com"))).thenReturn(Optional.of(sabrina));
            when(userConnectionRepository.findByUserAndConnectedUser(owner, sabrina)).thenReturn(Optional.empty());
            when(userConnectionRepository.save(any(UserConnection.class))).thenAnswer(inv -> inv.getArgument(0));

            TrustedContact result = service.create(owner, "Sabrina", "sabrina@example.com", "family", "my wife");

            assertThat(result).isNotNull();
            assertThat(result.getName()).isEqualTo("Sabrina");
            assertThat(result.getEmail()).isEqualTo("sabrina@example.com");
            verify(repository).save(any(TrustedContact.class));
            verify(userConnectionRepository, times(2)).save(any(UserConnection.class));
        }

        @Test
        @DisplayName("Should auto-connect user that appears in dropdown for credit card assignment")
        void shouldMakeUserAvailableForCreditCardAssignment() {
            when(repository.existsByOwnerAndEmail(owner, "sabrina@example.com")).thenReturn(false);
            when(repository.save(any(TrustedContact.class))).thenAnswer(inv -> inv.getArgument(0));
            when(userRepository.findByEmail(Email.of("sabrina@example.com"))).thenReturn(Optional.of(sabrina));
            when(userConnectionRepository.findByUserAndConnectedUser(owner, sabrina)).thenReturn(Optional.empty());
            when(userConnectionRepository.save(any(UserConnection.class))).thenAnswer(inv -> inv.getArgument(0));

            service.create(owner, "Sabrina", "sabrina@example.com", null, null);

            verify(userConnectionRepository).save(argThat(conn ->
                conn.getUser().equals(owner) && conn.getConnectedUser().equals(sabrina)));
            verify(userConnectionRepository).save(argThat(conn ->
                conn.getUser().equals(sabrina) && conn.getConnectedUser().equals(owner)));
        }
    }

    @Nested
    @DisplayName("Auto-connect on update")
    class AutoConnectOnUpdateTests {

        @Test
        @DisplayName("Should auto-connect when email is changed to a registered user's email")
        void shouldAutoConnectOnEmailChange() {
            TrustedContact contact = TrustedContact.create(owner, "Old Name", "old@example.com", null, null);
            ReflectionTestUtils.setField(contact, "id", 10L);

            when(repository.findByIdAndOwner(10L, owner)).thenReturn(Optional.of(contact));
            when(repository.existsByOwnerAndEmail(owner, "sabrina@example.com")).thenReturn(false);
            when(repository.save(any(TrustedContact.class))).thenAnswer(inv -> inv.getArgument(0));
            when(userRepository.findByEmail(Email.of("sabrina@example.com"))).thenReturn(Optional.of(sabrina));
            when(userConnectionRepository.findByUserAndConnectedUser(owner, sabrina)).thenReturn(Optional.empty());
            when(userConnectionRepository.save(any(UserConnection.class))).thenAnswer(inv -> inv.getArgument(0));

            service.update(owner, 10L, "Sabrina", "sabrina@example.com", null, null);

            verify(userConnectionRepository, times(2)).save(connectionCaptor.capture());
            List<UserConnection> saved = connectionCaptor.getAllValues();
            assertThat(saved.get(0).getUser()).isEqualTo(owner);
            assertThat(saved.get(0).getConnectedUser()).isEqualTo(sabrina);
        }

        @Test
        @DisplayName("Should not auto-connect when email is null on update")
        void shouldNotAutoConnectWhenEmailIsNullOnUpdate() {
            TrustedContact contact = TrustedContact.create(owner, "Sabrina", "sabrina@example.com", null, null);
            ReflectionTestUtils.setField(contact, "id", 10L);

            when(repository.findByIdAndOwner(10L, owner)).thenReturn(Optional.of(contact));
            when(repository.save(any(TrustedContact.class))).thenAnswer(inv -> inv.getArgument(0));

            service.update(owner, 10L, "Sabrina Updated", null, null, null);

            verify(userConnectionRepository, never()).save(any(UserConnection.class));
        }

        @Test
        @DisplayName("Should not auto-connect when email is blank on update")
        void shouldNotAutoConnectWhenEmailIsBlankOnUpdate() {
            TrustedContact contact = TrustedContact.create(owner, "Sabrina", "sabrina@example.com", null, null);
            ReflectionTestUtils.setField(contact, "id", 10L);

            when(repository.findByIdAndOwner(10L, owner)).thenReturn(Optional.of(contact));
            when(repository.save(any(TrustedContact.class))).thenAnswer(inv -> inv.getArgument(0));

            service.update(owner, 10L, "Sabrina Updated", "   ", null, null);

            verify(userConnectionRepository, never()).save(any(UserConnection.class));
        }

        @Test
        @DisplayName("Should skip duplicate connection on update if already connected")
        void shouldSkipDuplicateOnUpdate() {
            TrustedContact contact = TrustedContact.create(owner, "Sabrina", "old@example.com", null, null);
            ReflectionTestUtils.setField(contact, "id", 10L);

            when(repository.findByIdAndOwner(10L, owner)).thenReturn(Optional.of(contact));
            when(repository.existsByOwnerAndEmail(owner, "sabrina@example.com")).thenReturn(false);
            when(repository.save(any(TrustedContact.class))).thenAnswer(inv -> inv.getArgument(0));
            when(userRepository.findByEmail(Email.of("sabrina@example.com"))).thenReturn(Optional.of(sabrina));
            when(userConnectionRepository.findByUserAndConnectedUser(owner, sabrina))
                .thenReturn(Optional.of(new UserConnection(owner, sabrina)));

            service.update(owner, 10L, "Sabrina", "sabrina@example.com", null, null);

            verify(userConnectionRepository, never()).save(any(UserConnection.class));
        }

        @Test
        @DisplayName("Should auto-connect when email is kept the same but connection didn't exist yet")
        void shouldAutoConnectOnSaveEvenIfEmailUnchanged() {
            TrustedContact contact = TrustedContact.create(owner, "Sabrina", "sabrina@example.com", null, null);
            ReflectionTestUtils.setField(contact, "id", 10L);

            when(repository.findByIdAndOwner(10L, owner)).thenReturn(Optional.of(contact));
            when(repository.save(any(TrustedContact.class))).thenAnswer(inv -> inv.getArgument(0));
            when(userRepository.findByEmail(Email.of("sabrina@example.com"))).thenReturn(Optional.of(sabrina));
            when(userConnectionRepository.findByUserAndConnectedUser(owner, sabrina)).thenReturn(Optional.empty());
            when(userConnectionRepository.save(any(UserConnection.class))).thenAnswer(inv -> inv.getArgument(0));

            service.update(owner, 10L, "Sabrina Updated", "sabrina@example.com", "family", null);

            verify(userConnectionRepository, times(2)).save(any(UserConnection.class));
        }
    }

    @Nested
    @DisplayName("Create - validation and basic flow")
    class CreateTests {

        @Test
        @DisplayName("Should throw when contact with same email already exists for owner")
        void shouldThrowWhenDuplicateEmail() {
            when(repository.existsByOwnerAndEmail(owner, "sabrina@example.com")).thenReturn(true);

            assertThatThrownBy(() ->
                service.create(owner, "Sabrina", "sabrina@example.com", null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already exists");

            verify(repository, never()).save(any(TrustedContact.class));
            verify(userConnectionRepository, never()).save(any(UserConnection.class));
        }

        @Test
        @DisplayName("Should create contact successfully without auto-connect for non-registered email")
        void shouldCreateContactWithoutAutoConnect() {
            TrustedContact expected = TrustedContact.create(owner, "Stranger", "stranger@example.com", null, null);
            when(repository.existsByOwnerAndEmail(owner, "stranger@example.com")).thenReturn(false);
            when(repository.save(any(TrustedContact.class))).thenReturn(expected);
            when(userRepository.findByEmail(Email.of("stranger@example.com"))).thenReturn(Optional.empty());

            TrustedContact result = service.create(owner, "Stranger", "stranger@example.com", null, null);

            assertThat(result).isNotNull();
            assertThat(result.getName()).isEqualTo("Stranger");
            verify(repository).save(any(TrustedContact.class));
            verify(userConnectionRepository, never()).save(any(UserConnection.class));
        }
    }

    @Nested
    @DisplayName("Update - validation and basic flow")
    class UpdateTests {

        @Test
        @DisplayName("Should throw when contact not found for owner")
        void shouldThrowWhenContactNotFound() {
            when(repository.findByIdAndOwner(999L, owner)).thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                service.update(owner, 999L, "Name", "email@x.com", null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Contact not found");
        }

        @Test
        @DisplayName("Should throw when updated email already belongs to another contact of the same owner")
        void shouldThrowWhenUpdatedEmailConflicts() {
            TrustedContact contact = TrustedContact.create(owner, "Sabrina", "sabrina@example.com", null, null);
            ReflectionTestUtils.setField(contact, "id", 10L);

            when(repository.findByIdAndOwner(10L, owner)).thenReturn(Optional.of(contact));
            when(repository.existsByOwnerAndEmail(owner, "carlos@example.com")).thenReturn(true);

            assertThatThrownBy(() ->
                service.update(owner, 10L, "Sabrina", "carlos@example.com", null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already exists");
        }
    }

    @Nested
    @DisplayName("Delete")
    class DeleteTests {

        @Test
        @DisplayName("Should delete contact without affecting UserConnection")
        void shouldDeleteContactWithoutAffectingConnection() {
            TrustedContact contact = TrustedContact.create(owner, "Sabrina", "sabrina@example.com", null, null);
            ReflectionTestUtils.setField(contact, "id", 10L);

            when(repository.findByIdAndOwner(10L, owner)).thenReturn(Optional.of(contact));

            service.delete(owner, 10L);

            verify(repository).delete(contact);
            verify(userConnectionRepository, never()).save(any(UserConnection.class));
        }

        @Test
        @DisplayName("Should throw when deleting non-existent contact")
        void shouldThrowWhenDeletingNonExistentContact() {
            when(repository.findByIdAndOwner(999L, owner)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.delete(owner, 999L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Contact not found");
        }
    }

    @Nested
    @DisplayName("Find operations")
    class FindTests {

        @Test
        @DisplayName("Should find contacts by owner with search filter")
        void shouldFindByOwnerWithSearch() {
            TrustedContact contact = TrustedContact.create(owner, "Sabrina", "sabrina@example.com", null, null);
            when(repository.findByOwnerAndSearch(owner, "sabrina")).thenReturn(List.of(contact));

            List<TrustedContact> result = service.findByOwner(owner, "sabrina");

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getName()).isEqualTo("Sabrina");
        }

        @Test
        @DisplayName("Should find all contacts by owner when no search filter")
        void shouldFindAllByOwner() {
            TrustedContact c1 = TrustedContact.create(owner, "Sabrina", "sabrina@example.com", null, null);
            TrustedContact c2 = TrustedContact.create(owner, "Carlos", "carlos@example.com", null, null);
            when(repository.findByOwnerOrderByNameAsc(owner)).thenReturn(List.of(c2, c1));

            List<TrustedContact> result = service.findByOwner(owner, null);

            assertThat(result).hasSize(2);
        }

        @Test
        @DisplayName("Should find contact by id and owner")
        void shouldFindByIdAndOwner() {
            TrustedContact contact = TrustedContact.create(owner, "Sabrina", "sabrina@example.com", null, null);
            ReflectionTestUtils.setField(contact, "id", 10L);
            when(repository.findByIdAndOwner(10L, owner)).thenReturn(Optional.of(contact));

            TrustedContact result = service.findByIdAndOwner(10L, owner);

            assertThat(result.getName()).isEqualTo("Sabrina");
        }
    }
}
