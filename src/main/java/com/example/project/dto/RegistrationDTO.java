package com.example.project.dto;

import lombok.Data;

@Data
public class RegistrationDTO {
    private String name;
    private String email;
    private String password;
    private String confirmPassword;
    private Long regionId;
    private Long rtId;
}
