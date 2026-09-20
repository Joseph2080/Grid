package org.bazar.vektrlabs.dto;
import lombok.Builder;
import java.util.List;
@Builder
public record ProductAttributeResource(
        String name,
        String type,
        List<String> values,
        boolean selectable,
        String selectedValue
) {}