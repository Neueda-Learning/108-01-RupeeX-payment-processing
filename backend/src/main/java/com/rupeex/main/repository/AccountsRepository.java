package com.rupeex.main.repository;

import com.rupeex.main.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface AccountsRepository extends JpaRepository<Account, Long> {
    Optional<Account> findByAccountNumber(String accountNumber);
    List<Account> findAllByOrderByAccountHolderAsc();

    @Modifying
    @Query("UPDATE Account a SET a.balance = a.balance - :amount, a.updatedAt = CURRENT_TIMESTAMP WHERE a.accountNumber = :accountNumber AND a.balance >= :amount")
    int debitBalance(@Param("accountNumber") String accountNumber, @Param("amount") BigDecimal amount);

    @Modifying
    @Query("UPDATE Account a SET a.balance = a.balance + :amount, a.updatedAt = CURRENT_TIMESTAMP WHERE a.accountNumber = :accountNumber")
    int creditBalance(@Param("accountNumber") String accountNumber, @Param("amount") BigDecimal amount);
}
