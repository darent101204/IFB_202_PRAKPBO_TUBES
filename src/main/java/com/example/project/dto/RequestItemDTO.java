package com.example.project.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RequestItemDTO {
    private Long id;

    @NotNull(message = "Kategori limbah harus dipilih")
    private Long categoryId;

    @NotNull(message = "Jumlah tidak boleh kosong")
    private Double quantity;

    private String notes;
}
