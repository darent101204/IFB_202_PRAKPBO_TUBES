package com.example.project.service.impl;

import com.example.project.dto.RegistrationDTO;
import com.example.project.exception.BusinessException;
import com.example.project.exception.ResourceNotFoundException;
import com.example.project.model.Region;
import com.example.project.model.Role;
import com.example.project.model.Rt;
import com.example.project.model.User;
import com.example.project.repository.RegionRepository;
import com.example.project.repository.RtRepository;
import com.example.project.repository.UserRepository;
import com.example.project.service.RegistrationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class RegistrationServiceImpl implements RegistrationService {

    private final UserRepository userRepository;
    private final RegionRepository regionRepository;
    private final RtRepository rtRepository;

    @Override
    public User registerResident(RegistrationDTO dto) {
        // Validation
        if (dto.getName() == null || dto.getName().isBlank()) {
            throw new BusinessException("Nama tidak boleh kosong");
        }
        if (dto.getEmail() == null || dto.getEmail().isBlank()) {
            throw new BusinessException("Email tidak boleh kosong");
        }
        if (dto.getPassword() == null || dto.getPassword().isBlank()) {
            throw new BusinessException("Password tidak boleh kosong");
        }
        if (dto.getPassword().length() < 6) {
            throw new BusinessException("Password minimal 6 karakter");
        }
        if (!dto.getPassword().equals(dto.getConfirmPassword())) {
            throw new BusinessException("Konfirmasi password tidak cocok");
        }
        if (dto.getRegionId() == null) {
            throw new BusinessException("Region harus dipilih");
        }
        if (dto.getRtId() == null) {
            throw new BusinessException("RT harus dipilih");
        }

        String emailNormalized = dto.getEmail().trim().toLowerCase();
        if (userRepository.existsByEmail(emailNormalized)) {
            throw new BusinessException("Email sudah terdaftar");
        }

        // Server-side Region and RT validation
        Region region = regionRepository.findById(dto.getRegionId())
                .orElseThrow(() -> new ResourceNotFoundException("Region", dto.getRegionId()));
        Rt rt = rtRepository.findById(dto.getRtId())
                .orElseThrow(() -> new ResourceNotFoundException("RT", dto.getRtId()));

        if (rt.getRegion() == null || !rt.getRegion().getId().equals(region.getId())) {
            throw new BusinessException("RT yang dipilih harus berada di region yang sesuai");
        }

        // Create resident user
        User user = new User();
        user.setName(dto.getName().trim());
        user.setEmail(emailNormalized);
        user.setPassword(dto.getPassword()); // Stored plain text as required by login system
        user.setRole(Role.RESIDENT);
        user.setRegion(region);
        user.setRt(rt);
        user.setIsActive(true);

        return userRepository.save(user);
    }
}
