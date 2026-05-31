package com.example.project.service.impl;

import com.example.project.dto.UserDTO;
import com.example.project.exception.BusinessException;
import com.example.project.exception.ResourceNotFoundException;
import com.example.project.model.Region;
import com.example.project.model.Role;
import com.example.project.model.Rt;
import com.example.project.model.User;
import com.example.project.repository.RegionRepository;
import com.example.project.repository.RtRepository;
import com.example.project.repository.UserRepository;
import com.example.project.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final RegionRepository regionRepository;
    private final RtRepository rtRepository;

    @Override
    public User findById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));
    }

    @Override
    public User findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User tidak ditemukan dengan email: " + email));
    }

    @Override
    public List<User> findAll() {
        return userRepository.findAll();
    }

    @Override
    public User save(UserDTO dto) {
        normalizeAndValidate(dto, true);
        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new BusinessException("Email sudah terdaftar: " + dto.getEmail());
        }
        User user = new User();
        mapDtoToEntity(dto, user);
        return userRepository.save(user);
    }

    @Override
    public User update(Long id, UserDTO dto) {
        User user = findById(id);
        normalizeAndValidate(dto, false);
        userRepository.findByEmail(dto.getEmail()).ifPresent(existing -> {
            if (!existing.getId().equals(id)) {
                throw new BusinessException("Email sudah digunakan oleh user lain");
            }
        });
        mapDtoToEntity(dto, user);
        return userRepository.save(user);
    }

    @Override
    public void deleteById(Long id) {
        if (!userRepository.existsById(id)) {
            throw new ResourceNotFoundException("User", id);
        }
        try {
            userRepository.deleteById(id);
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException("User tidak dapat dihapus karena masih terhubung dengan data request atau data lain");
        }
    }

    @Override
    public boolean authenticate(String email, String password) {
        if (email == null || password == null) {
            return false;
        }
        return userRepository.findByEmail(email.trim().toLowerCase())
                .map(u -> u.getPassword().equals(password) && Boolean.TRUE.equals(u.getIsActive()))
                .orElse(false);
    }

    private void normalizeAndValidate(UserDTO dto, boolean requirePassword) {
        if (dto.getName() == null || dto.getName().isBlank()) {
            throw new BusinessException("Nama user tidak boleh kosong");
        }
        if (dto.getEmail() == null || dto.getEmail().isBlank()) {
            throw new BusinessException("Email user tidak boleh kosong");
        }
        if (requirePassword && (dto.getPassword() == null || dto.getPassword().isBlank())) {
            throw new BusinessException("Password user tidak boleh kosong");
        }
        if (dto.getRole() == null) {
            throw new BusinessException("Role user harus dipilih");
        }

        dto.setName(dto.getName().trim());
        dto.setEmail(dto.getEmail().trim().toLowerCase());

        if ((dto.getRole() == Role.RT || dto.getRole() == Role.RESIDENT) && dto.getRtId() == null) {
            throw new BusinessException("User dengan role " + dto.getRole() + " harus terhubung dengan RT");
        }
        if ((dto.getRole() == Role.RT || dto.getRole() == Role.RESIDENT || dto.getRole() == Role.COLLECTOR)
                && dto.getRegionId() == null) {
            throw new BusinessException("User dengan role " + dto.getRole() + " harus terhubung dengan region");
        }
    }

    private void mapDtoToEntity(UserDTO dto, User user) {
        user.setName(dto.getName());
        user.setEmail(dto.getEmail());
        if (dto.getPassword() != null && !dto.getPassword().isBlank()) {
            user.setPassword(dto.getPassword());
        }
        user.setRole(dto.getRole());
        user.setIsActive(dto.getIsActive() != null ? dto.getIsActive() : true);

        if (dto.getRegionId() != null) {
            Region region = regionRepository.findById(dto.getRegionId())
                    .orElseThrow(() -> new ResourceNotFoundException("Region", dto.getRegionId()));
            user.setRegion(region);
        } else {
            user.setRegion(null);
        }

        if (dto.getRtId() != null) {
            Rt rt = rtRepository.findById(dto.getRtId())
                    .orElseThrow(() -> new ResourceNotFoundException("RT", dto.getRtId()));
            user.setRt(rt);
            if (user.getRegion() != null && rt.getRegion() != null && !rt.getRegion().getId().equals(user.getRegion().getId())) {
                throw new BusinessException("RT yang dipilih harus berada di region user");
            }
        } else {
            user.setRt(null);
        }
    }
}
