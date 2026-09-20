package org.bazar.vektrlabs.ai.controller;

import lombok.RequiredArgsConstructor;
import org.bazar.vektrlabs.ai.dto.request.ChatRequest;
import org.bazar.vektrlabs.ai.dto.response.ChatResponse;
import org.bazar.vektrlabs.ai.service.ChatService;
import org.jericho.common.util.RestUtil;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @PostMapping
    public ResponseEntity<Map<String, Object>> chat(@RequestBody ChatRequest request) {
        String response = chatService.chat(request.message());
        ChatResponse chatResponse = ChatResponse.builder().response(response).build();
        return RestUtil.buildResponse(chatResponse, HttpStatus.OK, "Chat response generated successfully");
    }
}
