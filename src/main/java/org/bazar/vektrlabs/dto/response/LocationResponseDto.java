package org.bazar.vektrlabs.dto.response;

import lombok.Builder;

import java.util.UUID;

@Builder
public record LocationResponseDto(
        UUID id,
        String name,
        String address,
        String city,
        String country,
        String postalCode,
        String phone,
        Double latitude,
        Double longitude,
        boolean visible,
        UUID storeId,
        boolean primary
) {
}