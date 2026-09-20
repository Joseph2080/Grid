package org.bazar.vektrlabs.controller.rest;

import lombok.RequiredArgsConstructor;
import org.bazar.vektrlabs.dto.request.AttributeSchemaRequestDto;
import org.bazar.vektrlabs.dto.response.AttributeSchemaResponseDto;
import org.bazar.vektrlabs.service.AttributeSchemaService;
import org.jericho.common.util.RestUtil;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/attribute-schemas")
@RequiredArgsConstructor
public class AttributeSchemaController {

    private final AttributeSchemaService attributeSchemaService;

    @GetMapping
    public ResponseEntity<List<AttributeSchemaResponseDto>> findAllActive() {
        return ResponseEntity.ok(
                attributeSchemaService.findAllActive()
        );
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> create(
            @RequestBody AttributeSchemaRequestDto requestDto
    ) {
        return RestUtil.buildResponse(
                attributeSchemaService.create(requestDto),
                HttpStatus.CREATED,
                "attribute created successfully"
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<AttributeSchemaResponseDto> findById(
            @PathVariable UUID id) {
        return ResponseEntity.ok(
                attributeSchemaService.findByIdOrElseThrowException(id)
        );
    }

    @GetMapping("/code/{code}")
    public ResponseEntity<AttributeSchemaResponseDto> findByCode(
            @PathVariable String code) {
        return ResponseEntity.ok(
                attributeSchemaService.findByCode(code)
        );
    }
}