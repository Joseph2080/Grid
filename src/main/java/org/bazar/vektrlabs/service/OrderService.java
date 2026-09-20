package org.bazar.vektrlabs.service;

import org.bazar.vektrlabs.entity.enums.OrderStatus;
import org.jericho.common.service.Service;
import org.bazar.vektrlabs.dto.request.OrderCommand;
import org.bazar.vektrlabs.dto.response.OrderResponseDto;
import org.bazar.vektrlabs.entity.Order;

import java.util.UUID;

public interface OrderService extends Service<
        Order,
        UUID,
        OrderCommand,
        OrderResponseDto> {
    OrderResponseDto updateOrderStatus(UUID orderId, OrderStatus orderStatus);
}