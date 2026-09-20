package org.bazar.vektrlabs.dto.response;

import lombok.Builder;

import java.util.UUID;

@Builder
public record UserProfileResponseDto(
        UUID id,
        String cognitoSub,
        String firstName,
        String lastName,
        String middleName,
        String email
) {
}
