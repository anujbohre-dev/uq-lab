package com.anujbohre.uqlab.auth;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Spring Data generates the queries from the method names. */
public interface UserRepository extends JpaRepository<AppUser, String> {

    Optional<AppUser> findByUsername(String username);

    boolean existsByUsername(String username);
}
