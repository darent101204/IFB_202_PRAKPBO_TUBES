package com.example.project.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RegionDTO {
    private Long id;

    @NotBlank(message = "Nama region tidak boleh kosong")
    private String name;

    private String description;
}
