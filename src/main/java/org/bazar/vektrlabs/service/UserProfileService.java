package org.bazar.vektrlabs.service;

import org.bazar.vektrlabs.dto.request.UserProfileRequestDto;
import org.bazar.vektrlabs.dto.response.UserProfileResponseDto;
import org.bazar.vektrlabs.entity.UserProfile;
import org.jericho.common.service.Service;

import java.util.UUID;

public interface UserProfileService extends Service<UserProfile, UUID, UserProfileRequestDto, UserProfileResponseDto> {
    UserProfileResponseDto findByCognitoSubOrElseThrowException(String cognitoSub);
    UserProfile findEntityByCognitoSubOrElseThrowException(String cognitoSub);
    UserProfile requireCompleteProfileByCognitoSub(String cognitoSub);
}
