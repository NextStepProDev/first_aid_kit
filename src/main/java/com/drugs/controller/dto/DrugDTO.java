package com.drugs.controller.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DrugDTO {

    private Integer drugId;
    private String drugName;
    private DrugFormDTO drugForm;
    private OffsetDateTime expirationDate;
    private String drugDescription;
}
