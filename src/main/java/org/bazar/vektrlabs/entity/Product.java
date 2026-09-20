package org.bazar.vektrlabs.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.jericho.common.entity.BaseJpaEntity;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Entity
@Table(name = "products")
@Getter
@Setter
public class Product extends BaseJpaEntity {

    @Column(nullable = false)
    private String name;

    @Column(length = 5000)
    private String description;

    @Column(
            nullable = false,
            precision = 10,
            scale = 2)
    private BigDecimal price;

    @Column(nullable = false)
    private boolean active = true;

    @OneToMany(
            mappedBy = "product",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    // this should be fetched eagerly because why
    // wouldn't we need this if we are getting all products?
    private List<ProductVariant> variants = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @OneToMany(
            mappedBy = "product",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    @OrderBy("sortOrder ASC")
    private List<ProductMediaResource> mediaResources = new ArrayList<>();

    @ElementCollection
    @CollectionTable(
            name = "product_attributes",
            joinColumns = @JoinColumn(name = "product_id")
    )
    @MapKeyColumn(name = "attribute_name")
    @Column(name = "attribute_value", length = 1000)
    private Map<String, String> attributes = new HashMap<>();

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "attribute_schema_id", nullable = false)
    private AttributeSchema attributeSchema;
}