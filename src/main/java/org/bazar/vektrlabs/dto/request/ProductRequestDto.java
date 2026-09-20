package org.bazar.vektrlabs.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import jakarta.validation.constraints.Size;
import lombok.Builder;

import java.math.BigDecimal;
import java.util.UUID;

@Builder
public record ProductRequestDto (
    @NotBlank(message = "Product name is required")
    String name,
    @NotBlank(message = "Product description is required")
    @Size(max = 500, message = "Description must not exceed 500 characters")
    String description,
    @NotNull
    @DecimalMin("0.00")
    BigDecimal price,
    Boolean active,
    UUID storeId,
    UUID categoryId,
    @NotNull(message = "Product schema code is required")
    String attributeSchemaCode)
{ }