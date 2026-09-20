package org.bazar.vektrlabs.controller.rest;


import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.bazar.vektrlabs.dto.request.LocationRequestDto;
import org.bazar.vektrlabs.service.LocationService;
import org.jericho.common.util.RestUtil;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/location")
@RequiredArgsConstructor
public class LocationController {

    private final LocationService locationService;

    @PostMapping
    public ResponseEntity<Map<String, Object>> create(
            @Valid @RequestBody LocationRequestDto dto) {
        return RestUtil.buildResponse(
                locationService.create(dto),
                HttpStatus.CREATED,
                "Location created successfully."
        );
    }

    @GetMapping("/by-store/{storeId}")
    public ResponseEntity<Map<String, Object>> findById(
            @PathVariable UUID storeId) {
        return RestUtil.buildResponse(
                locationService.findAllByStoreId(storeId),
                HttpStatus.OK,
                "Locations by store id retrieved successfully."
        );
    }

}
