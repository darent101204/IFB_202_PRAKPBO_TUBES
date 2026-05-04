package com.example.project.service;

import com.example.project.dto.RtDTO;
import com.example.project.model.Rt;

import java.util.List;

public interface RtService {
    Rt findById(Long id);
    List<Rt> findAll();
    List<Rt> findByRegionId(Long regionId);
    Rt save(RtDTO dto);
    Rt update(Long id, RtDTO dto);
    void deleteById(Long id);
}
