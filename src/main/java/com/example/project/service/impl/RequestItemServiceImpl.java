package com.example.project.service.impl;

import com.example.project.dto.RequestItemDTO;
import com.example.project.exception.ResourceNotFoundException;
import com.example.project.model.PickupRequest;
import com.example.project.model.RequestItem;
import com.example.project.model.WasteCategory;
import com.example.project.repository.PickupRequestRepository;
import com.example.project.repository.RequestItemRepository;
import com.example.project.repository.WasteCategoryRepository;
import com.example.project.service.RequestItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class RequestItemServiceImpl implements RequestItemService {

    private final RequestItemRepository requestItemRepository;
    private final PickupRequestRepository pickupRequestRepository;
    private final WasteCategoryRepository wasteCategoryRepository;

    @Override
    public RequestItem findById(Long id) {
        return requestItemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Item Request", id));
    }

    @Override
    public List<RequestItem> findByRequestId(Long requestId) {
        return requestItemRepository.findByPickupRequestId(requestId);
    }

    @Override
    public RequestItem save(Long requestId, RequestItemDTO dto) {
        PickupRequest request = pickupRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("PickupRequest", requestId));
        WasteCategory category = wasteCategoryRepository.findById(dto.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Kategori Limbah", dto.getCategoryId()));

        RequestItem item = new RequestItem();
        item.setPickupRequest(request);
        item.setCategory(category);
        item.setQuantity(dto.getQuantity());
        item.setNotes(dto.getNotes());
        return requestItemRepository.save(item);
    }

    @Override
    public RequestItem update(Long id, RequestItemDTO dto) {
        RequestItem item = findById(id);
        WasteCategory category = wasteCategoryRepository.findById(dto.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Kategori Limbah", dto.getCategoryId()));
        item.setCategory(category);
        item.setQuantity(dto.getQuantity());
        item.setNotes(dto.getNotes());
        return requestItemRepository.save(item);
    }

    @Override
    public void deleteById(Long id) {
        if (!requestItemRepository.existsById(id)) {
            throw new ResourceNotFoundException("Item Request", id);
        }
        requestItemRepository.deleteById(id);
    }
}
