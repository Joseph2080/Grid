package org.bazar.vektrlabs.directprofile;

import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import org.bazar.vektrlabs.dto.request.StoreRequestDto;
import org.bazar.vektrlabs.entity.Store;
import org.bazar.vektrlabs.entity.UserProfile;
import org.bazar.vektrlabs.exception.ProfileIncompleteException;
import org.bazar.vektrlabs.exception.ProfileNotFoundException;
import org.bazar.vektrlabs.mapper.StoreMapper;
import org.bazar.vektrlabs.repository.StoreRepository;
import org.bazar.vektrlabs.service.UserProfileService;
import org.bazar.vektrlabs.service.impl.StoreServiceImpl;
import org.bazar.vektrlabs.util.CurrentUserUtil;
import org.jericho.mediaresource.dto.MediaResourceRequestDto;
import org.jericho.mediaresource.dto.MediaResourceResponseDto;
import org.jericho.mediaresource.entity.MediaResource;
import org.jericho.mediaresource.service.MediaResourceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class StoreServiceImplTest {
    private final StoreRepository repository = mock(StoreRepository.class);
    private final MediaResourceService media = mock(MediaResourceService.class);
    private final UserProfileService profiles = mock(UserProfileService.class);
    private final CurrentUserUtil currentUser = mock(CurrentUserUtil.class);
    private final UserProfile owner = Fixtures.profile("owner");
    private final StoreServiceImpl service =
            new StoreServiceImpl(repository, new StoreMapper(), media, profiles, currentUser);
    private final Map<UUID, Store> savedStores = new HashMap<>();

    @BeforeEach
    void setUp() {
        when(currentUser.currentCognitoSub()).thenReturn("owner");
        when(profiles.requireCompleteProfileByCognitoSub("owner")).thenReturn(owner);
        when(repository.save(any(Store.class))).thenAnswer(invocation -> {
            Store store = invocation.getArgument(0);
            assertSame(owner, store.getUserProfile(), "The dependency hook must set ownership before saving");
            if (store.getId() == null) {
                store.setId(UUID.randomUUID());
            }
            savedStores.put(store.getId(), store);
            return store;
        });
        when(repository.getReferenceById(any(UUID.class)))
                .thenAnswer(invocation -> savedStores.get(invocation.getArgument(0)));
        ReflectionTestUtils.setField(service, "bucketKey", "store-bucket");
    }

    @Test
    void profileRelationIsRequiredNonUniqueManyToOne() throws Exception {
        var field = Store.class.getDeclaredField("userProfile");
        var relation = field.getAnnotation(ManyToOne.class);
        var column = field.getAnnotation(JoinColumn.class);

        assertNotNull(relation);
        assertFalse(relation.optional());
        assertEquals(FetchType.LAZY, relation.fetch());
        assertNull(field.getAnnotation(OneToOne.class));
        assertEquals("user_profile_id", column.name());
        assertFalse(column.nullable());
        assertFalse(column.unique());
    }

    @Test
    void sameProfileCanCreateMultipleStoresThroughNormalRequestDto() {
        var first = service.create(request("First store", false));
        var second = service.create(request("Second store", false));

        assertNotEquals(first.getId(), second.getId());
        assertEquals(2, savedStores.size());
        assertTrue(savedStores.values().stream().allMatch(store -> store.getUserProfile() == owner));
        verify(repository, times(2)).save(any(Store.class));
        verifyNoInteractions(media);
    }

    @Test
    void createPreservesLogoUploadPathAndRequestFields() {
        var request = request("Store with logo", true);
        request.setDescription("Description");
        request.setTwitterUrl("https://example.test/social");
        var resource = mock(MediaResource.class);
        var response = mock(MediaResourceResponseDto.class);
        var mediaId = UUID.randomUUID();
        when(response.getId()).thenReturn(mediaId);
        when(media.create(any(MediaResourceRequestDto.class))).thenReturn(response);
        when(media.findReferenceById(mediaId)).thenReturn(resource);
        when(media.generatePreSignedUrlForResource(mediaId)).thenReturn("https://example.test/logo");

        var created = service.create(request);

        var mediaRequest = ArgumentCaptor.forClass(MediaResourceRequestDto.class);
        verify(media).create(mediaRequest.capture());
        assertSame(request.getLogo(), mediaRequest.getValue().getMultipartFile());
        assertEquals("store-bucket", mediaRequest.getValue().getContext());
        assertTrue(mediaRequest.getValue().getObjectKey().startsWith("stores/" + created.getId() + "/logo/"));
        assertTrue(mediaRequest.getValue().getObjectKey().endsWith(".png"));
        assertEquals("Store with logo", created.getName());
        assertEquals("Description", created.getDescription());
        assertEquals("https://example.test/social", created.getXUrl());
        assertEquals("https://example.test/logo", created.getLogoUrl());
        assertSame(resource, savedStores.get(created.getId()).getLogo());
        verify(repository, times(2)).save(any(Store.class));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void missingOrIncompleteProfileStopsPersistenceAndMedia(boolean missing) {
        RuntimeException failure = missing ? new ProfileNotFoundException("missing")
                : new ProfileIncompleteException("incomplete");
        when(profiles.requireCompleteProfileByCognitoSub("owner")).thenThrow(failure);

        assertSame(failure, assertThrows(RuntimeException.class, () -> service.create(request("Blocked", true))));
        assertSame(failure, assertThrows(RuntimeException.class,
                () -> service.update(UUID.randomUUID(), request("Blocked update", false))));
        assertSame(failure, assertThrows(RuntimeException.class, () -> service.deleteById(UUID.randomUUID())));
        verify(repository, never()).save(any());
        verify(repository, never()).deleteById(any());
        verifyNoInteractions(media);
    }

    @Test
    void updateKeepsExistingProfile() {
        var existing = ownedStore(owner);
        when(repository.findById(existing.getId())).thenReturn(Optional.of(existing));

        var updated = service.update(existing.getId(), request("Renamed store", false));

        assertEquals("Renamed store", updated.getName());
        assertSame(owner, existing.getUserProfile());
        verify(repository).save(existing);
        verifyNoInteractions(media);
    }

    @Test
    void foreignUpdateAndDeleteFailBeforeMutatingStore() {
        var foreignOwner = Fixtures.profile("foreign");
        var existing = ownedStore(foreignOwner);
        when(repository.findById(existing.getId())).thenReturn(Optional.of(existing));

        assertThrows(AccessDeniedException.class,
                () -> service.update(existing.getId(), request("Stolen", true)));
        assertThrows(AccessDeniedException.class, () -> service.deleteById(existing.getId()));

        assertEquals("Original", existing.getName());
        assertSame(foreignOwner, existing.getUserProfile());
        verify(repository, never()).save(any());
        verify(repository, never()).deleteById(any());
        verify(repository, never()).delete(any(Store.class));
        verifyNoInteractions(media);
    }

    private Store ownedStore(UserProfile profile) {
        var store = new Store();
        store.setId(UUID.randomUUID());
        store.setName("Original");
        store.setUserProfile(profile);
        return store;
    }

    private StoreRequestDto request(String name, boolean hasLogo) {
        return StoreRequestDto.builder().name(name)
                .currency(org.bazar.vektrlabs.entity.enums.Currency.USD)
                .logo(new MockMultipartFile("logo", "logo.png", "image/png",
                        hasLogo ? new byte[]{1, 2, 3} : new byte[0]))
                .build();
    }
}
