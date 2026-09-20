package org.bazar.vektrlabs.mapper;

import org.jericho.common.mapper.DtoMapper;
import org.bazar.vektrlabs.dto.request.ProductVariantRequestDto;
import org.bazar.vektrlabs.dto.response.ProductVariantResponseDto;
import org.bazar.vektrlabs.entity.ProductVariant;
import org.springframework.stereotype.Component;

import java.util.HashMap;

@Component
public class ProductVariantMapper implements DtoMapper<
        ProductVariant,
        ProductVariantRequestDto,
        ProductVariantResponseDto> {

    @Override
    public ProductVariant convertDtoToEntity(
            ProductVariantRequestDto dto) {
        ProductVariant variant = new ProductVariant();
        updateEntityFromDto(dto, variant);
        return variant;
    }

    @Override
    public ProductVariantResponseDto convertEntityToResponseDto(
            ProductVariant entity) {
        return ProductVariantResponseDto.builder()
                .id(entity.getId())
                .productId(entity.getProduct().getId())
                .stockQuantity(entity.getStockQuantity())
                .attributes(entity.getAttributes())
                .build();
    }

    @Override
    public void updateEntityFromDto(
            ProductVariantRequestDto dto,
            ProductVariant entity) {
        entity.setStockQuantity(dto.getStockQuantity());
        entity.setAttributes(
                dto.getAttributes() != null
                        ? new HashMap<>(dto.getAttributes())
                        : new HashMap<>()
        );    }
}