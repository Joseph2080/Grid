package org.bazar.vektrlabs.mapper;

import org.jericho.common.mapper.DtoMapper;
import org.bazar.vektrlabs.dto.request.ProductMediaResourceRequestDto;
import org.bazar.vektrlabs.dto.response.ProductMediaResourceResponseDto;
import org.bazar.vektrlabs.entity.ProductMediaResource;
import org.springframework.stereotype.Component;

@Component
public class ProductMediaResourceMapper implements DtoMapper<
        ProductMediaResource,
        ProductMediaResourceRequestDto,
        ProductMediaResourceResponseDto> {

    @Override
    public ProductMediaResource convertDtoToEntity(ProductMediaResourceRequestDto dto) {
        ProductMediaResource entity = new ProductMediaResource();
        updateEntityFromDto(dto, entity);
        return entity;
    }

    @Override
    public ProductMediaResourceResponseDto convertEntityToResponseDto(ProductMediaResource entity) {
        return ProductMediaResourceResponseDto.builder()
                .id(entity.getId())
                .productId(entity.getProduct().getId())
                .sortOrder(entity.getSortOrder())
                .primaryImage(entity.isPrimaryImage())
                .build();
    }

    @Override
    public void updateEntityFromDto(ProductMediaResourceRequestDto dto, ProductMediaResource entity) {
        //handled in the service layer
    }
}
