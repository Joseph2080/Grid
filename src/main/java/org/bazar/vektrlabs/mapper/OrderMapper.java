package org.bazar.vektrlabs.mapper;

import lombok.RequiredArgsConstructor;
import org.bazar.vektrlabs.entity.enums.OrderStatus;
import org.jericho.common.mapper.DtoMapper;
import org.bazar.vektrlabs.dto.request.OrderCommand;
import org.bazar.vektrlabs.dto.response.OrderResponseDto;
import org.bazar.vektrlabs.entity.Order;
import org.springframework.stereotype.Component;

import java.util.Collections;

@Component
@RequiredArgsConstructor
public class OrderMapper implements DtoMapper<
        Order,
        OrderCommand,
        OrderResponseDto> {

    private final OrderItemMapper orderItemMapper;

    @Override
    public Order convertDtoToEntity(OrderCommand dto) {
        Order order = new Order();
        updateEntityFromDto(dto, order);
        return order;
    }

    @Override
    public OrderResponseDto convertEntityToResponseDto(Order entity) {

        return OrderResponseDto.builder()
                .id(entity.getId())
                .buyerId(entity.getBuyer().getId())
                .cartId(entity.getCartId())
                .status(entity.getStatus())
                .totalAmount(entity.getTotalAmount())
                .currency(entity.getCurrency())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .items(entity.getItems() == null
                        ? Collections.emptyList()
                        : entity.getItems()
                        .stream()
                        .map(orderItemMapper::convertEntityToResponseDto)
                        .toList())
                .build();
    }

    @Override
    public void updateEntityFromDto(
            OrderCommand dto,
            Order entity) {
        if (entity.getStatus() == null) {
            entity.setStatus(OrderStatus.PENDING);
        }
        entity.setCartId(dto.cartId());
    }
}