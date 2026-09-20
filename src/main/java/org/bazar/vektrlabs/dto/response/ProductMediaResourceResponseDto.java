package org.bazar.vektrlabs.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class ProductMediaResourceResponseDto {
    private UUID id;
    private Integer sortOrder;
    private Boolean primaryImage;
    private String preSignedUrl;
    private UUID productId;
}
