package org.bazar.vektrlabs.service;

import org.jericho.common.service.Service;
import org.bazar.vektrlabs.dto.request.ProductMediaResourceRequestDto;
import org.bazar.vektrlabs.dto.response.ProductMediaResourceResponseDto;
import org.bazar.vektrlabs.entity.ProductMediaResource;

import java.util.List;
import java.util.UUID;

public interface ProductMediaResourceService extends Service<
        ProductMediaResource,
        UUID,
        ProductMediaResourceRequestDto,
        ProductMediaResourceResponseDto> {

    List<ProductMediaResourceResponseDto> findByProductId(UUID productId);

    List<ProductMediaResourceResponseDto> findByProductIds(List<UUID> productIds);
}
