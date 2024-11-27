package org.example.product.product.domain.product.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.example.product.ddd.AggregateRoot;
import org.example.product.product.domain.product.event.*;
import org.example.product.product.domain.product.repository.ProductIdGenerator;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

@Entity
@Getter
@Table(name = "products")
@NoArgsConstructor
public class Product extends AggregateRoot<Product> {

    @Setter
    @Transient
    @JsonIgnore
    private ProductIdGenerator idGenerator;

    @EmbeddedId
    @JsonUnwrapped
    private ProductId id;

    @NotBlank
    @Size(max = 250)
    private String name;
    @NotBlank
    @Size(max = 150)
    private String alias;
    private Instant createdOn;
    private Instant modifiedOn;
    private Instant publishedOn;

    @Enumerated(value = EnumType.STRING)
    private ProductStatus status;
    @Enumerated(value = EnumType.STRING)
    private Type type;

    @Valid
    @Embedded
    @JsonUnwrapped
    private ProductPricingInfo pricingInfo;
    @Valid
    @Embedded
    @JsonUnwrapped
    private ProductGeneralInfo generalInfo;

    private boolean available = false;

    @OneToMany(mappedBy = "aggRoot", fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true)
    @Fetch(FetchMode.SUBSELECT)
    private List<Variant> variants = new ArrayList<>();

    @OneToMany(mappedBy = "aggRoot", fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true)
    @Fetch(FetchMode.SUBSELECT)
    private List<Image> images = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "product_tags", joinColumns = {
            @JoinColumn(name = "name", referencedColumnName = "name"),
            @JoinColumn(name = "alias", referencedColumnName = "alias")
    })
    private List<Tag> tags = new ArrayList<>();

    public Product(
            ProductId id,
            String name,
            String alias,
            ProductGeneralInfo productGeneralInfo,
            List<String> tags,
            List<Variant> variants,
            List<Image> images,
            ProductIdGenerator idGenerator,
            ProductStatus status,
            Instant publishedOn
    ) {
        this.idGenerator = idGenerator;
        this.id = id;
        this.name = name;
        this.alias = alias;
        if (productGeneralInfo != null) this.generalInfo = productGeneralInfo;
        this.setTags(tags);
        this.setImages(images, null);
        this.setVariants(variants, null);

        this.resoleStatusAndPublishOn(status, publishedOn);

        this.createdOn = Instant.now();
        this.modifiedOn = Instant.now();
    }

    private void resoleStatusAndPublishOn(ProductStatus status, Instant publishedOn) {
        this.publishedOn = publishedOn;
        this.status = status != null ? status : ProductStatus.active;
    }

    private void setVariants(List<Variant> newVariants, Map<Integer, VariantUpdateInfo> updateVariantInfos) {
        if (CollectionUtils.isEmpty(newVariants) && (updateVariantInfos == null || updateVariantInfos.isEmpty()))
            return;

        if (newVariants == null) newVariants = List.of();
        if (updateVariantInfos == null) updateVariantInfos = new HashMap<>();

        var variantIds = updateVariantInfos.keySet();
        var needRemoveVariants = this.variants.stream().filter(v -> !variantIds.contains(v.getId())).toList();
        needRemoveVariants.forEach(this::internalRemoveVariant);

        newVariants.forEach(this::internalAddVariant);

        for (var variantId : variantIds) {
            this.internalUpdateVariant(variantId, updateVariantInfos.get(variantId));
        }

        this.internalReorderVariants();

        this.applyChangeSideEffect();
    }

    private void applyChangeSideEffect() {
        setAvailable();
        calculateAndSetPricingInfo();
        changeTypeByVariantType();
    }

    private void calculateAndSetPricingInfo() {
        var productPricingInfoBuilder = ProductPricingInfo.builder();
        setPriceMaxAndPriceMin(productPricingInfoBuilder);
        setCompareAtPriceMaxAndMin(productPricingInfoBuilder);
        setPriceVariantsAndCompareAtPrice(productPricingInfoBuilder);

        this.pricingInfo = productPricingInfoBuilder.build();
    }

    private void setPriceVariantsAndCompareAtPrice(ProductPricingInfo.ProductPricingInfoBuilder productPricingInfoBuilder) {
        boolean isPriceVariants = this.variants.stream().anyMatch(v -> v.getPricingInfo().getPrice().compareTo(BigDecimal.ZERO) > 0);
        boolean isCompareAtPrice = this.variants.stream()
                .map(v -> v.getPricingInfo().getPrice())
                .filter(Objects::nonNull)
                .sorted()
                .distinct()
                .toList().size() >= 2;
        productPricingInfoBuilder
                .priceVaries(isPriceVariants)
                .compareAtPriceVaries(isCompareAtPrice);
    }

    private void setCompareAtPriceMaxAndMin(ProductPricingInfo.ProductPricingInfoBuilder productPricingInfoBuilder) {
        var prices = this.variants.stream().map(v -> v.getPricingInfo().getCompareAtPrice()).filter(Objects::nonNull).toList();
        var compareAtPriceMin = Collections.min(prices);
        var compareAtPriceMax = Collections.max(prices);
        productPricingInfoBuilder
                .compareAtPriceMin(compareAtPriceMin)
                .compareAtPriceMax(compareAtPriceMax);
    }

    private void setPriceMaxAndPriceMin(ProductPricingInfo.ProductPricingInfoBuilder productPricingInfoBuilder) {
        var prices = this.variants.stream().map(v -> v.getPricingInfo().getPrice()).filter(Objects::nonNull).toList();
        var priceMax = Collections.max(prices);
        var priceMin = Collections.min(prices);
        productPricingInfoBuilder
                .priceMin(priceMin)
                .priceMax(priceMax);
    }

    private void setAvailable() {
        boolean checkAvailable = false;
        for (var variant : variants) {
            if (StringUtils.isBlank(variant.getInventoryManagementInfo().getInventoryManagement())) {
                checkAvailable = true;
                break;
            }
        }

        this.available = checkAvailable;
    }

    private void changeTypeByVariantType() {
        if (this.variants.stream().anyMatch(v -> v.getType() == Variant.VariantType.combo)) {
            this.type = Type.combo;
        } else if (this.variants.stream().anyMatch(v -> v.getType() == Variant.VariantType.packsize)) {
            this.type = Type.packsize;
        } else {
            this.type = Type.normal;
        }
    }

    private void internalReorderVariants() {
        var sortedVariants = this.variants.stream().sorted(getVariantComparator()).toList();
        for (int i = 0; i < sortedVariants.size(); i++) {
            sortedVariants.get(i).setPosition(i + 1);
        }
        this.variants = sortedVariants;
    }

    private static Comparator<Variant> getVariantComparator() {
        return (v1, v2) -> {
            if (v1.getPosition() == v2.getPosition()) {
                if (v1.getModifiedOn() != null && v2.getModifiedOn() != null) {
                    return v1.getModifiedOn().compareTo(v2.getModifiedOn());
                } else if (v1.getModifiedOn() == null) {
                    return 1;
                } else {
                    return 1;
                }
            }
            return Integer.compare(v1.getPosition(), v2.getPosition());
        };
    }

    private void internalUpdateVariant(Integer variantId, VariantUpdateInfo variantUpdateInfo) {
        var variant = this.variants.stream().filter(v -> v.getId() == variantId).findFirst().orElse(null);
        if (variant == null) return;
        variant.update(variantUpdateInfo);
    }

    private void internalAddVariant(Variant variant) {
        variant.setAggRoot(this);
        this.variants.add(variant);
        this.addDomainEvents(new ProductVariantAdded(variant.getId(), variant));
    }

    private void internalRemoveVariant(Variant variant) {
        if (!this.variants.contains(variant)) return;

        variant.setAggRoot(null);
        this.variants.remove(variant);
        this.addDomainEvents(new ProductVariantRemoved(variant.getId()));
    }

    private void setImages(List<Image> newImages, LinkedHashMap<Integer, ImageUpdatableInfo> updateImages) {
        if (newImages == null && updateImages == null) return;
        if (newImages == null) newImages = List.of();
        if (updateImages == null) updateImages = new LinkedHashMap<>();

        var imageIds = updateImages.keySet();
        var needRemoveImages = this.images.stream().filter(i -> imageIds.contains(i.getId())).toList();
        for (var image : needRemoveImages) internalRemoveImage(image);

        for (var image : newImages) internalAddImage(image);

        for (var imageId : imageIds) {
            internalUpdateImage(imageId, updateImages.get(imageId));
        }

        this.internalReorderImages();
    }

    private void internalReorderImages() {
        var soredImages = this.images.stream().sorted(getImageComparator()).toList();
        for (var i = 0; i < soredImages.size(); i++) {
            soredImages.get(i).setPosition(i + 1);
        }
        this.images = soredImages;
    }

    private static Comparator<Image> getImageComparator() {
        return (image1, image2) -> {
            if (image1.getPosition() == image2.getPosition()) {
                return Math.negateExact(image1.getModifiedOn().compareTo(image2.getModifiedOn()));
            }
            return Integer.compare(image1.getPosition(), image2.getPosition());
        };
    }

    private void internalUpdateImage(Integer imageId, ImageUpdatableInfo imageUpdatableInfo) {
        var image = this.images.stream().filter(i -> i.getId() == imageId).findFirst().orElse(null);
        if (image == null) return;
        image.update(imageUpdatableInfo);
    }

    private void internalAddImage(Image image) {
        image.setAggRoot(this);
        this.images.add(image);
        this.addDomainEvents(new ProductImageAdded(image.getId(), image.getFileName(), image.getSrc()));
    }

    private void internalRemoveImage(Image image) {
        if (!this.images.contains(image)) return;

        image.setAggRoot(null);
        this.images.remove(image);
        this.variants.stream().filter(v -> CollectionUtils.isNotEmpty(v.getImageIds()))
                .filter(v -> v.getImageIds().contains(image.getId()))
                .forEach(v -> v.removeImage(image.getId()));
        this.addDomainEvents(new ProductImageRemoved(image.getId(), image.getFileName(), image.getSrc()));
    }

    private void setTags(List<String> tagNames) {
        if (tagNames == null) tagNames = new ArrayList<>();
        for (var tagName : tagNames) internalAddTag(tagName);
        List<String> finalTagNames = tagNames;
        var removeTags = this.tags.stream().filter(tag -> !finalTagNames.contains(tag.getName())).toList();
        for (var tag : removeTags) internalRemoveTag(tag);
    }

    private void internalRemoveTag(Tag tag) {
        if (!this.tags.contains(tag)) return;
        this.tags.remove(tag);
        this.addDomainEvents(new ProductTagRemoved(tag));
    }

    private void internalAddTag(String tagName) {
        if (this.tags.stream().anyMatch(t -> StringUtils.equals(t.getName(), tagName))) return;
        var tag = new Tag(tagName, tagName);
        this.tags.add(tag);
        this.addDomainEvents(new ProductTagAdded(tagName));
    }

    public enum ProductStatus {
        draft, active, archive
    }

    public enum Type {
        normal, combo, packsize
    }
}
