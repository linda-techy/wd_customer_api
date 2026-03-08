package com.wd.custapi.repository;

import com.wd.custapi.model.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByToken(String token);
    void deleteByToken(String token);
    void deleteByUser_Id(Long userId);

    /**
     * Bulk-delete all expired or revoked tokens.
     * Called nightly by AuthService.cleanupExpiredRefreshTokens().
     * Using a @Modifying JPQL DELETE avoids loading entities into heap (vs findAll + deleteAll).
     */
    @Modifying
    @Query("DELETE FROM RefreshToken t WHERE t.expiryDate < :now OR t.revoked = true")
    int deleteExpiredAndRevoked(@Param("now") LocalDateTime now);
}
