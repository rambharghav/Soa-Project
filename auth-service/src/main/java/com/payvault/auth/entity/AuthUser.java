package com.payvault.auth.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "auth_users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AuthUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String role;

    public AuthUser(String username, String password, String role) {
        this.username = username;
        this.password = password;
        this.role = role;
    }
}