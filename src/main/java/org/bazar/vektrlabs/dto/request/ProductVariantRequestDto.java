package org.bazar.vektrlabs.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class ProductVariantRequestDto {
    @NotNull(message = "Product ID cannot be null")
    private UUID productId;
    @NotNull(message = "Variant name cannot be null")
    @Min(value =0 , message = "Stock quantity must be at least 1")
    private Integer stockQuantity;
    @Builder.Default
    private Map<String, String> attributes = new HashMap<>();}