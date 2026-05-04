package com.example.project.repository;

import com.example.project.model.PickupRequest;
import com.example.project.model.RequestStatus;
import com.example.project.model.Rt;
import com.example.project.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PickupRequestRepository extends JpaRepository<PickupRequest, Long> {

    List<PickupRequest> findByResident(User resident);

    List<PickupRequest> findByRt(Rt rt);

    List<PickupRequest> findByAssignedCollector(User collector);

    List<PickupRequest> findByStatus(RequestStatus status);

    List<PickupRequest> findByRtAndStatus(Rt rt, RequestStatus status);

    List<PickupRequest> findByAssignedCollectorAndStatusIn(User collector, List<RequestStatus> statuses);

    @Query("SELECT COUNT(r) FROM PickupRequest r WHERE r.assignedCollector = :collector " +
           "AND r.status IN ('SCHEDULED', 'ON_PROGRESS')")
    Long countActiveTasksByCollector(@Param("collector") User collector);

    @Query("SELECT r FROM PickupRequest r WHERE r.rt.region.id = :regionId ORDER BY r.createdAt DESC")
    List<PickupRequest> findByRegionId(@Param("regionId") Long regionId);

    List<PickupRequest> findAllByOrderByCreatedAtDesc();
}
