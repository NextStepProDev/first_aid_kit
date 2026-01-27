package com.firstaidkit.controller.dto.drug;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DrugResponse {

    private Integer drugId;
    private String drugName;
    private DrugFormDTO drugForm;
    private OffsetDateTime expirationDate;
    private String drugDescription;
}
