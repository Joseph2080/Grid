package org.bazar.vektrlabs.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class ProductMediaResourceRequestDto {
    @NotNull(message = "product id cannot be empty")
    private UUID productId;
    private Boolean primaryImage;
    @NotNull(message = "image file cannot be empty")
    private MultipartFile file;
}
