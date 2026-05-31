package com.courtier.courtier.otp.repository;

import com.courtier.courtier.otp.entity.OtpToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

public interface OtpTokenRepository extends JpaRepository<OtpToken, Long> {
    Optional<OtpToken> findByEmailAndPurpose(String email, OtpToken.OtpPurpose purpose);
    void deleteByEmailAndPurpose(String email, OtpToken.OtpPurpose purpose);
    void deleteAllByEmail(String email);

    @Modifying
    @Transactional
    @Query("""
        delete from OtpToken o
        where o.expiresAt < CURRENT_TIMESTAMP
    """)
    long deleteExpired();
}