package com.wd.custapi.service;

import com.wd.custapi.repository.PasswordResetTokenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
public class PasswordResetTokenCleanupJob {

    private static final Logger log = LoggerFactory.getLogger(PasswordResetTokenCleanupJob.class);

    private final PasswordResetTokenRepository passwordResetTokenRepository;

    public PasswordResetTokenCleanupJob(PasswordResetTokenRepository passwordResetTokenRepository) {
        this.passwordResetTokenRepository = passwordResetTokenRepository;
    }

    @Scheduled(fixedDelayString = "${app.password-reset.cleanup-interval-ms:900000}")
    @Transactional
    public void cleanupExpiredTokens() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(1);
        passwordResetTokenRepository.deleteByExpiresAtBefore(cutoff);
        log.debug("Password reset token cleanup executed for cutoff {}", cutoff);
    }
}
