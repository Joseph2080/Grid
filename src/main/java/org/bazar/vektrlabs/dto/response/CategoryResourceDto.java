package org.bazar.vektrlabs.dto.response;

import java.util.List;

public record CategoryResourceDto (
        CategoryResponseDto categoryResponseDto,
        List<ProductResponseDto> products){
}
