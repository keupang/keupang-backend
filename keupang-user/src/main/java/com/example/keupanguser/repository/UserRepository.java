package com.example.keupanguser.repository;

import com.example.keupanguser.domain.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
    boolean existsByUserEmail(String userEmail);
    Optional<User> findByUserEmail(String userEmail);
}
