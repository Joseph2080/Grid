package org.bazar.vektrlabs.mapper;

import org.jericho.common.mapper.DtoMapper;
import org.bazar.vektrlabs.dto.request.OrderItemRequestDto;
import org.bazar.vektrlabs.dto.response.OrderItemResponseDto;
import org.bazar.vektrlabs.entity.OrderItem;
import org.springframework.stereotype.Component;

@Component
public class OrderItemMapper implements DtoMapper<
        OrderItem,
        OrderItemRequestDto,
        OrderItemResponseDto> {

    @Override
    public OrderItem convertDtoToEntity(
            OrderItemRequestDto dto) {
        OrderItem orderItem = new OrderItem();
        updateEntityFromDto(dto, orderItem);
        return orderItem;
    }

    @Override
    public OrderItemResponseDto convertEntityToResponseDto(
            OrderItem entity) {

        return OrderItemResponseDto.builder()
                .id(entity.getId())
                .orderId(entity.getOrder().getId())
                .variantId(entity.getVariant().getId())
                .quantity(entity.getQuantity())
                .unitPrice(entity.getUnitPrice())
                .build();
    }

    @Override
    public void updateEntityFromDto(
            OrderItemRequestDto dto,
            OrderItem entity) {
        entity.setQuantity(dto.getQuantity());
    }
}