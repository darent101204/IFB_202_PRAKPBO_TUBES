package com.example.project.service.impl;

import com.example.project.dto.WasteCategoryDTO;
import com.example.project.exception.BusinessException;
import com.example.project.exception.ResourceNotFoundException;
import com.example.project.model.WasteCategory;
import com.example.project.repository.WasteCategoryRepository;
import com.example.project.service.WasteCategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
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
        validate(dto);
        WasteCategory category = new WasteCategory();
        apply(dto, category);
        return wasteCategoryRepository.save(category);
    }

    @Override
    public WasteCategory update(Long id, WasteCategoryDTO dto) {
        validate(dto);
        WasteCategory category = findById(id);
        apply(dto, category);
        return wasteCategoryRepository.save(category);
    }

    @Override
    public void deleteById(Long id) {
        if (!wasteCategoryRepository.existsById(id)) {
            throw new ResourceNotFoundException("Kategori Limbah", id);
        }
        try {
            wasteCategoryRepository.deleteById(id);
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException("Kategori limbah tidak dapat dihapus karena masih digunakan oleh item request");
        }
    }

    private void apply(WasteCategoryDTO dto, WasteCategory category) {
        category.setName(dto.getName().trim());
        category.setDescription(dto.getDescription());
        category.setUnit(dto.getUnit().trim());
    }

    private void validate(WasteCategoryDTO dto) {
        if (dto.getName() == null || dto.getName().isBlank()) {
            throw new BusinessException("Nama kategori tidak boleh kosong");
        }
        if (dto.getUnit() == null || dto.getUnit().isBlank()) {
            throw new BusinessException("Satuan kategori tidak boleh kosong");
        }
    }
}
