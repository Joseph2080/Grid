package org.bazar.vektrlabs.dto.request;

import lombok.Builder;

import java.util.List;
import java.util.UUID;

/** Internal checkout input; identity and items are supplied by CheckoutFacade. */
@Builder
public record OrderCommand(UUID cartId, List<OrderItemRequestDto> items, String buyerCognitoSub) {
}
