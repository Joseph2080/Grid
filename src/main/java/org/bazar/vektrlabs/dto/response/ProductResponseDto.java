package org.bazar.vektrlabs.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.bazar.vektrlabs.dto.ProductAttributeResource;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class ProductResponseDto {
    private UUID id;
    private String name;
    private String description;
    private BigDecimal price;
    private Boolean active;
    private UUID storeId;
    private UUID categoryId;
    private List<ProductMediaResourceResponseDto> productMediaResources;
    private List<ProductVariantResponseDto> variants;
    private List<ProductAttributeResource> attributes;
}