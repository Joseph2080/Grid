package org.bazar.vektrlabs.repository;

import org.bazar.vektrlabs.entity.AttributeSchema;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface AttributeSchemaRepository extends JpaRepository<AttributeSchema, UUID> {
    Optional<AttributeSchema> findByCodeAndVersion(String code, Integer version);
    Optional<AttributeSchema> findByCodeAndActiveTrue(String code);
    Optional<AttributeSchema> findByActiveTrue();
    boolean existsByCodeAndVersion(String code, Integer version);
}