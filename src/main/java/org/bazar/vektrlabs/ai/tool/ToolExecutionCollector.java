package org.bazar.vektrlabs.ai.tool;

import org.bazar.vektrlabs.ai.dto.response.ChatToolResponseDto;
import org.bazar.vektrlabs.ai.dto.response.ToolResponseType;

import java.util.ArrayList;
import java.util.List;

public class ToolExecutionCollector {

    private final List<ChatToolResponseDto> responses =
            new ArrayList<>();

    public synchronized void add(
            ToolResponseType type,
            Object data) {

        responses.add(
                ChatToolResponseDto.builder()
                        .type(type)
                        .data(data)
                        .build()
        );
    }

    public synchronized List<ChatToolResponseDto> getResponses() {
        return List.copyOf(responses);
    }
}