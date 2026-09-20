package org.bazar.vektrlabs.controller.rest;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.bazar.vektrlabs.dto.request.OrderRequestDto;
import org.bazar.vektrlabs.facade.CheckoutFacade;
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
@RequestMapping("/api/v1/order")
@RequiredArgsConstructor
public class OrderController {

    private final CheckoutFacade checkoutFacade;

    @PostMapping
    public ResponseEntity<Map<String, Object>> create(
            @Valid @RequestBody OrderRequestDto dto) {
        return RestUtil.buildResponse(
                checkoutFacade.checkout(dto.getCartId()),
                HttpStatus.CREATED,
                "Order created successfully."
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getById(@PathVariable UUID id) {
        return RestUtil.buildResponse(
                checkoutFacade.getOrder(id),
                HttpStatus.OK,
                "Orders retrieved successfully."
        );
    }
}
