package org.bazar.vektrlabs.facade;

import lombok.RequiredArgsConstructor;
import org.bazar.vektrlabs.dto.response.CategoryResourceDto;
import org.bazar.vektrlabs.dto.response.CategoryResponseDto;
import org.bazar.vektrlabs.dto.response.ProductResponseDto;
import org.bazar.vektrlabs.service.CategoryService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoryResourceFacade {

    private final CategoryService categoryService;

    public List<CategoryResourceDto> findAllByStoreId(
            UUID storeId,
            List<ProductResponseDto> products) {
        List<CategoryResponseDto> categories =
                categoryService.findByStoreId(storeId);
        Map<UUID, List<ProductResponseDto>> productsByCategory =
                products.stream()
                        .filter(product ->product.getCategoryId() != null)
                        .collect(Collectors.groupingBy(
                                ProductResponseDto::getCategoryId
                        ));
        return categories.stream()
                .map(category -> new CategoryResourceDto(
                        category,
                        productsByCategory.getOrDefault(
                                category.getId(),
                                List.of()
                        )
                ))
                .toList();
    }
}