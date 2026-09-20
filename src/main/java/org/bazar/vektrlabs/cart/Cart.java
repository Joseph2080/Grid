package org.bazar.vektrlabs.cart;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Cart {
    private UUID id;
    private String buyerCognitoSub;
    private List<CartItem> items = new ArrayList<>();
    private Instant expiresAt;
}