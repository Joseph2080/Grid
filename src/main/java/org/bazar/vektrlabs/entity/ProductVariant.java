package org.bazar.vektrlabs.entity;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.jericho.common.entity.BaseJpaEntity;

import java.util.HashMap;
import java.util.Map;

@Entity
@Table(
        name = "product_variants"
)
@Getter
@Setter
public class ProductVariant extends BaseJpaEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id")
    private Product product;

    @Column(name = "stock_quantity", nullable = false)
    private Integer stockQuantity;

    // We should probably add some schema validation here as well,
    // we should have predefined schema for clothing-stores, game stores,
    // to ensure that attributes make sense as well!
    @ElementCollection
    @CollectionTable(
            name = "variant_attributes",
            joinColumns = @JoinColumn(name = "variant_id")
    )
    @MapKeyColumn(name = "attribute_name")
    @Column(name = "attribute_value", length = 1000)
    private Map<String, String> attributes = new HashMap<>();
}