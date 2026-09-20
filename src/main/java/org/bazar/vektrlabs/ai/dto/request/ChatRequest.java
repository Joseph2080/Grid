package org.bazar.vektrlabs.ai.dto.request;

import lombok.Builder;

@Builder
public record ChatRequest(String message) {
}