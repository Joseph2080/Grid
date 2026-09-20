package org.bazar.vektrlabs.util;

import org.bazar.vektrlabs.dto.ProductAttributeResource;
import org.bazar.vektrlabs.dto.response.ProductVariantResponseDto;
import org.springframework.stereotype.Component;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
@Component
public class ProductAttributeResourceBuilder {

    public List<ProductAttributeResource> buildAttributes(List<ProductVariantResponseDto> variants) {
        if (variants == null || variants.isEmpty()) {
            return List.of();
        }
        Map<String, LinkedHashSet<String>> valuesByAttribute = new LinkedHashMap<>();
        variants.forEach(variant ->
                variant.getAttributes().forEach((name, value) ->
                        valuesByAttribute
                                .computeIfAbsent(name, key -> new LinkedHashSet<>())
                                .add(value)
                )
        );
        return valuesByAttribute.entrySet().stream()
                .map(entry -> {
                    List<String> values = new ArrayList<>(entry.getValue());
                    return ProductAttributeResource.builder()
                            .name(entry.getKey())
                            .type(resolveAttributeType(entry.getKey(), values))
                            .values(values)
                            .selectable(values.size() > 1)
                            .selectedValue(values.size() == 1 ? values.getFirst() : null)
                            .build();
                })
                .toList();
    }

    private String resolveAttributeType(String name, List<String> values) {
        if ("colour".equalsIgnoreCase(name)
                || "color".equalsIgnoreCase(name)) {
            return "COLOUR";
        }
        if (!values.isEmpty() && values.stream().allMatch(this::isHexColour)) {
            return "COLOUR";
        }
        return "SELECT";
    }

    private boolean isHexColour(String value) {
        return value != null && value.matches("^#[0-9A-Fa-f]{6}$");
    }
}