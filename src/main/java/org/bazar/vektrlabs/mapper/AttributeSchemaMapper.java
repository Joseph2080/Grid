package org.bazar.vektrlabs.mapper;

import org.bazar.vektrlabs.dto.request.AttributeSchemaRequestDto;
import org.bazar.vektrlabs.dto.response.AttributeSchemaResponseDto;
import org.bazar.vektrlabs.entity.AttributeSchema;
import org.jericho.common.mapper.DtoMapper;
import org.springframework.stereotype.Component;

@Component
public class AttributeSchemaMapper implements DtoMapper<
        AttributeSchema,
        AttributeSchemaRequestDto,
        AttributeSchemaResponseDto> {

    @Override
    public AttributeSchema convertDtoToEntity(
            AttributeSchemaRequestDto dto) {
        AttributeSchema entity = new AttributeSchema();
        updateEntityFromDto(dto, entity);
        return entity;
    }

    @Override
    public AttributeSchemaResponseDto convertEntityToResponseDto(
            AttributeSchema entity) {
        return AttributeSchemaResponseDto.builder()
                .id(entity.getId())
                .code(entity.getCode())
                .name(entity.getName())
                .version(entity.getVersion())
                .schemaJson(entity.getSchemaJson())
                .active(entity.isActive())
                .build();
    }

    @Override
    public void updateEntityFromDto(
            AttributeSchemaRequestDto dto,
            AttributeSchema entity) {

        entity.setCode(dto.code());
        entity.setName(dto.name());
        entity.setVersion(dto.version());
        entity.setSchemaJson(dto.schemaJson());
        entity.setActive(dto.active());
    }
}