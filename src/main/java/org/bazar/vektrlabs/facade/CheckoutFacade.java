package org.bazar.vektrlabs.facade;

import lombok.RequiredArgsConstructor;
import org.bazar.vektrlabs.cart.CartItemResponseDto;
import org.bazar.vektrlabs.cart.CartResponseDto;
import org.bazar.vektrlabs.cart.CartService;
import org.bazar.vektrlabs.dto.request.OrderItemRequestDto;
import org.bazar.vektrlabs.dto.request.OrderCommand;
import org.bazar.vektrlabs.dto.response.OrderResponseDto;
import org.bazar.vektrlabs.service.OrderService;
import org.bazar.vektrlabs.service.UserProfileService;
import org.bazar.vektrlabs.util.CurrentUserUtil;
import org.jericho.common.exception.InvalidParameterException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@RequiredArgsConstructor
@Component
public class CheckoutFacade {

    private final CartService cartService;
    private final OrderService orderService;
    private final CurrentUserUtil currentUserUtil;
    private final UserProfileService userProfileService;

    @Transactional
    public OrderResponseDto checkout(UUID cartId) {
        String sub = currentUserUtil.currentCognitoSub();
        userProfileService.requireCompleteProfileByCognitoSub(sub);
        CartResponseDto cart = cartService.getCart(cartId);
        if (cart.getItems().isEmpty()) {
            throw new InvalidParameterException("Cannot checkout an empty cart.");
        }
        return orderService.create(
                OrderCommand.builder()
                        .buyerCognitoSub(sub)
                        .cartId(cart.getId())
                        .items(
                                cart.getItems().stream()
                                        .map(this::mapToOrderItem)
                                        .toList()
                        )
                        .build());
    }

    @Transactional(readOnly = true)
    public OrderResponseDto getOrder(UUID orderId) {
        var profile = userProfileService.requireCompleteProfileByCognitoSub(currentUserUtil.currentCognitoSub());
        var order = orderService.findByIdOrElseThrowException(orderId);
        if (!profile.getId().equals(order.getBuyerId())) {
            throw new AccessDeniedException("This order belongs to another profile.");
        }
        return order;
    }

    private OrderItemRequestDto mapToOrderItem(CartItemResponseDto cartItem) {
        return OrderItemRequestDto.builder()
                .variantId(cartItem.getVariantId())
                .quantity(cartItem.getQuantity())
                .build();
    }
}
