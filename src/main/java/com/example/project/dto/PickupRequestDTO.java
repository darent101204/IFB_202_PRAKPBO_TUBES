package com.example.project.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class PickupRequestDTO {
    private Long id;

    @NotNull(message = "RT harus dipilih")
    private Long rtId;

    private Long regionId;

    private String notes;

    private LocalDate scheduledDate;

    private List<RequestItemDTO> items;
}
