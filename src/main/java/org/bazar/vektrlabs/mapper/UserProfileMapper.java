package org.bazar.vektrlabs.mapper;

import org.bazar.vektrlabs.dto.request.UserProfileRequestDto;
import org.bazar.vektrlabs.dto.response.UserProfileResponseDto;
import org.bazar.vektrlabs.entity.UserProfile;
import org.jericho.common.mapper.DtoMapper;
import org.springframework.stereotype.Component;

@Component
public class UserProfileMapper implements DtoMapper<UserProfile, UserProfileRequestDto, UserProfileResponseDto> {

    @Override
    public UserProfile convertDtoToEntity(UserProfileRequestDto dto) {
        UserProfile userProfile = new UserProfile();
        updateEntityFromDto(dto, userProfile);
        return userProfile;
    }

    @Override
    public UserProfileResponseDto convertEntityToResponseDto(UserProfile userProfile) {
        return UserProfileResponseDto.builder()
                .id(userProfile.getId())
                .cognitoSub(userProfile.getCognitoSub())
                .firstName(userProfile.getFirstName())
                .lastName(userProfile.getLastName())
                .middleName(userProfile.getMiddleName())
                .email(userProfile.getEmail())
                .build();
    }

    @Override
    public void updateEntityFromDto(UserProfileRequestDto dto, UserProfile userProfile) {
        userProfile.setFirstName(dto.firstName());
        userProfile.setLastName(dto.lastName());
        userProfile.setMiddleName(dto.middleName());
        // cognitoSub/email are never taken from client input -- they are
        // resolved server-side in UserProfileServiceImpl.setEntityDependencies
        // via CurrentUserUtil, straight off the authenticated token.
    }
}
