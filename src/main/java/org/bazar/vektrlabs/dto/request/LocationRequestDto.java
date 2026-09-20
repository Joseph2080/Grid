package org.bazar.vektrlabs.dto.request;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import java.util.UUID;

@Builder
public record LocationRequestDto(
        @NotBlank
        String name,
        @NotBlank
        String address,
        String city,
        String country,
        String postalCode,
        String phone,
        Double latitude,
        Double longitude,
        boolean visible,
        @NotNull
        UUID storeId,
        boolean primary
) {
}