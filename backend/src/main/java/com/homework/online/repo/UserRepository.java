package com.homework.online.repo;

import com.homework.online.domain.Role;
import com.homework.online.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);

    Optional<User> findByStudentNo(String studentNo);

    List<User> findByRole(Role role);
}
