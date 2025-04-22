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
public class DrugsDTO {

    private Integer drugsId;
    private String drugsName;
    private DrugsFormDTO drugsForm;
    private OffsetDateTime expirationDate;
    private String drugsDescription;
}
