package com.example.project.repository;

import com.example.project.model.Region;
import com.example.project.model.Role;
import com.example.project.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    List<User> findByRole(Role role);

    List<User> findByRoleAndRegion(Role role, Region region);

    List<User> findByRoleAndRegionId(Role role, Long regionId);

    boolean existsByEmail(String email);

    @Query("SELECT u FROM User u WHERE u.role = 'COLLECTOR' AND u.region = :region AND u.isActive = true " +
           "ORDER BY (SELECT COUNT(r) FROM PickupRequest r WHERE r.assignedCollector = u " +
           "AND r.status IN ('SCHEDULED', 'ON_PROGRESS')) ASC")
    List<User> findCollectorsByRegionOrderByWorkload(@Param("region") Region region);
}
