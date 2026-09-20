package org.bazar.vektrlabs.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.jericho.common.entity.BaseJpaEntity;

@Entity
@Table(name = "attribute_schemas")
@Getter
@Setter
public class AttributeSchema extends BaseJpaEntity {
    @Column(nullable = false, unique = true, length = 100)
    private String code;
    @Column(nullable = false, length = 150)
    private String name;
    @Column(nullable = false)
    private Integer version;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private String schemaJson;
    @Column(nullable = false)
    private boolean active = true;
}