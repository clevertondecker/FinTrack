package com.fintrack.infrastructure.persistence.contact;

import com.fintrack.domain.contact.TrustedContact;
import com.fintrack.domain.user.User;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TrustedContactJpaRepository extends JpaRepository<TrustedContact, Long> {

    List<TrustedContact> findByOwnerOrderByNameAsc(User owner);

    @Query("SELECT tc FROM TrustedContact tc WHERE tc.owner = :owner "
           + "AND (LOWER(tc.name) LIKE LOWER(CONCAT('%', :search, '%')) "
           + "OR LOWER(tc.email) LIKE LOWER(CONCAT('%', :search, '%'))) ORDER BY tc.name")
    List<TrustedContact> findByOwnerAndSearch(@Param("owner") User owner, @Param("search") String search);

    Optional<TrustedContact> findByIdAndOwner(Long id, User owner);

    Optional<TrustedContact> findByOwnerAndEmail(User owner, String email);

    boolean existsByOwnerAndEmail(User owner, String email);
}
