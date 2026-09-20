package org.bazar.vektrlabs.ai.service;

import org.bazar.vektrlabs.ai.tool.ToolExecutionCallback;
import org.bazar.vektrlabs.ai.tool.ToolExecutionCollector;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.util.Arrays;
import java.util.Map;

@Service
public class ChatService {

    private final ChatClient chatClient;

    public ChatService(ChatModel chatModel) {
        this.chatClient =
                ChatClient.builder(chatModel).build();
    }

    public String chat(String message) {
        Assert.hasText(message, "Message must not be empty");
        return chatClient.prompt()
                .user(message)
                .call()
                .content();
    }

    public String chatWithOptions(
            String systemPrompt,
            String userMessage,
            ToolCallback[] tools,
            Map<String, Object> toolContext,
            ToolExecutionCollector collector) {
        Assert.hasText(userMessage, "User message must not be empty");
        Assert.hasText(systemPrompt, "System prompt must not be empty");
        ChatClient.ChatClientRequestSpec requestSpec =
                chatClient.prompt()
                        .system(systemPrompt)
                        .user(userMessage);
        if (tools != null && tools.length > 0) {
            ToolCallback[] collectingTools =
                    Arrays.stream(tools)
                            .map(tool ->
                                    new ToolExecutionCallback(
                                            tool,
                                            collector
                                    )
                            )
                            .toArray(ToolCallback[]::new);
            requestSpec.toolCallbacks(collectingTools);
        }
        if (toolContext != null && !toolContext.isEmpty()) {
            requestSpec.toolContext(toolContext);
        }
        return requestSpec
                .call()
                .content();
    }
}