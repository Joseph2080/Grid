package org.bazar.vektrlabs.cart;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class CartResponseDto {
    private UUID id;
    private List<CartItemResponseDto> items;
    private Instant expiresAt;
}