package org.bazar.vektrlabs.service.impl;

import org.bazar.vektrlabs.dto.request.UserProfileRequestDto;
import org.bazar.vektrlabs.dto.response.UserProfileResponseDto;
import org.bazar.vektrlabs.entity.UserProfile;
import org.bazar.vektrlabs.exception.ProfileNotFoundException;
import org.bazar.vektrlabs.exception.ProfileIncompleteException;
import org.bazar.vektrlabs.mapper.UserProfileMapper;
import org.bazar.vektrlabs.repository.UserProfileRepository;
import org.bazar.vektrlabs.service.UserProfileService;
import org.bazar.vektrlabs.util.CurrentUserUtil;
import org.jericho.common.exception.EntityNotFoundException;
import org.jericho.common.exception.InvalidParameterException;
import org.jericho.common.service.AbstractJpaService;
import org.springframework.stereotype.Service;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.UUID;

@Service
public class UserProfileServiceImpl extends AbstractJpaService<
        UserProfile,
        UUID,
        UserProfileRequestDto,
        UserProfileResponseDto,
        UserProfileRepository>
        implements UserProfileService {

    private final CurrentUserUtil currentUserUtil;

    public UserProfileServiceImpl(
            UserProfileRepository repository,
            UserProfileMapper dtoMapper,
            CurrentUserUtil currentUserUtil) {
        super(repository, dtoMapper);
        this.currentUserUtil = currentUserUtil;
    }

    @Override
    protected void setEntityDependencies(UserProfile userProfile, UserProfileRequestDto requestDto) {
        validateRequiredNames(requestDto);
        String sub = currentUserUtil.currentCognitoSub();
        if (userProfile.getCognitoSub() == null) {
            if (repository.findByCognitoSub(sub).isPresent()) {
                throw new InvalidParameterException("A profile already exists for this signed-in user.");
            }
        } else if (!sub.equals(userProfile.getCognitoSub())) {
            throw new AccessDeniedException("You do not have access to this profile.");
        }
        String email = currentUserUtil.currentEmailClaim();
        if (!StringUtils.hasText(email)) {
            throw new InvalidParameterException("An email claim is required to register a profile.");
        }
        userProfile.setCognitoSub(sub);
        userProfile.setEmail(email);
    }

    // we should delegate this to dto layer but lets leave it here!
    private void validateRequiredNames(UserProfileRequestDto requestDto) {
        if (!StringUtils.hasText(requestDto.firstName()) || !StringUtils.hasText(requestDto.lastName())) {
            throw new InvalidParameterException("First and last name are required.");
        }
    }

    @Override
    @Transactional
    public UserProfileResponseDto update(UUID id, UserProfileRequestDto requestDto) {
        var profile = findEntityByIdOrElseThrowException(id);
        if (!currentUserUtil.currentCognitoSub().equals(profile.getCognitoSub())) {
            throw new AccessDeniedException("You do not have access to this profile.");
        }
        return super.update(id, requestDto);
    }

    @Override
    @Transactional(readOnly = true)
    public UserProfileResponseDto findByCognitoSubOrElseThrowException(String cognitoSub) {
        return dtoMapper.convertEntityToResponseDto(findEntityByCognitoSubOrElseThrowException(cognitoSub));
    }

    @Override
    @Transactional(readOnly = true)
    public UserProfile findEntityByCognitoSubOrElseThrowException(String cognitoSub) {
        if (!StringUtils.hasText(cognitoSub)) {
            throw new InvalidParameterException("Cognito subject is required.");
        }
        return repository.findByCognitoSub(cognitoSub)
                .orElseThrow(() -> new ProfileNotFoundException(
                        "No profile found for the signed-in user."));
    }

    @Override
    @Transactional(readOnly = true)
    public UserProfile requireCompleteProfileByCognitoSub(String cognitoSub) {
        var profile = findEntityByCognitoSubOrElseThrowException(cognitoSub);
        if (!StringUtils.hasText(profile.getFirstName()) || !StringUtils.hasText(profile.getLastName())) {
            throw new ProfileIncompleteException("Complete your first and last name before continuing.");
        }
        return profile;
    }

    @Override
    protected EntityNotFoundException entityNotFoundException() {
        return new ProfileNotFoundException("Profile not found.");
    }
}
