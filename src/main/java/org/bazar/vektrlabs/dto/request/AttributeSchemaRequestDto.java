package org.bazar.vektrlabs.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

@Builder
public record AttributeSchemaRequestDto(
        @NotBlank
        String code,
        @NotBlank
        String name,
        @NotNull
        Integer version,
        @NotBlank
        String schemaJson,
        boolean active
) {
}