package com.uretimtakip.erp.workorder.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class WorkOrderPartRequest {

    @NotNull(message = "Is emri ID bos olamaz")
    private UUID workOrderId;

    @NotNull(message = "Parca ID bos olamaz")
    private UUID partId;

    @Positive(message = "Adet 0'dan buyuk olmali")
    private Integer qty;
}