package org.bazar.vektrlabs.directprofile;

import org.bazar.vektrlabs.cart.Cart;
import org.bazar.vektrlabs.cart.CartItemRequestDto;
import org.bazar.vektrlabs.cart.CartNotFoundException;
import org.bazar.vektrlabs.cart.CartRequestDto;
import org.bazar.vektrlabs.cart.CartService;
import org.bazar.vektrlabs.exception.ProfileIncompleteException;
import org.bazar.vektrlabs.exception.ProfileNotFoundException;
import org.bazar.vektrlabs.service.ProductVariantService;
import org.bazar.vektrlabs.service.UserProfileService;
import org.bazar.vektrlabs.util.CurrentUserUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class CartServiceTest {
    private final ProductVariantService variants = mock(ProductVariantService.class);
    private final CurrentUserUtil currentUser = mock(CurrentUserUtil.class);
    private final UserProfileService profiles = mock(UserProfileService.class);
    private final CartService service = new CartService(variants, currentUser, profiles);
    private final UUID variantId = UUID.randomUUID();
    private UUID cartId;

    @BeforeEach
    void createOwnedCart() {
        when(currentUser.currentCognitoSub()).thenReturn("owner");
        when(profiles.requireCompleteProfileByCognitoSub("owner")).thenReturn(Fixtures.profile("owner"));
        when(profiles.requireCompleteProfileByCognitoSub("other")).thenReturn(Fixtures.profile("other"));
        cartId = service.createCart(request()).getId();
        clearInvocations(variants, profiles, currentUser);
    }

    @AfterEach
    void clearContext() {
        org.springframework.security.core.context.SecurityContextHolder.clearContext();
    }

    @ParameterizedTest
    @CsvSource({
            "create,true", "read,true", "add,true", "update,true", "remove,true", "clear,true",
            "create,false", "read,false", "add,false", "update,false", "remove,false", "clear,false"
    })
    void allPublicCartOperationsRequireCompleteProfile(String operation, boolean missing) {
        RuntimeException failure = missing ? new ProfileNotFoundException("missing")
                : new ProfileIncompleteException("incomplete");
        when(profiles.requireCompleteProfileByCognitoSub("owner")).thenThrow(failure);

        assertSame(failure, assertThrows(RuntimeException.class, () -> perform(operation)));

        assertEquals(1, carts().size());
        assertEquals(2, carts().get(cartId).getItems().getFirst().getQuantity());
        assertEquals(Map.of(variantId, 2), reservations());
        verifyNoInteractions(variants);
        verify(profiles).requireCompleteProfileByCognitoSub("owner");
    }

    @ParameterizedTest
    @ValueSource(strings = {"create", "read", "add", "update", "remove", "clear"})
    void missingAuthenticationStopsAllCartOperationsBeforeProfileOrStockAccess(String operation) {
        when(currentUser.currentCognitoSub()).thenThrow(new InsufficientAuthenticationException("Sign in"));

        assertThrows(InsufficientAuthenticationException.class, () -> perform(operation));

        assertEquals(1, carts().size());
        assertEquals(Map.of(variantId, 2), reservations());
        verifyNoInteractions(profiles, variants);
    }

    @ParameterizedTest
    @ValueSource(strings = {"read", "add", "update", "remove", "clear"})
    void foreignUserCannotReadOrChangeStateOrStock(String operation) {
        when(currentUser.currentCognitoSub()).thenReturn("other");

        assertThrows(AccessDeniedException.class, () -> perform(operation));

        assertEquals(1, carts().size());
        assertEquals(2, carts().get(cartId).getItems().getFirst().getQuantity());
        assertEquals(Map.of(variantId, 2), reservations());
        verifyNoInteractions(variants);
    }

    @Test
    void foreignExpiredCartCannotBeMutatedBeforeOwnershipCheck() {
        carts().get(cartId).setExpiresAt(Instant.now().minusSeconds(1));
        when(currentUser.currentCognitoSub()).thenReturn("other");

        assertThrows(AccessDeniedException.class, () -> service.getCart(cartId));

        assertTrue(carts().containsKey(cartId));
        assertEquals(Map.of(variantId, 2), reservations());
        verifyNoInteractions(variants);
    }

    @Test
    void ownerCanReadAddUpdateRemoveAndClearWithCorrectReservations() {
        assertEquals(2, service.getCart(cartId).getItems().getFirst().getQuantity());
        assertEquals(3, service.addItem(cartId, item(1)).getItems().getFirst().getQuantity());
        assertEquals(Map.of(variantId, 3), reservations());
        assertEquals(5, service.updateQuantity(cartId, variantId, 5).getItems().getFirst().getQuantity());
        assertEquals(Map.of(variantId, 5), reservations());
        assertEquals(1, service.updateQuantity(cartId, variantId, 1).getItems().getFirst().getQuantity());
        assertEquals(Map.of(variantId, 1), reservations());
        assertTrue(service.removeItem(cartId, variantId).getItems().isEmpty());
        assertTrue(reservations().isEmpty());
        service.addItem(cartId, item(2));
        service.clear(cartId);
        assertTrue(carts().isEmpty());
        assertTrue(reservations().isEmpty());
        verify(profiles, times(7)).requireCompleteProfileByCognitoSub("owner");
    }

    @Test
    void clearAsSystemNeedsNoAuthenticationAndIsIdempotent() {
        when(currentUser.currentCognitoSub()).thenThrow(new AssertionError("Must not resolve a request identity"));

        service.clearAsSystem(cartId);
        service.clearAsSystem(cartId);
        service.clearAsSystem(UUID.randomUUID());

        assertTrue(carts().isEmpty());
        assertTrue(reservations().isEmpty());
        verifyNoInteractions(currentUser, profiles, variants);
    }

    @Test
    void staleDuplicateClearCannotReleaseAnotherCartsReservations() {
        var removedCart = carts().get(cartId);
        var otherCartId = service.createCart(request()).getId();
        assertEquals(Map.of(variantId, 4), reservations());
        clearInvocations(currentUser, profiles, variants);

        service.clearAsSystem(cartId);
        assertEquals(Map.of(variantId, 2), reservations());

        ReflectionTestUtils.invokeMethod(service, "clearInternal", cartId, removedCart);

        assertFalse(carts().containsKey(cartId));
        assertEquals(1, carts().size());
        assertEquals(2, carts().get(otherCartId).getItems().getFirst().getQuantity());
        assertEquals(Map.of(variantId, 2), reservations());
        verifyNoInteractions(currentUser, profiles, variants);
    }

    @Test
    void expiredOwnerCartCleansUpOnceWithoutRecursiveFailure() {
        carts().get(cartId).setExpiresAt(Instant.now().minusSeconds(1));

        assertThrows(CartNotFoundException.class, () -> service.getCart(cartId));
        assertTrue(carts().isEmpty());
        assertTrue(reservations().isEmpty());
        assertThrows(CartNotFoundException.class, () -> service.getCart(cartId));
        service.clearAsSystem(cartId);
        verify(profiles, times(2)).requireCompleteProfileByCognitoSub("owner");
        verifyNoInteractions(variants);
    }

    @Test
    void deniedStockReservationDoesNotChangeExistingCart() {
        doThrow(new IllegalStateException("Unavailable"))
                .when(variants).findAvailableVariant(eq(variantId), anyInt());

        assertThrows(IllegalStateException.class, () -> service.addItem(cartId, item(100)));

        assertEquals(2, carts().get(cartId).getItems().getFirst().getQuantity());
        assertEquals(Map.of(variantId, 2), reservations());
    }

    private void perform(String operation) {
        switch (operation) {
            case "create" -> service.createCart(request());
            case "read" -> service.getCart(cartId);
            case "add" -> service.addItem(cartId, item(1));
            case "update" -> service.updateQuantity(cartId, variantId, 5);
            case "remove" -> service.removeItem(cartId, variantId);
            case "clear" -> service.clear(cartId);
            default -> throw new IllegalArgumentException(operation);
        }
    }

    private CartRequestDto request() {
        return new CartRequestDto(item(2));
    }

    private CartItemRequestDto item(int quantity) {
        return new CartItemRequestDto(variantId, quantity);
    }

    @SuppressWarnings("unchecked")
    private Map<UUID, Cart> carts() {
        return (Map<UUID, Cart>) ReflectionTestUtils.getField(service, "carts");
    }

    @SuppressWarnings("unchecked")
    private Map<UUID, Integer> reservations() {
        return (Map<UUID, Integer>) ReflectionTestUtils.getField(service, "reservedItems");
    }
}
