package org.bazar.vektrlabs.controller.rest;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.bazar.vektrlabs.dto.request.ChatRequestDto;
import org.bazar.vektrlabs.ai.dto.response.ChatResponseDto;
import org.bazar.vektrlabs.ai.facade.StoreAiChatFacade;
import org.jericho.common.util.RestUtil;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/ai/chat")
@RequiredArgsConstructor
public class ChatVektrController {

    private final StoreAiChatFacade storeAiChatFacade;

    @PostMapping
    public ResponseEntity<Map<String, Object>> chat(
            @Valid @RequestBody ChatRequestDto request) {
        ChatResponseDto response =
                storeAiChatFacade.chat(request);
        return RestUtil.buildResponse(
                response,
                HttpStatus.OK,
                "AI response generated successfully."
        );
    }
}