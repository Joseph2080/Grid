package org.bazar.vektrlabs.service;

import org.jericho.common.service.Service;
import org.bazar.vektrlabs.dto.request.ProductRequestDto;
import org.bazar.vektrlabs.dto.response.ProductResponseDto;
import org.bazar.vektrlabs.entity.Product;

import java.util.List;
import java.util.UUID;

public interface ProductService extends Service<
        Product,
        UUID,
        ProductRequestDto,
        ProductResponseDto> {
     List<ProductResponseDto> findAllByStoreId(UUID id);

     List<ProductResponseDto> findByCategoryId(UUID categoryId);
}