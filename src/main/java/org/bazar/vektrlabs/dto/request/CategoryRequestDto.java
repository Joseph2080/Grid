package org.bazar.vektrlabs.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record CategoryRequestDto(
        @NotBlank
        String name,
        @NotNull
        UUID storeId
) {}