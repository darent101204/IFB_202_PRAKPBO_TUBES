package com.example.project.service.impl;

import com.example.project.dto.RtDTO;
import com.example.project.exception.BusinessException;
import com.example.project.exception.ResourceNotFoundException;
import com.example.project.model.Region;
import com.example.project.model.Rt;
import com.example.project.repository.RegionRepository;
import com.example.project.repository.RtRepository;
import com.example.project.service.RtService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class RtServiceImpl implements RtService {

    private final RtRepository rtRepository;
    private final RegionRepository regionRepository;

    @Override
    public Rt findById(Long id) {
        return rtRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("RT", id));
    }

    @Override
    public List<Rt> findAll() {
        return rtRepository.findAll();
    }

    @Override
    public List<Rt> findByRegionId(Long regionId) {
        return rtRepository.findByRegionId(regionId);
    }

    @Override
    public Rt save(RtDTO dto) {
        validate(dto);
        Region region = findRegion(dto.getRegionId());
        Rt rt = new Rt();
        apply(dto, rt, region);
        return rtRepository.save(rt);
    }

    @Override
    public Rt update(Long id, RtDTO dto) {
        validate(dto);
        Rt rt = findById(id);
        Region region = findRegion(dto.getRegionId());
        apply(dto, rt, region);
        return rtRepository.save(rt);
    }

    @Override
    public void deleteById(Long id) {
        if (!rtRepository.existsById(id)) {
            throw new ResourceNotFoundException("RT", id);
        }
        try {
            rtRepository.deleteById(id);
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException("RT tidak dapat dihapus karena masih digunakan oleh user atau request");
        }
    }

    private Region findRegion(Long regionId) {
        return regionRepository.findById(regionId)
                .orElseThrow(() -> new ResourceNotFoundException("Region", regionId));
    }

    private void apply(RtDTO dto, Rt rt, Region region) {
        rt.setName(dto.getName().trim());
        rt.setRegion(region);
        rt.setContactPhone(dto.getContactPhone());
        rt.setAddress(dto.getAddress());
    }

    private void validate(RtDTO dto) {
        if (dto.getName() == null || dto.getName().isBlank()) {
            throw new BusinessException("Nama RT tidak boleh kosong");
        }
        if (dto.getRegionId() == null) {
            throw new BusinessException("Region RT harus dipilih");
        }
    }
}
