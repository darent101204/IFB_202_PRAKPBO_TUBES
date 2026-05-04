package com.example.project.service.impl;

import com.example.project.dto.RtDTO;
import com.example.project.exception.ResourceNotFoundException;
import com.example.project.model.Region;
import com.example.project.model.Rt;
import com.example.project.repository.RegionRepository;
import com.example.project.repository.RtRepository;
import com.example.project.service.RtService;
import lombok.RequiredArgsConstructor;
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
        Region region = regionRepository.findById(dto.getRegionId())
                .orElseThrow(() -> new ResourceNotFoundException("Region", dto.getRegionId()));
        Rt rt = new Rt();
        rt.setName(dto.getName());
        rt.setRegion(region);
        rt.setContactPhone(dto.getContactPhone());
        rt.setAddress(dto.getAddress());
        return rtRepository.save(rt);
    }

    @Override
    public Rt update(Long id, RtDTO dto) {
        Rt rt = findById(id);
        Region region = regionRepository.findById(dto.getRegionId())
                .orElseThrow(() -> new ResourceNotFoundException("Region", dto.getRegionId()));
        rt.setName(dto.getName());
        rt.setRegion(region);
        rt.setContactPhone(dto.getContactPhone());
        rt.setAddress(dto.getAddress());
        return rtRepository.save(rt);
    }

    @Override
    public void deleteById(Long id) {
        if (!rtRepository.existsById(id)) {
            throw new ResourceNotFoundException("RT", id);
        }
        rtRepository.deleteById(id);
    }
}
