package org.bazar.vektrlabs.service;

import org.jericho.common.service.Service;
import org.bazar.vektrlabs.dto.request.ProductVariantRequestDto;
import org.bazar.vektrlabs.dto.response.ProductVariantResponseDto;
import org.bazar.vektrlabs.entity.ProductVariant;

import java.util.List;
import java.util.UUID;

public interface ProductVariantService extends Service<
        ProductVariant,
        UUID,
        ProductVariantRequestDto,
        ProductVariantResponseDto> {
        List<ProductVariantResponseDto> findByProductId(UUID productId);
        void updateVariantStock(UUID productId, int quantity);
        ProductVariant findAvailableVariant(UUID productId, int requestedQuantity);
}