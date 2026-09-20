package org.bazar.vektrlabs.ai.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class ChatResponseDto {

    private final String message;
    private final List<ChatToolResponseDto> toolResponseData;

}