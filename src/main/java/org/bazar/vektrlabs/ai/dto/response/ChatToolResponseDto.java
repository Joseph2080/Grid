package org.bazar.vektrlabs.ai.dto.response;

import lombok.Builder;

@Builder
public record ChatToolResponseDto (
     ToolResponseType type,
     Object data
)
{}