package com.example.project.service;

import com.example.project.dto.WasteCategoryDTO;
import com.example.project.model.WasteCategory;

import java.util.List;

public interface WasteCategoryService {
    WasteCategory findById(Long id);
    List<WasteCategory> findAll();
    WasteCategory save(WasteCategoryDTO dto);
    WasteCategory update(Long id, WasteCategoryDTO dto);
    void deleteById(Long id);
}
