package org.bazar.vektrlabs.controller.rest;

import lombok.RequiredArgsConstructor;
import org.jericho.common.util.RestUtil;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/health")
@RequiredArgsConstructor
public class HealthCheckController {

    @GetMapping
    public ResponseEntity<Map<String, Object>> getHealthCheck(){
        return RestUtil.buildResponse(
                "System is up an running",
                HttpStatus.OK,
                "Location created successfully."
        );
    }

}
