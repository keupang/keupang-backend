package com.example.keupanguser.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@NoArgsConstructor
@Getter
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    Long userId;

    @Column(nullable = false, unique = true, name = "user_email")
    String userEmail;
    @Column(name = "user_password")
    String userPassword;
    @Column(name = "user_name")
    String userName;
    @Column(name = "user_phone")
    String userPhone;
    @Column(name = "role")
    Role role;

    @Builder
    public User(String userEmail, String userPassword, String userName,
        String userPhone,
        Role role) {
        this.userEmail = userEmail;
        this.userPassword = userPassword;
        this.userName = userName;
        this.userPhone = userPhone;
        this.role = role;
    }
}
