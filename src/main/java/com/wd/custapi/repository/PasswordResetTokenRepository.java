package com.wd.custapi.repository;

import com.wd.custapi.model.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {
    Optional<PasswordResetToken> findByEmailAndResetCodeAndUsedFalse(String email, String resetCode);
    void deleteByEmail(String email);
    void deleteByExpiresAtBefore(LocalDateTime cutoff);

    @Modifying
    @Query("update PasswordResetToken t set t.used = true where t.id = :id and t.used = false")
    int markTokenUsedIfUnused(@Param("id") Long id);
}
