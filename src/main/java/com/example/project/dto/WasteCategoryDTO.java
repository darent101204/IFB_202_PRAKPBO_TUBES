package com.example.project.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class WasteCategoryDTO {
    private Long id;

    @NotBlank(message = "Nama kategori tidak boleh kosong")
    private String name;

    private String description;

    @NotBlank(message = "Unit tidak boleh kosong")
    private String unit;
}
