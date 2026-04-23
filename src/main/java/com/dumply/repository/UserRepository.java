package com.dumply.repository;

import com.dumply.common.dto.DriverAutocomplete;
import com.dumply.common.dto.Role;
import com.dumply.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);
    
    @Query("SELECT (COUNT(u) > 0) FROM User u WHERE u.email = :email")
    boolean existsByEmailGlobal(@Param("email") String email);

    /** Busca por email e company para garantir isolamento multi-tenant no login/token. */
    Optional<User> findByEmailAndCompanyId(String email, UUID companyId);

    Optional<User> findByPasswordResetToken(String token);

    List<User> findByCompanyIdAndRoleIn(UUID companyId, List<Role> roles);

    Optional<User> findByIdAndCompanyId(UUID id, UUID companyId);

    @Query("""
    SELECT new com.dumply.common.dto.DriverAutocomplete(u.id, u.fullName, u.document)
    FROM User u
    WHERE u.company.id = :companyId
      AND u.role = com.dumply.common.dto.Role.DRIVER
      AND (
           LOWER(u.fullName) LIKE LOWER(CONCAT('%', :q, '%'))
        OR LOWER(u.document) LIKE LOWER(CONCAT('%', :q, '%'))
      )
    ORDER BY u.fullName
    """)
    List<DriverAutocomplete> searchDriversForSelect(
            @Param("q") String q,
            @Param("companyId") UUID companyId
    );
}
