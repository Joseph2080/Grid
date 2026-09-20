package org.bazar.vektrlabs.repository;

import org.bazar.vektrlabs.entity.ProductVariant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProductVariantRepository extends JpaRepository<ProductVariant, UUID> {

    Optional<List<ProductVariant>> findByProductId(UUID productId);

    @Query("SELECT pv FROM ProductVariant pv JOIN pv.attributes a WHERE pv.product.id = :productId AND KEY(a) = :key AND VALUE(a) = :value")
    Optional<List<ProductVariant>> findByProductIdAndAttribute(
            @Param("productId") UUID productId,
            @Param("key") String key,
            @Param("value") String value
    );

    @Query("SELECT pv FROM ProductVariant pv JOIN pv.attributes a WHERE KEY(a) = :key AND VALUE(a) = :value")
    Optional<List<ProductVariant>> findByAttribute(@Param("key") String key, @Param("value") String value);
}