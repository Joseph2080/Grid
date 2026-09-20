package org.bazar.vektrlabs.repository;

import org.bazar.vektrlabs.entity.Location;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LocationRepository extends JpaRepository<Location, UUID> {
    Optional<List<Location>> findAllByStoreId(UUID storeId);
    @Modifying
    @Query("""
            UPDATE Location location
            SET location.primary = false
            WHERE location.store.id = :storeId
              AND location.primary = true
            """)
    int unsetPrimaryByStoreId(@Param("storeId") UUID storeId);

}
