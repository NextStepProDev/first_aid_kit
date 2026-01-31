package com.firstaidkit.infrastructure.database.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.Length;

import java.time.OffsetDateTime;
import java.util.Set;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "app_user")
public class UserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Integer userId;

    @Column(name = "user_name")
    @Length(min = 5, max = 36)
    private String userName;

    @Column(name = "email")
    @Email
    private String email;

    @Column(name = "password")
    private String password;

    @Column(name = "name")
    private String name;

    @Column(name = "active")
    private Boolean active;

    @ManyToMany(cascade = CascadeType.MERGE, fetch = FetchType.EAGER)
    @JoinTable(
            name = "app_user_role",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<RoleEntity> role;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @Column(name = "last_login")
    private OffsetDateTime lastLogin;

    @Column(name = "failed_login_attempts")
    @Builder.Default
    private Integer failedLoginAttempts = 0;

    @Column(name = "locked_until")
    private OffsetDateTime lockedUntil;

    @Column(name = "alerts_enabled")
    @Builder.Default
    private Boolean alertsEnabled = true;

    public boolean isAccountLocked() {
        return lockedUntil != null && lockedUntil.isAfter(OffsetDateTime.now());
    }

    public void incrementFailedAttempts() {
        this.failedLoginAttempts = (this.failedLoginAttempts == null ? 0 : this.failedLoginAttempts) + 1;
    }

    public void resetFailedAttempts() {
        this.failedLoginAttempts = 0;
        this.lockedUntil = null;
    }
}
