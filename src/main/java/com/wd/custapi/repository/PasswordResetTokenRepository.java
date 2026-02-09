package com.wd.custapi.repository;

import com.wd.custapi.model.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {
    Optional<PasswordResetToken> findByEmailAndResetCodeAndUsedFalse(String email, String resetCode);
    void deleteByEmail(String email);
}
