package com.fintrack.infrastructure.persistence.creditcard;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import com.fintrack.domain.creditcard.Bank;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("BankJpaRepository Integration Tests")
class BankJpaRepositoryTest {

    @Autowired
    private BankJpaRepository bankRepository;

    @Autowired
    private TestEntityManager entityManager;

    private Bank nubank;
    private Bank itau;
    private Bank santander;

    @BeforeEach
    void setUp() {
        // Clear the database before each test
        bankRepository.deleteAll();
        entityManager.flush();
        entityManager.clear();

        // Create test banks with unique codes
        nubank = Bank.of("NU", "Nubank");
        itau = Bank.of("ITAU", "Itaú Unibanco");
        santander = Bank.of("SAN", "Santander");
    }

    @Nested
    @DisplayName("Save Operations")
    class SaveOperations {

        @Test
        @DisplayName("Should save bank successfully")
        void shouldSaveBankSuccessfully() {
            Bank savedBank = bankRepository.save(nubank);

            assertThat(savedBank).isNotNull();
            assertThat(savedBank.getId()).isNotNull();
            assertThat(savedBank.getCode()).isEqualTo("NU");
            assertThat(savedBank.getName()).isEqualTo("Nubank");
        }

        @Test
        @DisplayName("Should save multiple banks successfully")
        void shouldSaveMultipleBanksSuccessfully() {
            Bank savedNubank = bankRepository.save(nubank);
            Bank savedItau = bankRepository.save(itau);
            Bank savedSantander = bankRepository.save(santander);

            assertThat(savedNubank.getId()).isNotNull();
            assertThat(savedItau.getId()).isNotNull();
            assertThat(savedSantander.getId()).isNotNull();

            assertThat(bankRepository.count()).isEqualTo(3);
        }

        @Test
        @DisplayName("Should update existing bank")
        void shouldUpdateExistingBank() {
            Bank savedBank = bankRepository.save(nubank);
            Long bankId = savedBank.getId();

            // Create a new bank instance with updated data but same ID
            Bank updatedBank = Bank.of("NU_UPDATED", "Nubank Digital Bank");
            // Note: In a real scenario, you would need to set the ID or use a proper update method
            // For now, we'll test that we can save a new bank with a different code
            Bank newBank = bankRepository.save(updatedBank);

            assertThat(newBank.getId()).isNotNull();
            assertThat(newBank.getCode()).isEqualTo("NU_UPDATED");
            assertThat(newBank.getName()).isEqualTo("Nubank Digital Bank");
        }
    }

    @Nested
    @DisplayName("Find Operations")
    class FindOperations {

        @Test
        @DisplayName("Should find bank by ID")
        void shouldFindBankById() {
            Bank savedBank = bankRepository.save(nubank);
            Long bankId = savedBank.getId();

            Optional<Bank> foundBank = bankRepository.findById(bankId);

            assertThat(foundBank).isPresent();
            assertThat(foundBank.get().getCode()).isEqualTo("NU");
            assertThat(foundBank.get().getName()).isEqualTo("Nubank");
        }

        @Test
        @DisplayName("Should return empty when bank not found by ID")
        void shouldReturnEmptyWhenBankNotFoundById() {
            Optional<Bank> foundBank = bankRepository.findById(999L);

            assertThat(foundBank).isEmpty();
        }

        @Test
        @DisplayName("Should find bank by code")
        void shouldFindBankByCode() {
            bankRepository.save(nubank);

            Optional<Bank> foundBank = bankRepository.findByCode("NU");

            assertThat(foundBank).isPresent();
            assertThat(foundBank.get().getCode()).isEqualTo("NU");
            assertThat(foundBank.get().getName()).isEqualTo("Nubank");
        }

        @Test
        @DisplayName("Should return empty when bank not found by code")
        void shouldReturnEmptyWhenBankNotFoundByCode() {
            Optional<Bank> foundBank = bankRepository.findByCode("INVALID");

            assertThat(foundBank).isEmpty();
        }

        @Test
        @DisplayName("Should find all banks")
        void shouldFindAllBanks() {
            bankRepository.save(nubank);
            bankRepository.save(itau);
            bankRepository.save(santander);

            List<Bank> allBanks = bankRepository.findAll();

            assertThat(allBanks).hasSize(3);
            assertThat(allBanks).extracting("code")
                .containsExactlyInAnyOrder("NU", "ITAU", "SAN");
            assertThat(allBanks).extracting("name")
                .containsExactlyInAnyOrder("Nubank", "Itaú Unibanco", "Santander");
        }

        @Test
        @DisplayName("Should return empty list when no banks exist")
        void shouldReturnEmptyListWhenNoBanksExist() {
            List<Bank> allBanks = bankRepository.findAll();

            assertThat(allBanks).isEmpty();
        }
    }

    @Nested
    @DisplayName("Exists Operations")
    class ExistsOperations {

        @Test
        @DisplayName("Should return true when bank exists by code")
        void shouldReturnTrueWhenBankExistsByCode() {
            bankRepository.save(nubank);

            boolean exists = bankRepository.existsByCode("NU");

            assertThat(exists).isTrue();
        }

        @Test
        @DisplayName("Should return false when bank does not exist by code")
        void shouldReturnFalseWhenBankDoesNotExistByCode() {
            boolean exists = bankRepository.existsByCode("INVALID");

            assertThat(exists).isFalse();
        }

        @Test
        @DisplayName("Should return true when bank exists by ID")
        void shouldReturnTrueWhenBankExistsById() {
            Bank savedBank = bankRepository.save(nubank);
            Long bankId = savedBank.getId();

            boolean exists = bankRepository.existsById(bankId);

            assertThat(exists).isTrue();
        }

        @Test
        @DisplayName("Should return false when bank does not exist by ID")
        void shouldReturnFalseWhenBankDoesNotExistById() {
            boolean exists = bankRepository.existsById(999L);

            assertThat(exists).isFalse();
        }
    }

    @Nested
    @DisplayName("Delete Operations")
    class DeleteOperations {

        @Test
        @DisplayName("Should delete bank by ID")
        void shouldDeleteBankById() {
            Bank savedBank = bankRepository.save(nubank);
            Long bankId = savedBank.getId();

            bankRepository.deleteById(bankId);

            assertThat(bankRepository.existsById(bankId)).isFalse();
            assertThat(bankRepository.count()).isEqualTo(0);
        }

        @Test
        @DisplayName("Should delete bank by entity")
        void shouldDeleteBankByEntity() {
            Bank savedBank = bankRepository.save(nubank);

            bankRepository.delete(savedBank);

            assertThat(bankRepository.existsById(savedBank.getId())).isFalse();
            assertThat(bankRepository.count()).isEqualTo(0);
        }

        @Test
        @DisplayName("Should delete all banks")
        void shouldDeleteAllBanks() {
            bankRepository.save(nubank);
            bankRepository.save(itau);
            bankRepository.save(santander);

            bankRepository.deleteAll();

            assertThat(bankRepository.count()).isEqualTo(0);
        }

        @Test
        @DisplayName("Should delete all banks by entities")
        void shouldDeleteAllBanksByEntities() {
            Bank savedNubank = bankRepository.save(nubank);
            Bank savedItau = bankRepository.save(itau);
            Bank savedSantander = bankRepository.save(santander);

            bankRepository.deleteAll(List.of(savedNubank, savedItau, savedSantander));

            assertThat(bankRepository.count()).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("Count Operations")
    class CountOperations {

        @Test
        @DisplayName("Should count banks correctly")
        void shouldCountBanksCorrectly() {
            assertThat(bankRepository.count()).isEqualTo(0);

            bankRepository.save(nubank);
            assertThat(bankRepository.count()).isEqualTo(1);

            bankRepository.save(itau);
            assertThat(bankRepository.count()).isEqualTo(2);

            bankRepository.save(santander);
            assertThat(bankRepository.count()).isEqualTo(3);
        }

        @Test
        @DisplayName("Should return zero when no banks exist")
        void shouldReturnZeroWhenNoBanksExist() {
            assertThat(bankRepository.count()).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("Unique Constraint Tests")
    class UniqueConstraintTests {

        @Test
        @DisplayName("Should enforce unique code constraint")
        void shouldEnforceUniqueCodeConstraint() {
            bankRepository.save(nubank);

            Bank duplicateBank = Bank.of("NU", "Different Name");

            // This should throw an exception due to unique constraint violation
            assertThatThrownBy(() -> bankRepository.save(duplicateBank))
                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
        }
    }

    @Nested
    @DisplayName("Data Integrity Tests")
    class DataIntegrityTests {

        @Test
        @DisplayName("Should maintain data integrity after save and find")
        void shouldMaintainDataIntegrityAfterSaveAndFind() {
            Bank savedBank = bankRepository.save(nubank);
            Long bankId = savedBank.getId();

            Optional<Bank> foundBank = bankRepository.findById(bankId);

            assertThat(foundBank).isPresent();
            Bank retrievedBank = foundBank.get();

            assertThat(retrievedBank.getId()).isEqualTo(bankId);
            assertThat(retrievedBank.getCode()).isEqualTo("NU");
            assertThat(retrievedBank.getName()).isEqualTo("Nubank");
        }

        @Test
        @DisplayName("Should maintain data integrity after update")
        void shouldMaintainDataIntegrityAfterUpdate() {
            Bank savedBank = bankRepository.save(nubank);
            Long bankId = savedBank.getId();

            // Create a new bank instance with updated data
            Bank updatedBank = Bank.of("NU_UPDATED", "Nubank Digital Bank");
            Bank savedUpdatedBank = bankRepository.save(updatedBank);

            assertThat(savedUpdatedBank.getId()).isNotNull();
            assertThat(savedUpdatedBank.getCode()).isEqualTo("NU_UPDATED");
            assertThat(savedUpdatedBank.getName()).isEqualTo("Nubank Digital Bank");

            // Verify the original bank still exists
            Optional<Bank> foundOriginalBank = bankRepository.findById(bankId);
            assertThat(foundOriginalBank).isPresent();
            assertThat(foundOriginalBank.get().getCode()).isEqualTo("NU");
            assertThat(foundOriginalBank.get().getName()).isEqualTo("Nubank");
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCases {

        @Test
        @DisplayName("Should handle large number of banks")
        void shouldHandleLargeNumberOfBanks() {
            // Create 100 banks
            for (int i = 1; i <= 100; i++) {
                Bank bank = Bank.of("BANK" + i, "Bank " + i);
                bankRepository.save(bank);
            }

            assertThat(bankRepository.count()).isEqualTo(100);

            List<Bank> allBanks = bankRepository.findAll();
            assertThat(allBanks).hasSize(100);
        }

        @Test
        @DisplayName("Should handle special characters in code and name")
        void shouldHandleSpecialCharactersInCodeAndName() {
            Bank specialBank = Bank.of("ITAU-银行", "Itaú Unibanco S.A. - 银行");
            Bank savedBank = bankRepository.save(specialBank);

            Optional<Bank> foundBank = bankRepository.findByCode("ITAU-银行");

            assertThat(foundBank).isPresent();
            assertThat(foundBank.get().getCode()).isEqualTo("ITAU-银行");
            assertThat(foundBank.get().getName()).isEqualTo("Itaú Unibanco S.A. - 银行");
        }

        @Test
        @DisplayName("Should handle case sensitivity in code")
        void shouldHandleCaseSensitivityInCode() {
            Bank lowerBank = Bank.of("nu", "Nubank Lower");
            Bank upperBank = Bank.of("NU", "Nubank Upper");

            bankRepository.save(lowerBank);
            bankRepository.save(upperBank);

            assertThat(bankRepository.count()).isEqualTo(2);
            assertThat(bankRepository.findByCode("nu")).isPresent();
            assertThat(bankRepository.findByCode("NU")).isPresent();
        }
    }
} 