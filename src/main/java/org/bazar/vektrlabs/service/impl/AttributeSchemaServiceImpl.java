package org.bazar.vektrlabs.service.impl;

import org.bazar.vektrlabs.dto.request.AttributeSchemaRequestDto;
import org.bazar.vektrlabs.dto.response.AttributeSchemaResponseDto;
import org.bazar.vektrlabs.entity.AttributeSchema;
import org.bazar.vektrlabs.exception.AttributeSchemaNotFoundException;
import org.bazar.vektrlabs.mapper.AttributeSchemaMapper;
import org.bazar.vektrlabs.repository.AttributeSchemaRepository;
import org.bazar.vektrlabs.service.AttributeSchemaService;
import org.bazar.vektrlabs.validation.AttributeSchemaValidator;
import org.jericho.common.exception.EntityNotFoundException;
import org.jericho.common.service.AbstractJpaService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class AttributeSchemaServiceImpl extends AbstractJpaService<
        AttributeSchema,
        UUID,
        AttributeSchemaRequestDto,
        AttributeSchemaResponseDto,
        AttributeSchemaRepository>
        implements AttributeSchemaService {

    private final AttributeSchemaValidator schemaValidator;

    public AttributeSchemaServiceImpl(
            AttributeSchemaRepository repository,
            AttributeSchemaMapper dtoMapper,
            AttributeSchemaValidator schemaValidator) {
        super(repository, dtoMapper);
        this.schemaValidator = schemaValidator;
    }

    @Override
    public List<AttributeSchemaResponseDto> findAllActive() {
        return repository.findByActiveTrue()
                .stream()
                .map(dtoMapper::convertEntityToResponseDto)
                .toList();
    }

    @Override
    public AttributeSchemaResponseDto findByCode(String code) {
        return dtoMapper.convertEntityToResponseDto(
                findEntityByCode(code)
        );
    }

    @Override
    public AttributeSchema findEntityByCode(String code) {
        return repository.findByCodeAndActiveTrue(code)
                .orElseThrow(() ->
                        new AttributeSchemaNotFoundException(
                                "Active attribute schema not found with code: " + code
                        )
                );
    }

    @Transactional(readOnly = true)
    @Override
    public void validateAttributes(UUID id, Map<String, String> attributes){
        var schema = findEntityByIdOrElseThrowException(id);
        schemaValidator.validate(schema.getSchemaJson(), attributes);
    }

    @Override
    protected EntityNotFoundException entityNotFoundException() {
        return new AttributeSchemaNotFoundException(
                "Attribute schema not found"
        );
    }
}