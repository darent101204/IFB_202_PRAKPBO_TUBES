package com.example.project.repository;

import com.example.project.model.PickupRequest;
import com.example.project.model.RequestItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RequestItemRepository extends JpaRepository<RequestItem, Long> {
    List<RequestItem> findByPickupRequest(PickupRequest pickupRequest);
    List<RequestItem> findByPickupRequestId(Long requestId);
    void deleteByPickupRequestId(Long requestId);
}
