package org.bazar.vektrlabs.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.bazar.vektrlabs.entity.enums.Currency;
import org.springframework.web.multipart.MultipartFile;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class StoreRequestDto {
    @NotBlank(message = "Store name is required")
    @Size(max = 255, message = "Store name must not exceed 255 characters")
    private String name;
    @Size(max = 5000, message = "Description must not exceed 5000 characters")
    private String description;
    // Optional social media links which need to be validated
    private String facebookUrl;
    private String instagramUrl;
    private String twitterUrl;
    private String tiktokUrl;
    private String youtubeUrl;
    private String linkedinUrl;
    @NotNull(message = "Store currency is required")
    private Currency currency;
    @NotNull(message = "image file cannot be empty")
    private MultipartFile logo;
}