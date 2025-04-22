package com.drugs.controller.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DrugSimpleDTO {
    private Integer drugsId;
    private String drugsName;
    private DrugsFormDTO drugsForm;
    private String expirationDate;
}
