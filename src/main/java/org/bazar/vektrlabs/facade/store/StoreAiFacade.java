package org.bazar.vektrlabs.facade.store;

import lombok.RequiredArgsConstructor;
import org.bazar.vektrlabs.cart.CartItemRequestDto;
import org.bazar.vektrlabs.cart.CartRequestDto;
import org.bazar.vektrlabs.cart.CartResponseDto;
import org.bazar.vektrlabs.cart.CartService;
import org.bazar.vektrlabs.dto.response.OrderResponseDto;
import org.bazar.vektrlabs.dto.response.ProductResponseDto;
import org.bazar.vektrlabs.facade.CheckoutFacade;
import org.bazar.vektrlabs.facade.ProductResourceFacade;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StoreAiFacade {

    private final ProductResourceFacade productResourceFacade;
    private final CartService cartService;
    private final CheckoutFacade checkoutFacade;

    public List<ProductResponseDto> findProducts(UUID storeId) {
        return productResourceFacade.findAllByStoreId(storeId);
    }

    public ProductResponseDto findProduct(UUID productId) {
        return productResourceFacade.findById(productId);
    }

    public CartResponseDto getCart(UUID cartId) {
        return cartService.getCart(cartId);
    }

    public CartResponseDto createCart(
            UUID variantId,
            Integer quantity) {
        return cartService.createCart(
                CartRequestDto.builder()
                        .item(
                                CartItemRequestDto.builder()
                                        .variantId(variantId)
                                        .quantity(quantity)
                                        .build()
                        )
                        .build()
        );
    }

    public CartResponseDto addToCart(
            UUID cartId,
            UUID variantId,
            Integer quantity) {

        if (cartId == null) {
            return cartService.createCart(
                    CartRequestDto.builder()
                            .item(
                                    CartItemRequestDto.builder()
                                            .variantId(variantId)
                                            .quantity(quantity)
                                            .build()
                            )
                            .build()
            );
        }

        return cartService.addItem(
                cartId,
                CartItemRequestDto.builder()
                        .variantId(variantId)
                        .quantity(quantity)
                        .build()
        );
    }

    public OrderResponseDto checkout(UUID cartId) {
        return checkoutFacade.checkout(cartId);
    }

    public OrderResponseDto getOrder(UUID orderId) {
        return checkoutFacade.getOrder(orderId);
    }
}