package org.bazar.vektrlabs.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class OrderItemResponseDto {
    private UUID id;
    private UUID orderId;
    private UUID variantId;
    private Integer quantity;
    private BigDecimal unitPrice;
}