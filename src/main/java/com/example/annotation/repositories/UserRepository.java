package com.example.annotation.repositories;

import com.example.annotation.entities.Role;
import com.example.annotation.entities.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsernameAndActiveTrue(String username);

    Optional<User> findByUsername(String username); // ✅ Needed for security + login

    List<User> findByRoleAndActiveTrue(Role role);

    List<User> findAllByActiveTrue();
    List<User> findByRole(Role role); // ✅ Add this

    Page<User> findByActive(boolean active, Pageable pageable); // ✅ for pagination

    Page<User> findByUsernameContainingIgnoreCase(String keyword, Pageable pageable);

    Page<User> findByUsernameContainingIgnoreCaseAndActive(String keyword, boolean active, Pageable pageable);
}
