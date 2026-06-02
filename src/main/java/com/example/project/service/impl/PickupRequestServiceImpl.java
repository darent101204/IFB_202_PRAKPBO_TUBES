package com.example.project.service.impl;

import com.example.project.dto.PickupRequestDTO;
import com.example.project.dto.RequestItemDTO;
import com.example.project.exception.BusinessException;
import com.example.project.exception.ResourceNotFoundException;
import com.example.project.model.PickupRequest;
import com.example.project.model.Region;
import com.example.project.model.RequestItem;
import com.example.project.model.RequestStatus;
import com.example.project.model.Role;
import com.example.project.model.Rt;
import com.example.project.model.User;
import com.example.project.model.WasteCategory;
import com.example.project.repository.PickupRequestRepository;
import com.example.project.repository.RequestItemRepository;
import com.example.project.repository.RtRepository;
import com.example.project.repository.UserRepository;
import com.example.project.repository.WasteCategoryRepository;
import com.example.project.service.PickupRequestService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class PickupRequestServiceImpl implements PickupRequestService {

    private final PickupRequestRepository pickupRequestRepository;
    private final RequestItemRepository requestItemRepository;
    private final UserRepository userRepository;
    private final RtRepository rtRepository;
    private final WasteCategoryRepository wasteCategoryRepository;

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
        ensureRtUserHasRt(rtUser);
        return pickupRequestRepository.findByRt(rtUser.getRt());
    }

    @Override
    public List<PickupRequest> findPendingApprovalByRtUser(User rtUser) {
        ensureRtUserHasRt(rtUser);
        return pickupRequestRepository.findByRtAndStatus(rtUser.getRt(), RequestStatus.PENDING_APPROVAL);
    }

    @Override
    public PickupRequest createRequest(PickupRequestDTO dto, User resident) {
        try {
            validateResidentRequest(dto, resident);

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

            if (dto.getPhoto() != null && !dto.getPhoto().isEmpty()) {
                try {
                    String originalFilename = dto.getPhoto().getOriginalFilename();
                    String extension = "";
                    if (originalFilename != null && originalFilename.contains(".")) {
                        extension = originalFilename.substring(originalFilename.lastIndexOf("."));
                    }
                    String fileName = UUID.randomUUID().toString() + extension;
                    
                    Path uploadPath = Paths.get("uploads");
                    if (!Files.exists(uploadPath)) {
                        Files.createDirectories(uploadPath);
                    }
                    
                    Path filePath = uploadPath.resolve(fileName);
                    Files.copy(dto.getPhoto().getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
                    
                    request.setPhotoPath(fileName);
                } catch (Exception e) {
                    log.error("Gagal mengupload foto: {}", e.getMessage());
                    throw new BusinessException("Gagal menyimpan foto sampah: " + e.getMessage());
                }
            }

            PickupRequest saved = pickupRequestRepository.save(request);
            saveItems(dto, saved);

            log.info("Request baru dibuat oleh resident {} dengan status PENDING_APPROVAL", resident.getName());
            return saved;
        } catch (Exception e) {
            log.error("Gagal membuat request: {}", e.getMessage());
            if (e instanceof ResourceNotFoundException || e instanceof BusinessException) throw e;
            throw new BusinessException("Gagal membuat request penjemputan: " + e.getMessage());
        }
    }

    @Override
    public PickupRequest approveRequest(Long id, User rtUser) {
        try {
            PickupRequest request = findById(id);
            validateRtCanManageRequest(rtUser, request);

            if (request.getStatus() != RequestStatus.PENDING_APPROVAL) {
                throw new BusinessException("Request tidak dalam status PENDING_APPROVAL, tidak bisa di-approve");
            }

            request.setStatus(RequestStatus.PENDING);
            PickupRequest approved = pickupRequestRepository.save(request);

            log.info("Request {} di-approve oleh RT {}", id, rtUser.getName());
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
            validateRtCanManageRequest(rtUser, request);

            if (request.getStatus() != RequestStatus.PENDING_APPROVAL) {
                throw new BusinessException("Request tidak dalam status PENDING_APPROVAL, tidak bisa di-reject");
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

    @Override
    public PickupRequest autoAssignCollector(Long requestId) {
        PickupRequest request = findById(requestId);

        if (request.getStatus() != RequestStatus.PENDING) {
            throw new BusinessException("Request harus berstatus PENDING untuk bisa di-assign");
        }

        Region region = request.getRegion();
        List<User> availableCollectors = userRepository.findCollectorsByRegionOrderByWorkload(region);

        if (availableCollectors.isEmpty()) {
            log.warn("Tidak ada collector tersedia di region {} untuk request {}", region.getName(), requestId);
            return request;
        }

        User assignedCollector = availableCollectors.get(0);
        request.setAssignedCollector(assignedCollector);
        request.setStatus(RequestStatus.SCHEDULED);

        log.info("Request {} di-assign ke collector {} (workload terendah di region {})",
                requestId, assignedCollector.getName(), region.getName());

        return pickupRequestRepository.save(request);
    }

    @Override
    public PickupRequest updateStatus(Long id, RequestStatus newStatus, User user) {
        try {
            if (user == null || user.getRole() != Role.COLLECTOR) {
                throw new BusinessException("Hanya collector yang dapat mengubah status request");
            }

            PickupRequest request = findById(id);
            validateAssignedCollector(request, user);

            switch (newStatus) {
                case ON_PROGRESS -> {
                    if (request.getStatus() != RequestStatus.SCHEDULED) {
                        throw new BusinessException("Hanya request berstatus SCHEDULED yang bisa dimulai");
                    }
                }
                case COMPLETED -> {
                    if (request.getStatus() != RequestStatus.ON_PROGRESS) {
                        throw new BusinessException("Hanya request berstatus ON_PROGRESS yang bisa diselesaikan");
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
        validateRtCanManageRequest(rtUser, master);

        if (master.getStatus() != RequestStatus.PENDING_APPROVAL) {
            throw new BusinessException("Hanya request PENDING_APPROVAL yang bisa digabungkan");
        }

        master.setIsCollective(true);
        pickupRequestRepository.save(master);

        if (childIds != null) {
            for (Long childId : childIds) {
                if (childId == null || childId.equals(masterId)) {
                    continue;
                }

                PickupRequest child = findById(childId);
                validateRtCanManageRequest(rtUser, child);
                if (child.getStatus() != RequestStatus.PENDING_APPROVAL) {
                    throw new BusinessException("Semua request gabungan harus berstatus PENDING_APPROVAL");
                }

                child.setIsCollective(true);
                child.setStatus(RequestStatus.REJECTED);
                child.setNotes("Digabungkan ke request #" + masterId);
                pickupRequestRepository.save(child);
            }
        }

        log.info("Request {} dijadikan kolektif oleh RT {}", masterId, rtUser.getName());
        return master;
    }

    @Override
    public void deleteById(Long id) {
        if (!pickupRequestRepository.existsById(id)) {
            throw new ResourceNotFoundException("Pickup Request", id);
        }
        requestItemRepository.deleteByPickupRequestId(id);
        pickupRequestRepository.deleteById(id);
    }

    @Override
    public void deleteRequest(Long id, User user) {
        if (user == null || user.getRole() != Role.RESIDENT) {
            throw new BusinessException("Hanya resident yang dapat menghapus request");
        }

        PickupRequest request = findById(id);

        if (request.getResident() == null || !request.getResident().getId().equals(user.getId())) {
            throw new BusinessException("Anda tidak berhak menghapus request ini");
        }

        if (request.getStatus() != RequestStatus.PENDING_APPROVAL
                && request.getStatus() != RequestStatus.REJECTED) {
            throw new BusinessException(
                    "Hanya request dengan status PENDING_APPROVAL atau REJECTED yang dapat dihapus");
        }

        log.info("Request {} dihapus oleh resident {}", id, user.getName());
        requestItemRepository.deleteByPickupRequestId(id);
        pickupRequestRepository.deleteById(id);
    }

    @Override
    public PickupRequest updateRequest(Long id, PickupRequestDTO dto, User user) {

        PickupRequest request = findById(id);

        if (user == null) {
            throw new BusinessException("User tidak valid");
        }

        // hanya resident pemilik request yang boleh edit
        if (user.getRole() == Role.RESIDENT) {

            if (request.getResident() == null ||
                !request.getResident().getId().equals(user.getId())) {
                throw new BusinessException("Anda tidak berhak mengubah request ini");
            }

            if (request.getStatus() != RequestStatus.PENDING_APPROVAL) {
                throw new BusinessException(
                        "Request yang sudah diproses tidak dapat diubah");
            }
        }

        // Safeguard 1: Validate items exists
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new BusinessException("Data item request tidak ditemukan");
        }

        if (dto.getScheduledDate() != null) {
            request.setScheduledDate(dto.getScheduledDate());
        }

        request.setNotes(dto.getNotes());

        // Update RequestItem
        if (dto.getItems() != null && !dto.getItems().isEmpty()) {
            RequestItemDTO itemDTO = dto.getItems().get(0);
            if (itemDTO.getCategoryId() != null && itemDTO.getQuantity() != null) {
                if (itemDTO.getQuantity() <= 0) {
                    throw new BusinessException("Item request harus memiliki jumlah lebih dari 0");
                }
                
                WasteCategory category = wasteCategoryRepository.findById(itemDTO.getCategoryId())
                        .orElseThrow(() -> new ResourceNotFoundException("Kategori Limbah", itemDTO.getCategoryId()));
                
                RequestItem item = request.getItems().get(0);
                item.setCategory(category);
                item.setQuantity(itemDTO.getQuantity());
                item.setNotes(dto.getNotes()); // Sync notes to item if needed
                requestItemRepository.save(item);
            } else {
                throw new BusinessException("Kategori dan jumlah item harus diisi");
            }
        } else {
            throw new BusinessException("Kategori dan jumlah item harus diisi");
        }

        // Photo Replacement
        if (dto.getPhoto() != null && !dto.getPhoto().isEmpty()) {
            try {
                String originalFilename = dto.getPhoto().getOriginalFilename();
                String extension = "";
                if (originalFilename != null && originalFilename.contains(".")) {
                    extension = originalFilename.substring(originalFilename.lastIndexOf("."));
                }
                String fileName = UUID.randomUUID().toString() + extension;
                
                Path uploadPath = Paths.get("uploads");
                if (!Files.exists(uploadPath)) {
                    Files.createDirectories(uploadPath);
                }
                
                Path filePath = uploadPath.resolve(fileName);
                Files.copy(dto.getPhoto().getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
                
                request.setPhotoPath(fileName);
            } catch (Exception e) {
                log.error("Gagal mengupload foto baru: {}", e.getMessage());
                throw new BusinessException("Gagal menyimpan foto sampah: " + e.getMessage());
            }
        }

        return pickupRequestRepository.save(request);
    }

    private void validateResidentRequest(PickupRequestDTO dto, User resident) {
        if (resident == null || resident.getRole() != Role.RESIDENT) {
            throw new BusinessException("Hanya resident yang dapat membuat request penjemputan");
        }
        if (dto.getRtId() == null) {
            throw new BusinessException("RT tujuan harus dipilih");
        }
        if (resident.getRt() == null) {
            throw new BusinessException("Resident belum terhubung dengan data RT");
        }
        if (!resident.getRt().getId().equals(dto.getRtId())) {
            throw new BusinessException("Resident hanya dapat membuat request untuk RT sendiri");
        }
        if (dto.getScheduledDate() != null && dto.getScheduledDate().isBefore(LocalDate.now())) {
            throw new BusinessException("Tanggal penjemputan tidak boleh sebelum hari ini");
        }
    }

    private void saveItems(PickupRequestDTO dto, PickupRequest saved) {
        if (dto.getItems() == null || dto.getItems().isEmpty()) {
            return;
        }

        for (RequestItemDTO itemDTO : dto.getItems()) {
            if (itemDTO.getCategoryId() == null && itemDTO.getQuantity() == null) {
                continue;
            }
            if (itemDTO.getCategoryId() == null || itemDTO.getQuantity() == null || itemDTO.getQuantity() <= 0) {
                throw new BusinessException("Item request harus memiliki kategori dan jumlah lebih dari 0");
            }

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

    private void ensureRtUserHasRt(User rtUser) {
        if (rtUser == null || rtUser.getRole() != Role.RT || rtUser.getRt() == null) {
            throw new BusinessException("User RT tidak memiliki data RT yang terhubung");
        }
    }

    private void validateRtCanManageRequest(User rtUser, PickupRequest request) {
        ensureRtUserHasRt(rtUser);
        if (request.getRt() == null || !rtUser.getRt().getId().equals(request.getRt().getId())) {
            throw new BusinessException("Anda tidak berwenang mengelola request ini");
        }
    }

    private void validateAssignedCollector(PickupRequest request, User user) {
        if (request.getAssignedCollector() == null || !request.getAssignedCollector().getId().equals(user.getId())) {
            throw new BusinessException("Anda bukan collector yang ditugaskan untuk request ini");
        }
    }
}
