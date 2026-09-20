package org.bazar.vektrlabs.service.impl;

import org.bazar.vektrlabs.entity.ProductVariant;
import org.jericho.common.exception.EntityNotFoundException;
import org.jericho.common.service.AbstractJpaService;
import org.bazar.vektrlabs.dto.request.OrderItemRequestDto;
import org.bazar.vektrlabs.dto.response.OrderItemResponseDto;
import org.bazar.vektrlabs.entity.OrderItem;
import org.bazar.vektrlabs.exception.OrderItemNotFoundException;
import org.bazar.vektrlabs.mapper.OrderItemMapper;
import org.bazar.vektrlabs.repository.OrderItemRepository;
import org.bazar.vektrlabs.service.OrderItemService;
import org.bazar.vektrlabs.service.ProductVariantService;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class OrderItemServiceImpl extends AbstractJpaService<
        OrderItem,
        UUID,
        OrderItemRequestDto,
        OrderItemResponseDto,
        OrderItemRepository>
        implements OrderItemService {

    private final ProductVariantService productVariantService;

    public OrderItemServiceImpl(
            OrderItemRepository repository,
            OrderItemMapper dtoMapper,
            ProductVariantService productVariantService) {
        super(repository, dtoMapper);
        this.productVariantService = productVariantService;
    }

    //not really needed because order items are created through the order service, but we can keep it for now
    @Override
    protected void setEntityDependencies(
            OrderItem entity,
            OrderItemRequestDto dto) {
        ProductVariant productVariant =productVariantService
                .findAvailableVariant(dto.getVariantId(), dto.getQuantity());
        entity.setVariant(productVariant);
        entity.setUnitPrice(productVariant.getProduct().getPrice());
    }

    @Override
    public OrderItemResponseDto create(OrderItemRequestDto orderItemRequestDto){
        throw new UnsupportedOperationException("OrderItemServiceImpl does not support create operation directly. Use OrderService to create order items.");
    }

    @Override
    protected EntityNotFoundException entityNotFoundException() {
        return new OrderItemNotFoundException(
                "ORDER_ITEM_NOT_FOUND"
        );
    }
}