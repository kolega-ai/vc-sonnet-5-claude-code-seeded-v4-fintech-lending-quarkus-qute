package com.loanportal.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * Platform user / login account. Password is stored as a BCrypt hash only;
 * quarkus-security-jpa reads {@link #password} and {@link #role} to authenticate.
 */
@Entity
@Table(name = "app_user")
public class User extends LoanPortalEntity {

    @Column(nullable = false, unique = true)
    public String email;

    /** BCrypt hash, never plaintext. */
    @Column(name = "password_hash", nullable = false)
    public String password;

    @Column(name = "full_name", nullable = false)
    public String fullName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    public Role role;

    @Column(nullable = false)
    public boolean active = true;

    @Column(name = "created_at", nullable = false)
    public Instant createdAt = Instant.now();

    public static User findByEmail(String email) {
        return find("lower(email) = ?1", email == null ? "" : email.toLowerCase()).firstResult();
    }
}
