package com.example.project.service;

import com.example.project.dto.PickupRequestDTO;
import com.example.project.model.PickupRequest;
import com.example.project.model.RequestStatus;
import com.example.project.model.User;

import java.util.List;

public interface PickupRequestService {
    PickupRequest findById(Long id);
    List<PickupRequest> findAll();
    List<PickupRequest> findByResident(User resident);
    List<PickupRequest> findByCollector(User collector);
    List<PickupRequest> findByRtUser(User rtUser);
    List<PickupRequest> findPendingApprovalByRtUser(User rtUser);

    PickupRequest createRequest(PickupRequestDTO dto, User resident);
    PickupRequest approveRequest(Long id, User rtUser);
    PickupRequest rejectRequest(Long id, User rtUser);
    PickupRequest autoAssignCollector(Long requestId);
    PickupRequest updateStatus(Long id, RequestStatus newStatus, User user);
    PickupRequest makeCollective(Long masterId, List<Long> childIds, User rtUser);
    PickupRequest updateRequest(Long id, PickupRequestDTO dto, User user);
    void deleteById(Long id);
}
