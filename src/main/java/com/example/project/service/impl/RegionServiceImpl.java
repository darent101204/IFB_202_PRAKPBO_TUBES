package com.example.project.service.impl;

import com.example.project.dto.RegionDTO;
import com.example.project.exception.BusinessException;
import com.example.project.exception.ResourceNotFoundException;
import com.example.project.model.Region;
import com.example.project.repository.RegionRepository;
import com.example.project.service.RegionService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class RegionServiceImpl implements RegionService {

    private final RegionRepository regionRepository;

    @Override
    public Region findById(Long id) {
        return regionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Region", id));
    }

    @Override
    public List<Region> findAll() {
        return regionRepository.findAll();
    }

    @Override
    public Region save(RegionDTO dto) {
        validate(dto);
        Region region = new Region();
        region.setName(dto.getName().trim());
        region.setDescription(dto.getDescription());
        return regionRepository.save(region);
    }

    @Override
    public Region update(Long id, RegionDTO dto) {
        validate(dto);
        Region region = findById(id);
        region.setName(dto.getName().trim());
        region.setDescription(dto.getDescription());
        return regionRepository.save(region);
    }

    @Override
    public void deleteById(Long id) {
        if (!regionRepository.existsById(id)) {
            throw new ResourceNotFoundException("Region", id);
        }
        try {
            regionRepository.deleteById(id);
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException("Region tidak dapat dihapus karena masih digunakan oleh RT, user, atau request");
        }
    }

    private void validate(RegionDTO dto) {
        if (dto.getName() == null || dto.getName().isBlank()) {
            throw new BusinessException("Nama region tidak boleh kosong");
        }
    }
}
