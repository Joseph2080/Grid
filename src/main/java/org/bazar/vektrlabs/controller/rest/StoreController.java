package org.bazar.vektrlabs.controller.rest;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.bazar.vektrlabs.dto.request.StoreRequestDto;
import org.bazar.vektrlabs.service.StoreService;
import org.bazar.vektrlabs.facade.store.StoreViewFacade;
import org.jericho.common.util.RestUtil;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/store")
@RequiredArgsConstructor
public class StoreController {

    private final StoreService storeService;
    private final StoreViewFacade storeViewFacade;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> create(
            @Valid @ModelAttribute StoreRequestDto dto) {
        return RestUtil.buildResponse(
                storeService.create(dto),
                HttpStatus.CREATED,
                "store created successfully."
        );
    }

    @GetMapping("/{name}")
    public ResponseEntity<Map<String, Object>> getByStoreName(
            @PathVariable String name,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "12") int size) {
        return RestUtil.buildResponse(
                storeViewFacade.getViewByNameCategory(name, page, size),
                HttpStatus.OK,
                "store fetched successfully."
        );
    }

    @GetMapping("/{name}/products")
    public ResponseEntity<Map<String, Object>> getStoreProducts(
            @PathVariable String name,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(required = false) String categoryId) {
        return RestUtil.buildResponse(
                storeViewFacade.getViewByNameCategory(name, page, size),
                HttpStatus.OK,
                "store products fetched successfully."
        );
    }
}
