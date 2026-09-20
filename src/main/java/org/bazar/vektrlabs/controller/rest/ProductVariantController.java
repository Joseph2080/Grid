package org.bazar.vektrlabs.controller.rest;

 import jakarta.validation.Valid;
 import lombok.RequiredArgsConstructor;
 import org.jericho.common.util.RestUtil;
import org.bazar.vektrlabs.dto.request.ProductVariantRequestDto;
import org.bazar.vektrlabs.service.ProductVariantService;
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

@RestController
@RequestMapping("/api/v1/product-variants")
@RequiredArgsConstructor
public class ProductVariantController {

    private final ProductVariantService productVariantService;

    @PostMapping
    public ResponseEntity<Map<String, Object>> create(
            @Valid @RequestBody ProductVariantRequestDto dto) {
        return RestUtil.buildResponse(
                productVariantService.create(dto),
                HttpStatus.CREATED,
                "Product variant created successfully."
        );
    }

    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> update(
            @PathVariable UUID id,
            @Valid @RequestBody ProductVariantRequestDto dto) {
        return RestUtil.buildResponse(
                productVariantService.update(id, dto),
                HttpStatus.OK,
                "Product variant updated successfully."
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> findById(
            @PathVariable UUID id) {
        return RestUtil.buildResponse(
                productVariantService.findByIdOrElseThrowException(id),
                HttpStatus.OK,
                "Product variant retrieved successfully."
        );
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> findAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return RestUtil.buildResponse(
                productVariantService.findAll(PageRequest.of(page, size)),
                HttpStatus.OK,
                "Product variants retrieved successfully."
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> delete(
            @PathVariable UUID id) {
        productVariantService.deleteById(id);
        return RestUtil.buildResponse(
                null,
                HttpStatus.OK,
                "Product variant deleted successfully."
        );
    }
}