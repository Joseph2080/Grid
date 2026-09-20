package org.bazar.vektrlabs.service;

import org.bazar.vektrlabs.dto.request.AttributeSchemaRequestDto;
import org.bazar.vektrlabs.dto.response.AttributeSchemaResponseDto;
import org.bazar.vektrlabs.entity.AttributeSchema;
import org.jericho.common.service.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface AttributeSchemaService extends Service<AttributeSchema, UUID, AttributeSchemaRequestDto, AttributeSchemaResponseDto> {
    List<AttributeSchemaResponseDto> findAllActive();
    AttributeSchemaResponseDto findByCode(String code);
    AttributeSchema findEntityByCode(String code);
    void validateAttributes(UUID id, Map<String, String> attributes);
}