package org.bazar.vektrlabs.ai.facade;

import lombok.RequiredArgsConstructor;
import org.bazar.vektrlabs.ai.orchestrator.AiOrchestrator;
import org.bazar.vektrlabs.ai.tool.StoreTools;
import org.bazar.vektrlabs.ai.tool.ToolExecutionCollector;
import org.bazar.vektrlabs.dto.request.ChatRequestDto;
import org.bazar.vektrlabs.ai.dto.response.ChatResponseDto;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class StoreAiChatFacade {

    private final AiOrchestrator aiOrchestrator;
    private final StoreTools storeTools;

    public ChatResponseDto chat(ChatRequestDto request) {
        var collector = new ToolExecutionCollector();
        Map<String, Object> toolContext = new HashMap<>();
        var storeId = request.storeId();
        var cartId = request.cartId();
        if(storeId != null) {
            toolContext.put("storeId", storeId);
        }
        if(cartId != null) {
            toolContext.put("cartId", cartId);
        }
        String message = aiOrchestrator.chatWithContext(
                request.message(),
                ToolCallbacks.from(storeTools),
                toolContext,
                collector
        );
        return ChatResponseDto.builder()
                .message(message)
                .toolResponseData(collector.getResponses())
                .build();
    }
}