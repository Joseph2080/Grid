package org.bazar.vektrlabs.cart;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.UUID;

@Data
@AllArgsConstructor
public class CartItemResponseDto {
    private UUID variantId;
    private Integer quantity;
}