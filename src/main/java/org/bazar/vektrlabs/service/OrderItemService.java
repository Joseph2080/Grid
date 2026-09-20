package org.bazar.vektrlabs.service;

import org.jericho.common.service.Service;
import org.bazar.vektrlabs.dto.request.OrderItemRequestDto;
import org.bazar.vektrlabs.dto.response.OrderItemResponseDto;
import org.bazar.vektrlabs.entity.OrderItem;

import java.util.UUID;

public interface OrderItemService extends Service<
        OrderItem,
        UUID,
        OrderItemRequestDto,
        OrderItemResponseDto> {
}