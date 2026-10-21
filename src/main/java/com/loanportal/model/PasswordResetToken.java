package com.loanportal.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;

/** Single-use, time-bound password reset token. Only the SHA-256 hash of the token is stored. */
@Entity
@Table(name = "password_reset_token")
public class PasswordResetToken extends LoanPortalEntity {

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false)
    public User user;

    @Column(name = "token_hash", nullable = false, unique = true)
    public String tokenHash;

    @Column(name = "expires_at", nullable = false)
    public Instant expiresAt;

    @Column(nullable = false)
    public boolean used = false;

    @Column(name = "created_at", nullable = false)
    public Instant createdAt = Instant.now();
}
