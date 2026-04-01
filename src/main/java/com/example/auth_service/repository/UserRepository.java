package com.example.auth_service.repository;

import com.example.auth_service.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    @Query("SELECT u FROM User u JOIN FETCH u.roles r JOIN FETCH u.tenant t WHERE u.email = :email")
    Optional<User> findByEmailWithRolesAndTenant(@Param("email") String email);

    Optional<User> findByEmail(String email);
}
