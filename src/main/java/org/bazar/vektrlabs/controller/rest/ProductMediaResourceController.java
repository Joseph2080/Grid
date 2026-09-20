package org.bazar.vektrlabs.controller.rest;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.bazar.vektrlabs.dto.request.ProductMediaResourceRequestDto;
import org.bazar.vektrlabs.service.ProductMediaResourceService;
import org.jericho.common.util.RestUtil;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/product-media-resources")
@RequiredArgsConstructor
public class ProductMediaResourceController {

    private final ProductMediaResourceService productMediaResourceService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> create(
            @Valid @ModelAttribute ProductMediaResourceRequestDto dto) {
        return RestUtil.buildResponse(
                productMediaResourceService.create(dto),
                HttpStatus.CREATED,
                "Product media resource created successfully."
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> findById(
            @PathVariable UUID id) {
        return RestUtil.buildResponse(
                productMediaResourceService.findByIdOrElseThrowException(id),
                HttpStatus.OK,
                "Product media resource retrieved successfully."
        );
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> findAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return RestUtil.buildResponse(
                productMediaResourceService.findAll(PageRequest.of(page, size)),
                HttpStatus.OK,
                "Product media resources retrieved successfully."
        );
    }

    @GetMapping("/product/{productId}")
    public ResponseEntity<Map<String, Object>> findByProductId(
            @PathVariable UUID productId) {
        return RestUtil.buildResponse(
                productMediaResourceService.findByProductId(productId),
                HttpStatus.OK,
                "Product media resources retrieved successfully for product."
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> delete(
            @PathVariable UUID id) {
        productMediaResourceService.deleteById(id);
        return RestUtil.buildResponse(
                null,
                HttpStatus.OK,
                "Product media resource deleted successfully."
        );
    }
}
