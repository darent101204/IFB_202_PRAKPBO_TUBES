package com.example.project.dto;

import com.example.project.model.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UserDTO {

    private Long id;

    @NotBlank(message = "Nama tidak boleh kosong")
    private String name;

    @NotBlank(message = "Email tidak boleh kosong")
    @Email(message = "Format email tidak valid")
    private String email;

    private String password;

    @NotNull(message = "Role harus dipilih")
    private Role role;

    private Long regionId;

    private Long rtId;

    private Boolean isActive = true;
}
