package com.example.project.service;

import com.example.project.dto.RegistrationDTO;
import com.example.project.model.User;

public interface RegistrationService {
    User registerResident(RegistrationDTO dto);
}
