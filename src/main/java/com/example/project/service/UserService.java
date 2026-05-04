package com.example.project.service;

import com.example.project.dto.UserDTO;
import com.example.project.model.User;

import java.util.List;

public interface UserService {
    User findById(Long id);
    User findByEmail(String email);
    List<User> findAll();
    User save(UserDTO dto);
    User update(Long id, UserDTO dto);
    void deleteById(Long id);
    boolean authenticate(String email, String password);
}
