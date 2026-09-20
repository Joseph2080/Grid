package org.bazar.vektrlabs.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.bazar.vektrlabs.entity.enums.Currency;
import org.bazar.vektrlabs.entity.enums.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class OrderResponseDto {
    private UUID id;
    private UUID buyerId;
    private String paymentUrl;
    private OrderStatus status;
    private BigDecimal totalAmount;
    private Currency currency;
    private List<OrderItemResponseDto> items;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private UUID cartId;
}