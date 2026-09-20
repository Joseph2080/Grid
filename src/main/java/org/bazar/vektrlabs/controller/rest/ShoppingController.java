package org.bazar.vektrlabs.controller.rest;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.bazar.vektrlabs.cart.CartItemRequestDto;
import org.bazar.vektrlabs.cart.CartRequestDto;
import org.bazar.vektrlabs.cart.CartService;

import org.bazar.vektrlabs.facade.CheckoutFacade;
import org.jericho.common.util.RestUtil;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/shopping/cart")
@RequiredArgsConstructor
public class ShoppingController {

    private final CartService cartService;
    private final CheckoutFacade checkoutFacade;

    @PostMapping
    public ResponseEntity<Map<String, Object>> createCart(
            @Valid @RequestBody CartRequestDto cartRequestDto) {

        return RestUtil.buildResponse(
                cartService.createCart(cartRequestDto),
                HttpStatus.CREATED,
                "Cart created successfully."
        );
    }

    @GetMapping("/{cartId}")
    public ResponseEntity<Map<String, Object>> getCart(
            @PathVariable UUID cartId) {

        return RestUtil.buildResponse(
                cartService.getCart(cartId),
                HttpStatus.OK,
                "Cart retrieved successfully."
        );
    }

    @PostMapping("/{cartId}/items")
    public ResponseEntity<Map<String, Object>> addItem(
            @PathVariable UUID cartId,
            @Valid @RequestBody CartItemRequestDto requestDto) {

        return RestUtil.buildResponse(
                cartService.addItem(cartId, requestDto),
                HttpStatus.OK,
                "Item added to cart successfully."
        );
    }

    @PutMapping("/{cartId}/items/{variantId}")
    public ResponseEntity<Map<String, Object>> updateQuantity(
            @PathVariable UUID cartId,
            @PathVariable UUID variantId,
            @RequestParam Integer quantity) {

        return RestUtil.buildResponse(
                cartService.updateQuantity(cartId, variantId, quantity),
                HttpStatus.OK,
                "Cart item quantity updated successfully."
        );
    }

    @DeleteMapping("/{cartId}/items/{variantId}")
    public ResponseEntity<Map<String, Object>> removeItem(
            @PathVariable UUID cartId,
            @PathVariable UUID variantId) {

        return RestUtil.buildResponse(
                cartService.removeItem(cartId, variantId),
                HttpStatus.OK,
                "Item removed from cart successfully."
        );
    }

    @DeleteMapping("/{cartId}")
    public ResponseEntity<Map<String, Object>> clearCart(
            @PathVariable UUID cartId) {

        cartService.clear(cartId);
        return RestUtil.buildResponse(
                null,
                HttpStatus.OK,
                "Cart cleared successfully."
        );
    }

    @PostMapping("/{cartId}/checkout")
    public ResponseEntity<Map<String, Object>> checkout(
            @PathVariable UUID cartId) {

        return RestUtil.buildResponse(
                checkoutFacade.checkout(cartId),
                HttpStatus.CREATED,
                "Checkout successful, order created."
        );
    }
}