package org.bazar.vektrlabs.repository;

import org.bazar.vektrlabs.entity.ProductMediaResource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProductMediaResourceRepository extends JpaRepository<ProductMediaResource, UUID> {

    // Ordered primary-first, then explicit sort order: this is the single
    // canonical media ordering rule for a product; consumers (OG/meta tags,
    // storefront UI, etc.) should rely on this instead of re-sorting.
    @Query("""
            SELECT pmr FROM ProductMediaResource pmr
            WHERE pmr.product.id = :productId
            ORDER BY pmr.primaryImage DESC, pmr.sortOrder ASC
            """)
    List<ProductMediaResource> findByProductId(@Param("productId") UUID productId);

    @Modifying
    @Query("""
            UPDATE ProductMediaResource pmr
            SET pmr.primaryImage = false
            WHERE pmr.product.id = :productId
              AND pmr.primaryImage = true
            """)
    int unsetPrimaryImageByProductId(@Param("productId") UUID productId);

        @Query("""
        SELECT MAX(pmr.sortOrder)
        FROM ProductMediaResource pmr
        WHERE pmr.product.id = :productId
        """)
        Optional<Integer> findMaxSortOrderByProductId(
                @Param("productId") UUID productId);

    @Query("""
    SELECT pmr
    FROM ProductMediaResource pmr
    WHERE pmr.product.id IN :productIds
    ORDER BY pmr.product.id, pmr.sortOrder
    """)
    List<ProductMediaResource> findAllByProductIds(
            @Param("productIds") Collection<Long> productIds);

    // Same primary-first-then-sort-order rule as findByProductId, applied to
    // the bulk lookup used when hydrating a full product listing.
    @Query("""
            SELECT pmr FROM ProductMediaResource pmr
            WHERE pmr.product.id IN :productIds
            ORDER BY pmr.product.id, pmr.primaryImage DESC, pmr.sortOrder ASC
            """)
    List<ProductMediaResource> findByProductIdIn(@Param("productIds") List<UUID> productIds);
}
