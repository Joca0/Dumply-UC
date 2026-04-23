package com.dumply.model;

import com.dumply.common.dto.Role;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "users",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"company_id", "document", "email"})
        }
)
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    private String email;

    private String fullName;

    private boolean firstLogin = true;

    private String document;

    private String password;

    private String secret2fa;

    private boolean is2faEnabled = false;

    private int failedLoginAttempts = 0;

    private LocalDateTime locktime;

    private String passwordResetToken;

    private LocalDateTime passwordResetExpiresAt;

    private boolean accountNonLocked = true;

    private String disable2faCode;

    @Enumerated(EnumType.STRING)
    private Role role;

    @ManyToOne
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

}

