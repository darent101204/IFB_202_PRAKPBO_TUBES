package com.example.project.service;

import com.example.project.dto.RequestItemDTO;
import com.example.project.model.RequestItem;

import java.util.List;

public interface RequestItemService {
    RequestItem findById(Long id);
    List<RequestItem> findByRequestId(Long requestId);
    RequestItem save(Long requestId, RequestItemDTO dto);
    RequestItem update(Long id, RequestItemDTO dto);
    void deleteById(Long id);
}
