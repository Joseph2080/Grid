package org.bazar.vektrlabs.dto.response;

import lombok.Builder;

import java.util.UUID;

@Builder
public record AttributeSchemaResponseDto(
        UUID id,
        String code,
        String name,
        Integer version,
        String schemaJson,
        boolean active
) {
}