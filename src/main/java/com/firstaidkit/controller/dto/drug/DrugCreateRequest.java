package com.firstaidkit.controller.dto.drug;

import com.firstaidkit.infrastructure.validation.NotBeforeCurrentMonth;
import com.firstaidkit.infrastructure.validation.NotBeforeCurrentYear;
import com.firstaidkit.infrastructure.validation.ValueOfEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Schema(description = "Request object for creating a new drug")
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@NotBeforeCurrentMonth
public class DrugCreateRequest {

    @NotBlank(message = "Drug name must not be blank")
    @Size(min = 2, max = 100, message = "Drug name must be between 2 and 100 characters")
    @Schema(description = "Name of the drug", example = "Ibuprofen")
    @NotNull(message = "Drug name must not be null")
    private String name;

    @NotBlank(message = "Form must not be blank")
    @NotNull(message = "Form must not be null")
    @Size(max = 36, message = "Form must be at most 36 characters")
    @ValueOfEnum(enumClass = DrugFormDTO.class, message = "Invalid form. Accepted values: {enumValues}")
    @Schema(description = "Form of the drug", example = "PILLS")
    private String form;

    @NotNull(message = "Expiration year must not be null")
    @NotBeforeCurrentYear
    @Schema(description = "Expiration year of the drug", example = "2025")
    private Integer expirationYear;

    @NotNull(message = "Expiration month must not be null")
    @Min(value = 1, message = "Expiration month must be between 1 and 12")
    @Max(value = 12, message = "Expiration month must be between 1 and 12")
    @Schema(description = "Expiration month of the drug", example = "5")
    private Integer expirationMonth;

    @Size(max = 2000, message = "Description must be at most 2000 characters")
    @Schema(description = "Description of the drug", example = "Painkiller for fever and inflammation", nullable = true)
    private String description;
}
