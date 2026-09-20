package org.bazar.vektrlabs.ai.orchestrator;

import lombok.RequiredArgsConstructor;
import org.bazar.vektrlabs.ai.service.ChatService;
import org.bazar.vektrlabs.ai.tool.ToolExecutionCollector;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class AiOrchestrator {

    private final ChatService chatService;
    @Value("${ai.rag.system-prompt:You are an intelligent assistant. Use the following context to answer the user's question. If the context does not contain the answer, say you do not know.\\n\\nContext:\\n%s}")
    private String systemPromptTemplate;

    public String chatWithContext(
            String message,
            ToolCallback[] tools,
            Map<String, Object> toolContext,
            ToolExecutionCollector collector) {
        Assert.hasText(message, "Message must not be empty");
        Assert.hasText(systemPromptTemplate, "System prompt must not be empty");
        return chatService.chatWithOptions(
                systemPromptTemplate,
                message,
                tools,
                toolContext,
                collector);
    }
}