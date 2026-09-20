package org.bazar.vektrlabs.controller.rest;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.bazar.vektrlabs.dto.request.UserProfileRequestDto;
import org.bazar.vektrlabs.service.UserProfileService;
import org.bazar.vektrlabs.util.CurrentUserUtil;
import org.jericho.common.util.RestUtil;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/user-profiles")
@RequiredArgsConstructor
public class UserProfileController {

    private final UserProfileService userProfileService;
    private final CurrentUserUtil currentUserUtil;

    @PostMapping
    public ResponseEntity<Map<String, Object>> create(
            @Valid @RequestBody UserProfileRequestDto dto) {
        return RestUtil.buildResponse(
                userProfileService.create(dto),
                HttpStatus.CREATED,
                "Profile created successfully."
        );
    }

    /**
     * Frontend polls this on load to decide profile-gating/redirects:
     * 200 profile exists, 401 not signed in, 404 signed in but no profile.
     */
    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> me() {
        var profile = userProfileService.findByCognitoSubOrElseThrowException(currentUserUtil.currentCognitoSub());
        return RestUtil.buildResponse(
                profile,
                HttpStatus.OK,
                "Profile retrieved successfully."
        );
    }

    @PutMapping("/me")
    public ResponseEntity<Map<String, Object>> update(
            @Valid @RequestBody UserProfileRequestDto dto) {
        var profile = userProfileService.findByCognitoSubOrElseThrowException(currentUserUtil.currentCognitoSub());
        return RestUtil.buildResponse(
                userProfileService.update(profile.id(), dto),
                HttpStatus.OK,
                "Profile updated successfully."
        );
    }
}
