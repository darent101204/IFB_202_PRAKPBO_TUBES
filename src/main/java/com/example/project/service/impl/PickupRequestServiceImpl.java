package com.example.project.service.impl;

import com.example.project.dto.PickupRequestDTO;
import com.example.project.dto.RequestItemDTO;
import com.example.project.exception.BusinessException;
import com.example.project.exception.ResourceNotFoundException;
import com.example.project.model.*;
import com.example.project.repository.*;
import com.example.project.service.PickupRequestService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class PickupRequestServiceImpl implements PickupRequestService {

    private final PickupRequestRepository pickupRequestRepository;
    private final RequestItemRepository requestItemRepository;
    private final UserRepository userRepository;
    private final RtRepository rtRepository;
    private final RegionRepository regionRepository;
    private final WasteCategoryRepository wasteCategoryRepository;

    // ─── READ ────────────────────────────────────────────────────────────────

    @Override
    public PickupRequest findById(Long id) {
        return pickupRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pickup Request", id));
    }

    @Override
    public List<PickupRequest> findAll() {
        return pickupRequestRepository.findAllByOrderByCreatedAtDesc();
    }

    @Override
    public List<PickupRequest> findByResident(User resident) {
        return pickupRequestRepository.findByResident(resident);
    }

    @Override
    public List<PickupRequest> findByCollector(User collector) {
        return pickupRequestRepository.findByAssignedCollector(collector);
    }

    @Override
    public List<PickupRequest> findByRtUser(User rtUser) {
        if (rtUser.getRt() == null) {
            throw new BusinessException("User RT tidak memiliki data RT yang terhubung");
        }
        return pickupRequestRepository.findByRt(rtUser.getRt());
    }

    @Override
    public List<PickupRequest> findPendingApprovalByRtUser(User rtUser) {
        if (rtUser.getRt() == null) {
            throw new BusinessException("User RT tidak memiliki data RT yang terhubung");
        }
        return pickupRequestRepository.findByRtAndStatus(rtUser.getRt(), RequestStatus.PENDING_APPROVAL);
    }

    // ─── CREATE ──────────────────────────────────────────────────────────────

    @Override
    public PickupRequest createRequest(PickupRequestDTO dto, User resident) {
        try {
            Rt rt = rtRepository.findById(dto.getRtId())
                    .orElseThrow(() -> new ResourceNotFoundException("RT", dto.getRtId()));

            Region region = rt.getRegion();

            PickupRequest request = new PickupRequest();
            request.setResident(resident);
            request.setCreatedBy(resident);
            request.setRt(rt);
            request.setRegion(region);
            request.setStatus(RequestStatus.PENDING_APPROVAL);
            request.setIsCollective(false);
            request.setNotes(dto.getNotes());
            request.setScheduledDate(dto.getScheduledDate());

            PickupRequest saved = pickupRequestRepository.save(request);

            // Save items if provided
            if (dto.getItems() != null && !dto.getItems().isEmpty()) {
                for (RequestItemDTO itemDTO : dto.getItems()) {
                    if (itemDTO.getCategoryId() != null && itemDTO.getQuantity() != null) {
                        WasteCategory category = wasteCategoryRepository.findById(itemDTO.getCategoryId())
                                .orElseThrow(() -> new ResourceNotFoundException("Kategori Limbah", itemDTO.getCategoryId()));
                        RequestItem item = new RequestItem();
                        item.setPickupRequest(saved);
                        item.setCategory(category);
                        item.setQuantity(itemDTO.getQuantity());
                        item.setNotes(itemDTO.getNotes());
                        requestItemRepository.save(item);
                    }
                }
            }

            log.info("Request baru dibuat oleh resident {} dengan status PENDING_APPROVAL", resident.getName());
            return saved;
        } catch (Exception e) {
            log.error("Gagal membuat request: {}", e.getMessage());
            if (e instanceof ResourceNotFoundException || e instanceof BusinessException) throw e;
            throw new BusinessException("Gagal membuat request penjemputan: " + e.getMessage());
        }
    }

    // ─── APPROVE / REJECT ────────────────────────────────────────────────────

    @Override
    public PickupRequest approveRequest(Long id, User rtUser) {
        try {
            PickupRequest request = findById(id);

            if (request.getStatus() != RequestStatus.PENDING_APPROVAL) {
                throw new BusinessException("Request tidak dalam status PENDING_APPROVAL, tidak bisa di-approve");
            }

            // Validate RT ownership
            if (rtUser.getRt() != null && !rtUser.getRt().getId().equals(request.getRt().getId())) {
                throw new BusinessException("Anda tidak berwenang untuk meng-approve request ini");
            }

            request.setStatus(RequestStatus.PENDING);
            PickupRequest approved = pickupRequestRepository.save(request);

            log.info("Request {} di-approve oleh RT {}", id, rtUser.getName());

            // Trigger auto assign setelah approval
            return autoAssignCollector(approved.getId());
        } catch (Exception e) {
            log.error("Gagal meng-approve request {}: {}", id, e.getMessage());
            if (e instanceof ResourceNotFoundException || e instanceof BusinessException) throw e;
            throw new BusinessException("Gagal menyetujui request: " + e.getMessage());
        }
    }

    @Override
    public PickupRequest rejectRequest(Long id, User rtUser) {
        try {
            PickupRequest request = findById(id);

            if (request.getStatus() != RequestStatus.PENDING_APPROVAL) {
                throw new BusinessException("Request tidak dalam status PENDING_APPROVAL, tidak bisa di-reject");
            }

            if (rtUser.getRt() != null && !rtUser.getRt().getId().equals(request.getRt().getId())) {
                throw new BusinessException("Anda tidak berwenang untuk meng-reject request ini");
            }

            request.setStatus(RequestStatus.REJECTED);
            log.info("Request {} di-reject oleh RT {}", id, rtUser.getName());
            return pickupRequestRepository.save(request);
        } catch (Exception e) {
            log.error("Gagal me-reject request {}: {}", id, e.getMessage());
            if (e instanceof ResourceNotFoundException || e instanceof BusinessException) throw e;
            throw new BusinessException("Gagal menolak request: " + e.getMessage());
        }
    }

    // ─── AUTO ASSIGN ─────────────────────────────────────────────────────────

    @Override
    public PickupRequest autoAssignCollector(Long requestId) {
        PickupRequest request = findById(requestId);

        if (request.getStatus() != RequestStatus.PENDING) {
            throw new BusinessException("Request harus berstatus PENDING untuk bisa di-assign");
        }

        Region region = request.getRegion();

        // Cari collector dengan workload paling sedikit di region yang sama
        List<User> availableCollectors = userRepository.findCollectorsByRegionOrderByWorkload(region);

        if (availableCollectors.isEmpty()) {
            log.warn("Tidak ada collector tersedia di region {} untuk request {}", region.getName(), requestId);
            // Tetap PENDING jika tidak ada collector
            return request;
        }

        User assignedCollector = availableCollectors.get(0);
        request.setAssignedCollector(assignedCollector);
        request.setStatus(RequestStatus.SCHEDULED);

        log.info("Request {} di-assign ke collector {} (workload terendah di region {})",
                requestId, assignedCollector.getName(), region.getName());

        return pickupRequestRepository.save(request);
    }

    // ─── UPDATE STATUS (Collector) ────────────────────────────────────────────

    @Override
    public PickupRequest updateStatus(Long id, RequestStatus newStatus, User user) {
        try {
            PickupRequest request = findById(id);

            // Validasi transisi status
            switch (newStatus) {
                case ON_PROGRESS -> {
                    if (request.getStatus() != RequestStatus.SCHEDULED) {
                        throw new BusinessException("Hanya request berstatus SCHEDULED yang bisa dimulai");
                    }
                    if (request.getAssignedCollector() == null || !request.getAssignedCollector().getId().equals(user.getId())) {
                        throw new BusinessException("Anda bukan collector yang ditugaskan untuk request ini");
                    }
                }
                case COMPLETED -> {
                    if (request.getStatus() != RequestStatus.ON_PROGRESS) {
                        throw new BusinessException("Hanya request berstatus ON_PROGRESS yang bisa diselesaikan");
                    }
                    if (request.getAssignedCollector() == null || !request.getAssignedCollector().getId().equals(user.getId())) {
                        throw new BusinessException("Anda bukan collector yang ditugaskan untuk request ini");
                    }
                }
                default -> throw new BusinessException("Transisi status tidak valid: " + newStatus);
            }

            request.setStatus(newStatus);
            log.info("Status request {} diupdate ke {} oleh {}", id, newStatus, user.getName());
            return pickupRequestRepository.save(request);
        } catch (Exception e) {
            log.error("Gagal update status request {}: {}", id, e.getMessage());
            if (e instanceof ResourceNotFoundException || e instanceof BusinessException) throw e;
            throw new BusinessException("Gagal mengubah status request: " + e.getMessage());
        }
    }

    // ─── COLLECTIVE ──────────────────────────────────────────────────────────

    @Override
    public PickupRequest makeCollective(Long masterId, List<Long> childIds, User rtUser) {
        PickupRequest master = findById(masterId);

        if (master.getStatus() != RequestStatus.PENDING_APPROVAL) {
            throw new BusinessException("Hanya request PENDING_APPROVAL yang bisa digabungkan");
        }

        master.setIsCollective(true);
        pickupRequestRepository.save(master);

        // Mark children as collective and link to master
        for (Long childId : childIds) {
            if (!childId.equals(masterId)) {
                PickupRequest child = findById(childId);
                child.setIsCollective(true);
                child.setStatus(RequestStatus.REJECTED); // child digabung ke master
                child.setNotes("Digabungkan ke request #" + masterId);
                pickupRequestRepository.save(child);
            }
        }

        log.info("Request {} dijadikan kolektif oleh RT {}", masterId, rtUser.getName());
        return master;
    }

    // ─── DELETE ──────────────────────────────────────────────────────────────

    @Override
    public void deleteById(Long id) {
        if (!pickupRequestRepository.existsById(id)) {
            throw new ResourceNotFoundException("Pickup Request", id);
        }
        requestItemRepository.deleteByPickupRequestId(id);
        pickupRequestRepository.deleteById(id);
    }
}
