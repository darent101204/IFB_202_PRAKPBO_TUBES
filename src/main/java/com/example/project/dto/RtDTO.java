package com.example.project.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RtDTO {
    private Long id;

    @NotBlank(message = "Nama RT tidak boleh kosong")
    private String name;

    @NotNull(message = "Region harus dipilih")
    private Long regionId;

    private String contactPhone;
    private String address;
}
