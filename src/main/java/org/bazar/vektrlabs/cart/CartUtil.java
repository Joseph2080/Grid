package org.bazar.vektrlabs.cart;

import lombok.experimental.UtilityClass;

import java.util.List;

@UtilityClass
public final class CartUtil {

    public static CartItem toCartItem(CartItemRequestDto requestDto) {
        if (requestDto == null) {
            return null;
        }
        return CartItem.builder()
                .variantId(requestDto.getVariantId())
                .quantity(requestDto.getQuantity())
                .build();
    }

    public static CartResponseDto toCartResponseDto(Cart cart) {
        if (cart == null) {
            return null;
        }
        List<CartItemResponseDto> itemDtos = cart.getItems().stream()
                .map(item -> new CartItemResponseDto(item.getVariantId(), item.getQuantity()))
                .toList();

        return CartResponseDto.builder()
                .id(cart.getId())
                .items(itemDtos)
                .expiresAt(cart.getExpiresAt())
                .build();
    }
}