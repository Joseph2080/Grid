package org.bazar.vektrlabs.service;

import org.bazar.vektrlabs.dto.request.CategoryRequestDto;
import org.bazar.vektrlabs.dto.response.CategoryResponseDto;
import org.bazar.vektrlabs.entity.Category;
import org.jericho.common.service.Service;

import java.util.List;
import java.util.UUID;

public interface CategoryService extends Service<Category, UUID, CategoryRequestDto, CategoryResponseDto> {
    List<CategoryResponseDto> findByStoreId(UUID storeId);
}
