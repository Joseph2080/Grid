package org.bazar.vektrlabs.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.bazar.vektrlabs.entity.enums.Currency;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class StoreResponseDto {
    private UUID id;
    private String name;
    private String description;
    // Fixed social media fields
    private String facebookUrl;
    private String instagramUrl;
    private String xUrl;
    private String tiktokUrl;
    private String youtubeUrl;
    private String linkedinUrl;
    private Currency currency;
    // Flattened from MediaResource for easier frontend consumption
    private String logoUrl;
    private String logoAltText;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}