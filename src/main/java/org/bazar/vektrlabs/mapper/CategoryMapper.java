package org.bazar.vektrlabs.mapper;

import org.jericho.common.mapper.DtoMapper;
import org.bazar.vektrlabs.dto.request.CategoryRequestDto;
import org.bazar.vektrlabs.dto.response.CategoryResponseDto;
import org.bazar.vektrlabs.entity.Category;
import org.springframework.stereotype.Component;

@Component
public class CategoryMapper implements DtoMapper<
        Category,
        CategoryRequestDto,
        CategoryResponseDto> {

    @Override
    public Category convertDtoToEntity(
            CategoryRequestDto dto) {

        Category category = new Category();
        updateEntityFromDto(dto, category);
        return category;
    }

    @Override
    public CategoryResponseDto convertEntityToResponseDto(
            Category entity) {

        return CategoryResponseDto.builder()
                .id(entity.getId())
                .name(entity.getName())
                .storeId(entity.getStore().getId())
                .build();
    }

    @Override
    public void updateEntityFromDto(
            CategoryRequestDto dto,
            Category entity) {

        entity.setName(dto.name());
    }
}