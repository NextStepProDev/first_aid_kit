package com.drugs.controller.dto;

import com.drugs.infrastructure.validation.ValueOfEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import lombok.*;

@Data
@Schema(description = "Request object for creating a new drug")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DrugsRequestDTO {

    @NotBlank(message = "Drug name must not be blank")
    @Size(min = 2, max = 100, message = "Drug name must be between 2 and 100 characters")
    @Schema(description = "Name of the drug", example = "Ibuprofen")
    private String name;

    @ValueOfEnum(enumClass = DrugsFormDTO.class, message = "Invalid form. Accepted values: {enumValues}")
    @Schema(description = "Form of the drug", example = "PILLS")
    private String form;

    @NotNull(message = "Expiration year must not be null")
    @Min(value = 2025, message = "Expiration year must be 2025 or later")
    @Schema(description = "Expiration year of the drug", example = "2025")
    private Integer expirationYear;

    @NotNull(message = "Expiration month must not be null")
    @Min(value = 1, message = "Expiration month must be between 1 and 12")
    @Max(value = 12, message = "Expiration month must be between 1 and 12")
    @Schema(description = "Expiration month of the drug", example = "5")
    private Integer expirationMonth;

    @NotBlank(message = "Description must not be blank")
    @Size(max = 500, message = "Description must be at most 500 characters")
    @Schema(description = "Description of the drug", example = "Painkiller for fever and inflammation")
    private String description;
}