package com.anujbohre.uqlab.auth;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * A registered user. Only a one-way BCrypt hash of the password is stored, never the password.
 * The table is called app_user because "user" is a reserved word in several databases.
 */
@Entity
@Table(name = "app_user")
public class AppUser {

    @Id
    @Column(length = 36)
    private String id;

    /** Stored in lower case, so "Anuj" and "anuj" are the same account. */
    @Column(nullable = false, unique = true, length = 30)
    private String username;

    @Column(nullable = false, length = 60)
    private String passwordHash;

    @Column(nullable = false)
    private Instant createdAt;

    /** Required by JPA. */
    protected AppUser() {}

    public AppUser(String id, String username, String passwordHash, Instant createdAt) {
        this.id = id;
        this.username = username;
        this.passwordHash = passwordHash;
        this.createdAt = createdAt;
    }

    public String getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
