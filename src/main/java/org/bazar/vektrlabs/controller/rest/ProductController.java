package org.bazar.vektrlabs.controller.rest;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.bazar.vektrlabs.dto.request.ProductRequestDto;
import org.bazar.vektrlabs.facade.ProductResourceFacade;
import org.bazar.vektrlabs.service.ProductService;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;
import java.util.UUID;

import org.jericho.common.util.RestUtil;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;
    private final ProductResourceFacade productFacade;

    @PostMapping
    public ResponseEntity<Map<String, Object>> create(
            @Valid @RequestBody ProductRequestDto dto) {
        return RestUtil.buildResponse(
                productService.create(dto),
                HttpStatus.CREATED,
                "Product created successfully."
        );
    }

    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> update(
            @PathVariable UUID id,
            @Valid @RequestBody ProductRequestDto dto) {
        return RestUtil.buildResponse(
                productService.update(id, dto),
                HttpStatus.OK,
                "Product updated successfully."
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> findById(
            @PathVariable UUID id) {
        return RestUtil.buildResponse(
                productFacade.findById(id),
                HttpStatus.OK,
                "Product retrieved successfully."
        );
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> findAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return RestUtil.buildResponse(
                productFacade.findAll(PageRequest.of(page, size)),
                HttpStatus.OK,
                "Products retrieved successfully."
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> delete(
            @PathVariable UUID id) {
        productService.deleteById(id);
        return RestUtil.buildResponse(
                null,
                HttpStatus.OK,
                "Product deleted successfully."
        );
    }
}