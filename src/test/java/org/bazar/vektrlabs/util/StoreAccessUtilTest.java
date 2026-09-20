package org.bazar.vektrlabs.util;

import org.bazar.vektrlabs.entity.Store;
import org.bazar.vektrlabs.entity.UserProfile;
import org.bazar.vektrlabs.service.UserProfileService;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StoreAccessUtilTest {

    @Test
    void comparesPersistentProfileIdsRatherThanEntityIdentity() {
        var currentUser = mock(CurrentUserUtil.class);
        var profiles = mock(UserProfileService.class);
        var caller = new UserProfile();
        caller.setId(UUID.randomUUID());
        var storeProfile = new UserProfile();
        storeProfile.setId(caller.getId());
        var store = new Store();
        store.setUserProfile(storeProfile);
        when(currentUser.currentCognitoSub()).thenReturn("subject");
        when(profiles.requireCompleteProfileByCognitoSub("subject")).thenReturn(caller);

        assertDoesNotThrow(() -> new StoreAccessUtil(currentUser, profiles).requireAccess(store));

        verify(profiles).requireCompleteProfileByCognitoSub("subject");
    }

    @Test
    void rejectsStoresWithoutAnAssociation() {
        var currentUser = mock(CurrentUserUtil.class);
        var profiles = mock(UserProfileService.class);
        var caller = new UserProfile();
        caller.setId(UUID.randomUUID());
        when(currentUser.currentCognitoSub()).thenReturn("subject");
        when(profiles.requireCompleteProfileByCognitoSub("subject")).thenReturn(caller);
        var access = new StoreAccessUtil(currentUser, profiles);

        assertThrows(AccessDeniedException.class, () -> access.requireAccess(new Store()));
        assertThrows(AccessDeniedException.class, () -> access.requireAccess(null));
    }
}
