package com.example.project.service;

import com.example.project.dto.RegionDTO;
import com.example.project.model.Region;

import java.util.List;

public interface RegionService {
    Region findById(Long id);
    List<Region> findAll();
    Region save(RegionDTO dto);
    Region update(Long id, RegionDTO dto);
    void deleteById(Long id);
}
