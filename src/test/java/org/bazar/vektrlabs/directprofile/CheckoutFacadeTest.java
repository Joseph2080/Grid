package org.bazar.vektrlabs.directprofile;

import org.bazar.vektrlabs.cart.CartItemRequestDto;
import org.bazar.vektrlabs.cart.CartItemResponseDto;
import org.bazar.vektrlabs.cart.CartRequestDto;
import org.bazar.vektrlabs.cart.CartResponseDto;
import org.bazar.vektrlabs.cart.CartService;
import org.bazar.vektrlabs.dto.request.OrderCommand;
import org.bazar.vektrlabs.dto.response.OrderResponseDto;
import org.bazar.vektrlabs.entity.UserProfile;
import org.bazar.vektrlabs.exception.ProfileIncompleteException;
import org.bazar.vektrlabs.exception.ProfileNotFoundException;
import org.bazar.vektrlabs.facade.CheckoutFacade;
import org.bazar.vektrlabs.service.OrderService;
import org.bazar.vektrlabs.service.ProductVariantService;
import org.bazar.vektrlabs.service.UserProfileService;
import org.bazar.vektrlabs.util.CurrentUserUtil;
import org.jericho.common.exception.InvalidParameterException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CheckoutFacadeTest {
    private final CartService carts = mock(CartService.class);
    private final OrderService orders = mock(OrderService.class);
    private final CurrentUserUtil currentUser = new CurrentUserUtil();
    private final UserProfileService profiles = mock(UserProfileService.class);
    private final UserProfile owner = Fixtures.profile("owner");
    private final CheckoutFacade facade = new CheckoutFacade(carts, orders, currentUser, profiles);

    @BeforeEach
    void signIn() {
        Fixtures.signIn("owner");
        when(profiles.requireCompleteProfileByCognitoSub("owner")).thenReturn(owner);
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void checkoutDerivesBuyerAndItemsFromAuthenticatedOwnedCart() {
        var requestedCartId = UUID.randomUUID();
        var persistedCartId = UUID.randomUUID();
        var firstVariant = UUID.randomUUID();
        var secondVariant = UUID.randomUUID();
        when(carts.getCart(requestedCartId)).thenReturn(CartResponseDto.builder()
                .id(persistedCartId)
                .items(List.of(new CartItemResponseDto(firstVariant, 2),
                        new CartItemResponseDto(secondVariant, 4))).build());
        var response = new OrderResponseDto();
        when(orders.create(any(OrderCommand.class))).thenReturn(response);

        assertSame(response, facade.checkout(requestedCartId));

        var command = ArgumentCaptor.forClass(OrderCommand.class);
        var inOrder = inOrder(profiles, carts, orders);
        inOrder.verify(profiles).requireCompleteProfileByCognitoSub("owner");
        inOrder.verify(carts).getCart(requestedCartId);
        inOrder.verify(orders).create(command.capture());
        assertEquals("owner", command.getValue().buyerCognitoSub());
        assertEquals(persistedCartId, command.getValue().cartId());
        assertEquals(List.of(firstVariant, secondVariant),
                command.getValue().items().stream().map(item -> item.getVariantId()).toList());
        assertEquals(List.of(2, 4), command.getValue().items().stream().map(item -> item.getQuantity()).toList());
    }

    @Test
    void emptyCartDoesNotCreateOrder() {
        var cartId = UUID.randomUUID();
        when(carts.getCart(cartId)).thenReturn(CartResponseDto.builder().id(cartId).items(List.of()).build());

        assertThrows(InvalidParameterException.class, () -> facade.checkout(cartId));

        verifyNoInteractions(orders);
    }

    @Test
    void foreignCartIsRejectedByRealCartOwnershipBoundary() {
        var variants = mock(ProductVariantService.class);
        var realCarts = new CartService(variants, currentUser, profiles);
        var cart = realCarts.createCart(new CartRequestDto(new CartItemRequestDto(UUID.randomUUID(), 2)));
        var realFacade = new CheckoutFacade(realCarts, orders, currentUser, profiles);
        Fixtures.signIn("other");
        when(profiles.requireCompleteProfileByCognitoSub("other")).thenReturn(Fixtures.profile("other"));
        clearInvocations(variants);

        assertThrows(AccessDeniedException.class, () -> realFacade.checkout(cart.getId()));

        verifyNoInteractions(orders, variants);
        Fixtures.signIn("owner");
        assertEquals(2, realCarts.getCart(cart.getId()).getItems().getFirst().getQuantity());
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void checkoutAndOrderReadRequireCompleteProfileBeforeAccess(boolean missing) {
        RuntimeException failure = missing ? new ProfileNotFoundException("missing")
                : new ProfileIncompleteException("incomplete");
        when(profiles.requireCompleteProfileByCognitoSub("owner")).thenThrow(failure);

        assertSame(failure, assertThrows(RuntimeException.class, () -> facade.checkout(UUID.randomUUID())));
        assertSame(failure, assertThrows(RuntimeException.class, () -> facade.getOrder(UUID.randomUUID())));

        verifyNoInteractions(carts, orders);
    }

    @Test
    void orderReadComparesBuyerProfileIdNotOrderIdOrSubject() {
        var id = UUID.randomUUID();
        var response = OrderResponseDto.builder().id(id).buyerId(owner.getId()).build();
        when(orders.findByIdOrElseThrowException(id)).thenReturn(response);

        assertSame(response, facade.getOrder(id));

        response.setBuyerId(UUID.randomUUID());
        assertThrows(AccessDeniedException.class, () -> facade.getOrder(id));
    }
}
