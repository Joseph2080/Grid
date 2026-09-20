package org.bazar.vektrlabs.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import java.util.UUID;

@Builder
public record ChatRequestDto(
        @NotBlank
        String message,
        UUID storeId,
        UUID cartId
) {}