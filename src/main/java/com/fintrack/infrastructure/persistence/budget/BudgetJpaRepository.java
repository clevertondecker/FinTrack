package com.fintrack.infrastructure.persistence.budget;

import com.fintrack.domain.budget.Budget;
import com.fintrack.domain.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

@Repository
public interface BudgetJpaRepository extends JpaRepository<Budget, Long> {

    Optional<Budget> findByIdAndOwner(Long id, User owner);

    @Query("SELECT b FROM Budget b WHERE b.owner = :owner AND b.active = true "
            + "AND (b.month = :month OR b.month IS NULL)")
    List<Budget> findActiveByOwnerAndMonth(
            @Param("owner") User owner,
            @Param("month") YearMonth month);

    List<Budget> findByOwnerAndActiveTrue(User owner);
}
