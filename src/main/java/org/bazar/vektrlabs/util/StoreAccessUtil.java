package org.bazar.vektrlabs.util;

import lombok.RequiredArgsConstructor;
import org.bazar.vektrlabs.entity.Store;
import org.bazar.vektrlabs.service.UserProfileService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StoreAccessUtil {

    private final CurrentUserUtil currentUserUtil;
    private final UserProfileService userProfileService;

    public void requireAccess(Store store) {
        var profile = userProfileService.requireCompleteProfileByCognitoSub(
                currentUserUtil.currentCognitoSub());
        if (store == null || store.getUserProfile() == null || profile.getId() == null
                || !profile.getId().equals(store.getUserProfile().getId())) {
            throw new AccessDeniedException("You are not associated with this store.");
        }
    }
}
