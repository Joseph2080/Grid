package org.bazar.vektrlabs.directprofile;

import jakarta.validation.Validation;
import org.bazar.vektrlabs.dto.request.UserProfileRequestDto;
import org.bazar.vektrlabs.entity.UserProfile;
import org.bazar.vektrlabs.exception.GlobalExceptionHandler;
import org.bazar.vektrlabs.exception.ProfileIncompleteException;
import org.bazar.vektrlabs.exception.ProfileNotFoundException;
import org.bazar.vektrlabs.mapper.UserProfileMapper;
import org.bazar.vektrlabs.repository.UserProfileRepository;
import org.bazar.vektrlabs.service.impl.UserProfileServiceImpl;
import org.bazar.vektrlabs.util.CurrentUserUtil;
import org.jericho.common.exception.InvalidParameterException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;

import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class UserProfileServiceImplTest {
    private final UserProfileRepository repository = mock(UserProfileRepository.class);
    private final CurrentUserUtil currentUser = mock(CurrentUserUtil.class);
    private final UserProfileMapper mapper = new UserProfileMapper();
    private final UserProfileServiceImpl service = new UserProfileServiceImpl(repository, mapper, currentUser);

    @BeforeEach
    void identity() {
        when(currentUser.currentCognitoSub()).thenReturn("owner");
        when(currentUser.currentEmailClaim()).thenReturn("claim@example.test");
    }

    @Test
    void missingProfileHasSpecific404Failure() {
        var failure = assertThrows(ProfileNotFoundException.class,
                () -> service.findEntityByCognitoSubOrElseThrowException("missing"));

        assertEquals(HttpStatus.NOT_FOUND,
                new GlobalExceptionHandler().handleProfileNotFoundException(failure).getStatusCode());
        assertThrows(ProfileNotFoundException.class,
                () -> service.findByCognitoSubOrElseThrowException("missing"));
        verify(repository, never()).save(any());
    }

    @ParameterizedTest
    @CsvSource(value = {"NULL,Last", "'',Last", "'   ',Last", "First,NULL", "First,''", "First,'   '"},
            nullValues = "NULL")
    void completeProfileRejectsMissingRequiredNames(String first, String last) {
        var profile = Fixtures.profile("owner");
        profile.setFirstName(first);
        profile.setLastName(last);
        when(repository.findByCognitoSub("owner")).thenReturn(Optional.of(profile));

        assertThrows(ProfileIncompleteException.class, () -> service.requireCompleteProfileByCognitoSub("owner"));
        verify(repository, never()).save(any());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"Middle"})
    void middleNameIsOptionalInDtoAndCompleteProfile(String middleName) {
        var profile = Fixtures.profile("owner");
        profile.setMiddleName(middleName);
        when(repository.findByCognitoSub("owner")).thenReturn(Optional.of(profile));
        var request = new UserProfileRequestDto("First", "Last", middleName);

        try (var factory = Validation.buildDefaultValidatorFactory()) {
            assertTrue(factory.getValidator().validate(request).isEmpty());
        }
        assertSame(profile, service.requireCompleteProfileByCognitoSub("owner"));
        assertEquals(middleName, service.findByCognitoSubOrElseThrowException("owner").middleName());
    }

    @ParameterizedTest
    @CsvSource(value = {"NULL,Last", "'',Last", "'   ',Last", "First,NULL", "First,''", "First,'   '"},
            nullValues = "NULL")
    void createAndUpdateRequireBothNames(String first, String last) {
        var profile = Fixtures.profile("owner");
        when(repository.findByCognitoSub("owner")).thenReturn(Optional.empty());
        when(repository.findById(profile.getId())).thenReturn(Optional.of(profile));
        var request = new UserProfileRequestDto(first, last, null);

        try (var factory = Validation.buildDefaultValidatorFactory()) {
            assertFalse(factory.getValidator().validate(request).isEmpty());
        }
        assertThrows(InvalidParameterException.class, () -> service.create(request));
        assertThrows(InvalidParameterException.class, () -> service.update(profile.getId(), request));
        verify(repository, never()).save(any());
    }

    @Test
    void createAndUpdateMapNamesButTakeIdentityOnlyFromClaims() {
        when(repository.save(any(UserProfile.class))).thenAnswer(invocation -> {
            UserProfile saved = invocation.getArgument(0);
            if (saved.getId() == null) {
                saved.setId(UUID.randomUUID());
            }
            return saved;
        });
        var created = service.create(new UserProfileRequestDto("First", "Last", "Middle"));

        assertEquals("owner", created.cognitoSub());
        assertEquals("claim@example.test", created.email());
        assertEquals("Middle", created.middleName());
        var profile = Fixtures.profile("owner");
        when(repository.findById(profile.getId())).thenReturn(Optional.of(profile));
        // Updating an already-existing profile must succeed: the "already exists" check
        // only applies when creating a brand-new profile, not when updating one.
        var updated = service.update(profile.getId(), new UserProfileRequestDto("New", "Name", null));

        assertEquals("New", updated.firstName());
        assertEquals("Name", updated.lastName());
        assertNull(updated.middleName());
        assertEquals("owner", updated.cognitoSub());
        assertEquals("claim@example.test", updated.email());
    }

    @Test
    void cannotCreateASecondProfileForTheSameSignedInUser() {
        var existing = Fixtures.profile("owner");
        when(repository.findByCognitoSub("owner")).thenReturn(Optional.of(existing));

        assertThrows(InvalidParameterException.class,
                () -> service.create(new UserProfileRequestDto("First", "Last", null)));
        verify(repository, never()).save(any());
    }

    @Test
    void requestAndMapperCannotOverwriteIdentity() {
        assertEquals(java.util.Set.of("firstName", "lastName", "middleName"),
                Arrays.stream(UserProfileRequestDto.class.getRecordComponents())
                        .map(java.lang.reflect.RecordComponent::getName).collect(java.util.stream.Collectors.toSet()));
        var profile = Fixtures.profile("owner");
        var id = profile.getId();
        mapper.updateEntityFromDto(new UserProfileRequestDto("New", "Name", "Middle"), profile);

        assertEquals(id, profile.getId());
        assertEquals("owner", profile.getCognitoSub());
        assertEquals("owner@example.test", profile.getEmail());
        var newProfile = mapper.convertDtoToEntity(new UserProfileRequestDto("First", "Last", null));
        assertNull(newProfile.getCognitoSub());
        assertNull(newProfile.getEmail());
    }

    @Test
    void cannotUpdateAnotherProfileBeforeMapperMutatesIt() {
        var foreign = Fixtures.profile("other");
        when(repository.findById(foreign.getId())).thenReturn(Optional.of(foreign));

        assertThrows(AccessDeniedException.class,
                () -> service.update(foreign.getId(), new UserProfileRequestDto("Changed", "Name", null)));

        assertEquals("First", foreign.getFirstName());
        assertEquals("other", foreign.getCognitoSub());
        verify(repository, never()).save(any());
    }
}
