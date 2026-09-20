package org.bazar.vektrlabs.mapper;

import org.jericho.common.mapper.DtoMapper;
import org.bazar.vektrlabs.dto.request.ProductRequestDto;
import org.bazar.vektrlabs.dto.response.ProductResponseDto;
import org.bazar.vektrlabs.dto.response.ProductVariantResponseDto;
import org.bazar.vektrlabs.entity.Product;
import org.springframework.stereotype.Component;

import java.util.Collections;

@Component
public class ProductMapper implements DtoMapper<
        Product,
        ProductRequestDto,
        ProductResponseDto> {

    @Override
    public Product convertDtoToEntity(ProductRequestDto dto) {
        Product product = new Product();
        updateEntityFromDto(dto, product);
        return product;
    }

    @Override
    public ProductResponseDto convertEntityToResponseDto(Product entity) {
        return ProductResponseDto.builder()
                .id(entity.getId())
                .name(entity.getName())
                .description(entity.getDescription())
                .price(entity.getPrice())
                .active(entity.isActive())
                .storeId(entity.getStore().getId())
                .categoryId(entity.getCategory().getId())
                .variants(entity.getVariants() == null
                        ? Collections.emptyList()
                        : entity.getVariants()
                        .stream()
                        .map(variant -> ProductVariantResponseDto.builder()
                                .id(variant.getId())
                                .productId(entity.getId())
                                .stockQuantity(variant.getStockQuantity())
                                .attributes(variant.getAttributes())
                                .build())
                        .toList())
                .build();
    }

    @Override
    public void updateEntityFromDto(
            ProductRequestDto dto,
            Product entity) {

        entity.setName(dto.name());
        entity.setDescription(dto.description());
        entity.setPrice(dto.price());
        // hmm we might need to specify the type of category first to be honest
        entity.setActive(dto.active() != null && dto.active());
    }
}