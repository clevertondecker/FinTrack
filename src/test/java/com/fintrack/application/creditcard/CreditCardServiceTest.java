package com.fintrack.application.creditcard;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fintrack.domain.creditcard.*;
import com.fintrack.domain.user.Role;
import com.fintrack.domain.user.User;
import com.fintrack.domain.user.UserRepository;
import com.fintrack.dto.creditcard.CreateCreditCardRequest;
import com.fintrack.infrastructure.persistence.creditcard.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CreditCardService Tests")
class CreditCardServiceTest {

    @Mock
    private CreditCardJpaRepository creditCardRepository;

    @Mock
    private BankJpaRepository bankRepository;

    @Mock
    private UserRepository userRepository;

    private CreditCardService creditCardService;

    private User testUser;
    private Bank testBank;
    private CreditCard testCreditCard;

    @BeforeEach
    void setUp() {
        creditCardService = new CreditCardService(
            creditCardRepository, bankRepository, userRepository);

        testUser = User.of("John Doe", "john@example.com", "password123", Set.of(Role.USER));
        testBank = Bank.of("NU", "Nubank");
        testCreditCard = CreditCard.of("Test Card", "1234", new BigDecimal("5000.00"), testUser, testBank);
    }

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {

        @Test
        @DisplayName("Should create CreditCardService with valid dependencies")
        void shouldCreateCreditCardServiceWithValidDependencies() {
            assertNotNull(creditCardService);
        }
    }

    @Nested
    @DisplayName("findUserByUsername Tests")
    class FindUserByUsernameTests {

        @Test
        @DisplayName("Should find user by valid username")
        void shouldFindUserByValidUsername() {
            when(userRepository.findByEmail(any())).thenReturn(Optional.of(testUser));

            Optional<User> result = creditCardService.findUserByUsername("john@example.com");

            assertTrue(result.isPresent());
            assertEquals(testUser, result.get());
        }

        @Test
        @DisplayName("Should return empty when username is null")
        void shouldReturnEmptyWhenUsernameIsNull() {
            Optional<User> result = creditCardService.findUserByUsername(null);

            assertFalse(result.isPresent());
        }

        @Test
        @DisplayName("Should return empty when username is empty")
        void shouldReturnEmptyWhenUsernameIsEmpty() {
            Optional<User> result = creditCardService.findUserByUsername("");

            assertFalse(result.isPresent());
        }

        @Test
        @DisplayName("Should return empty when user not found")
        void shouldReturnEmptyWhenUserNotFound() {
            when(userRepository.findByEmail(any())).thenReturn(Optional.empty());

            Optional<User> result =
              creditCardService.findUserByUsername("nonexistent@example.com");

            assertFalse(result.isPresent());
        }
    }

    @Nested
    @DisplayName("createCreditCard Tests")
    class CreateCreditCardTests {

        @Test
        @DisplayName("Should create credit card successfully")
        void shouldCreateCreditCardSuccessfully() {
            CreateCreditCardRequest request = new CreateCreditCardRequest(
                "Test Card", "1234", new BigDecimal("5000.00"), 1L, CardType.PHYSICAL, null, null);

            when(bankRepository.findById(1L))
              .thenReturn(Optional.of(testBank));

            when(creditCardRepository.save(any()))
              .thenReturn(testCreditCard);

            CreditCard result =
              creditCardService.createCreditCard(request, testUser);

            assertNotNull(result);
            assertEquals(testCreditCard, result);
        }

        @Test
        @DisplayName("Should throw exception when bank not found")
        void shouldThrowExceptionWhenBankNotFound() {
            CreateCreditCardRequest request = new CreateCreditCardRequest(
                "Test Card", "1234", new BigDecimal("5000.00"), 999L, CardType.PHYSICAL, null, null);

            when(bankRepository.findById(999L)).thenReturn(Optional.empty());

            assertThrows(IllegalArgumentException.class, () ->
                creditCardService.createCreditCard(request, testUser));
        }
    }

    @Nested
    @DisplayName("getUserCreditCards Tests")
    class GetUserCreditCardsTests {

        @Test
        @DisplayName("Should return user credit cards")
        void shouldReturnUserCreditCards() {
            List<CreditCard> expectedCards = List.of(testCreditCard);
            when(creditCardRepository.findByOwner(testUser)).thenReturn(expectedCards);

            List<CreditCard> result = creditCardService.getUserCreditCards(testUser);

            assertEquals(expectedCards, result);
        }
    }

    @Nested
    @DisplayName("getCreditCard Tests")
    class GetCreditCardTests {

        @Test
        @DisplayName("Should return credit card by ID")
        void shouldReturnCreditCardById() {
            when(creditCardRepository.findByIdAndOwner(1L, testUser)).thenReturn(Optional.of(testCreditCard));

            CreditCard result = creditCardService.getCreditCard(1L, testUser);

            assertEquals(testCreditCard, result);
        }

        @Test
        @DisplayName("Should throw exception when credit card not found")
        void shouldThrowExceptionWhenCreditCardNotFound() {
            when(creditCardRepository.findByIdAndOwner(999L, testUser)).thenReturn(Optional.empty());

            assertThrows(IllegalArgumentException.class, () ->
                creditCardService.getCreditCard(999L, testUser));
        }
    }

    @Nested
    @DisplayName("activateCreditCard Tests")
    class ActivateCreditCardTests {

        @Test
        @DisplayName("Should activate credit card successfully")
        void shouldActivateCreditCardSuccessfully() {
            CreditCard inactiveCard =
              CreditCard.of("Test Card", "1234", new BigDecimal("5000.00"), testUser, testBank);
            inactiveCard.deactivate();

            when(creditCardRepository.findByIdAndOwner(1L, testUser))
              .thenReturn(Optional.of(inactiveCard));

            when(creditCardRepository.save(any()))
              .thenReturn(inactiveCard);

            CreditCard result = creditCardService.activateCreditCard(1L, testUser);

            assertNotNull(result);
            assertTrue(result.isActive());
        }
    }

    @Nested
    @DisplayName("deactivateCreditCard Tests")
    class DeactivateCreditCardTests {

        @Test
        @DisplayName("Should deactivate credit card successfully")
        void shouldDeactivateCreditCardSuccessfully() {
            when(creditCardRepository.findByIdAndOwner(1L, testUser))
              .thenReturn(Optional.of(testCreditCard));

            when(creditCardRepository.save(any())).thenReturn(testCreditCard);

            CreditCard result = creditCardService.deactivateCreditCard(1L, testUser);

            assertNotNull(result);
            assertFalse(result.isActive());
        }

        @Test
        @DisplayName("Should throw exception when credit card not found")
        void shouldThrowExceptionWhenCreditCardNotFound() {
            when(creditCardRepository.findByIdAndOwner(999L, testUser))
              .thenReturn(Optional.empty());

            assertThrows(IllegalArgumentException.class, () ->
                creditCardService.deactivateCreditCard(999L, testUser));
        }

        @Test
        @DisplayName("Should deactivate already inactive credit card")
        void shouldDeactivateAlreadyInactiveCreditCard() {
            CreditCard inactiveCard = CreditCard.of("Test Card", "1234", new BigDecimal("5000.00"), testUser, testBank);
            inactiveCard.deactivate();

            when(creditCardRepository.findByIdAndOwner(1L, testUser))
              .thenReturn(Optional.of(inactiveCard));

            when(creditCardRepository.save(any())).thenReturn(inactiveCard);

            CreditCard result = creditCardService.deactivateCreditCard(1L, testUser);

            assertNotNull(result);
            assertFalse(result.isActive());
        }
    }

    @Nested
    @DisplayName("updateCreditCard Tests")
    class UpdateCreditCardTests {

        @Test
        @DisplayName("Should update credit card successfully")
        void shouldUpdateCreditCardSuccessfully() {
            CreateCreditCardRequest request = new CreateCreditCardRequest(
                "Updated Card", "5678", new BigDecimal("10000.00"), 1L, CardType.PHYSICAL, null, null);

            when(creditCardRepository.findByIdAndOwner(1L, testUser))
              .thenReturn(Optional.of(testCreditCard));

            when(bankRepository.findById(1L)).thenReturn(Optional.of(testBank));
            when(creditCardRepository.save(any())).thenReturn(testCreditCard);

            CreditCard result = creditCardService.updateCreditCard(1L, request, testUser);

            assertNotNull(result);
            assertEquals(testCreditCard, result);
        }

        @Test
        @DisplayName("Should throw exception when credit card not found")
        void shouldThrowExceptionWhenCreditCardNotFound() {
            CreateCreditCardRequest request = new CreateCreditCardRequest(
                "Updated Card", "5678", new BigDecimal("10000.00"), 1L, CardType.PHYSICAL, null, null);

            when(creditCardRepository.findByIdAndOwner(999L, testUser))
              .thenReturn(Optional.empty());

            assertThrows(IllegalArgumentException.class, () ->
                creditCardService.updateCreditCard(999L, request, testUser));
        }

        @Test
        @DisplayName("Should throw exception when bank not found")
        void shouldThrowExceptionWhenBankNotFound() {
            CreateCreditCardRequest request = new CreateCreditCardRequest(
                "Updated Card", "5678", new BigDecimal("10000.00"), 999L, CardType.PHYSICAL, null, null);

            when(creditCardRepository.findByIdAndOwner(1L, testUser))
              .thenReturn(Optional.of(testCreditCard));

            when(bankRepository.findById(999L)).thenReturn(Optional.empty());

            assertThrows(IllegalArgumentException.class, () ->
                creditCardService.updateCreditCard(1L, request, testUser));
        }

        @Test
        @DisplayName("Should update credit card with different bank")
        void shouldUpdateCreditCardWithDifferentBank() {
            Bank newBank = Bank.of("IT", "Itaú");
            CreateCreditCardRequest request = new CreateCreditCardRequest(
                "Updated Card", "5678", new BigDecimal("10000.00"), 2L, CardType.PHYSICAL, null, null);

            when(creditCardRepository.findByIdAndOwner(1L, testUser))
              .thenReturn(Optional.of(testCreditCard));

            when(bankRepository.findById(2L)).thenReturn(Optional.of(newBank));
            when(creditCardRepository.save(any())).thenReturn(testCreditCard);

            CreditCard result = creditCardService.updateCreditCard(1L, request, testUser);

            assertNotNull(result);
            assertEquals(testCreditCard, result);
        }
    }

    @Nested
    @DisplayName("DTO Conversion Tests")
    class DtoConversionTests {

        @Test
        @DisplayName("Should convert credit card to DTO")
        void shouldConvertCreditCardToDto() {
            Map<String, Object> result = creditCardService.toCreditCardDto(testCreditCard);

            assertNotNull(result);
            assertEquals(testCreditCard.getId(), result.get("id"));
            assertEquals(testCreditCard.getName(), result.get("name"));
            assertEquals(testCreditCard.getLastFourDigits(), result.get("lastFourDigits"));
            assertEquals(testCreditCard.getLimit(), result.get("limit"));
            assertEquals(testCreditCard.getBank().getName(), result.get("bankName"));
            assertEquals(testCreditCard.getBank().getCode(), result.get("bankCode"));
            assertEquals(testCreditCard.isActive(), result.get("active"));
        }

        @Test
        @DisplayName("Should convert list of credit cards to DTOs")
        void shouldConvertListOfCreditCardsToDtos() {
            List<CreditCard> creditCards = List.of(testCreditCard);
            List<Map<String, Object>> result = creditCardService.toCreditCardDtos(creditCards);

            assertNotNull(result);
            assertEquals(1, result.size());
        }
    }
}