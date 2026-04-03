package com.epam.finaltask.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import com.epam.finaltask.model.enums.Role;
import com.epam.finaltask.model.User;

public interface UserRepository extends JpaRepository<User, UUID>, JpaSpecificationExecutor<User> {

    boolean existsByUsername(String username);

    Optional<User> findUserByUsername(String username);

    Optional<User> findByEmail(String email);

    Page<User> findAllByRole(Role role, Pageable pageable);

    Page<User> findAllByActive(boolean active, Pageable pageable);

    List<User> findAllByActive(boolean active);

    @Query("SELECT COUNT(u) FROM User u")
    long countAllUsers();

    @Query("SELECT COUNT(u) FROM User u WHERE u.active = true")
    long countActiveUsers();
}
