package org.bazar.vektrlabs.dto.response;

import java.util.List;

public record StoreViewResponse(
        StoreResponseDto storeResponseDto,
        List<CategoryResourceDto> categoryResourceDtos,
        List<LocationResponseDto> locationResponseDtos,
        List<ProductResponseDto> productResponseDtos,
        int page,
        int pageSize,
        long totalProducts,
        int totalPages,
        boolean hasNext,
        boolean hasPrevious
) {
}
