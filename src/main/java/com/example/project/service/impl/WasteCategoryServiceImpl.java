package com.example.project.service.impl;

import com.example.project.dto.WasteCategoryDTO;
import com.example.project.exception.ResourceNotFoundException;
import com.example.project.model.WasteCategory;
import com.example.project.repository.WasteCategoryRepository;
import com.example.project.service.WasteCategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class WasteCategoryServiceImpl implements WasteCategoryService {

    private final WasteCategoryRepository wasteCategoryRepository;

    @Override
    public WasteCategory findById(Long id) {
        return wasteCategoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Kategori Limbah", id));
    }

    @Override
    public List<WasteCategory> findAll() {
        return wasteCategoryRepository.findAll();
    }

    @Override
    public WasteCategory save(WasteCategoryDTO dto) {
        WasteCategory category = new WasteCategory();
        category.setName(dto.getName());
        category.setDescription(dto.getDescription());
        category.setUnit(dto.getUnit());
        return wasteCategoryRepository.save(category);
    }

    @Override
    public WasteCategory update(Long id, WasteCategoryDTO dto) {
        WasteCategory category = findById(id);
        category.setName(dto.getName());
        category.setDescription(dto.getDescription());
        category.setUnit(dto.getUnit());
        return wasteCategoryRepository.save(category);
    }

    @Override
    public void deleteById(Long id) {
        if (!wasteCategoryRepository.existsById(id)) {
            throw new ResourceNotFoundException("Kategori Limbah", id);
        }
        wasteCategoryRepository.deleteById(id);
    }
}
