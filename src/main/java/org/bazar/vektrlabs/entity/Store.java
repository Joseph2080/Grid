package org.bazar.vektrlabs.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.bazar.vektrlabs.entity.enums.Currency;
import org.hibernate.annotations.ColumnDefault;
import org.jericho.common.entity.BaseJpaEntity;
import org.jericho.mediaresource.entity.MediaResource;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "stores")
@Getter
@Setter
public class Store extends BaseJpaEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_profile_id", nullable = false)
    private UserProfile userProfile;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(length = 5000)
    private String description;

    @Column(length = 1000)
    private String facebookUrl;

    @Column(length = 1000)
    private String instagramUrl;

    @Column(length = 1000)
    private String xUrl;

    @Column(length = 1000)
    private String tiktokUrl;

    @Column(length = 1000)
    private String youtubeUrl;

    @Column(length = 1000)
    private String linkedinUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @ColumnDefault("USD")
    private Currency currency = Currency.USD;

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "logo_media_id")
    private MediaResource logo;

    @OneToMany(
            mappedBy = "store",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<Product> products = new ArrayList<>();

    @OneToMany(
            mappedBy = "store",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<Location> locations = new ArrayList<>();
}