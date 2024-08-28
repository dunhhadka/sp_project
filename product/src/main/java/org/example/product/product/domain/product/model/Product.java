package org.example.product.product.domain.product.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import jakarta.persistence.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;

@Entity
@Table(name = "Products")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@DynamicUpdate
public class Product {

    @Transient
    @JsonIgnore
    @Setter
    private ProductIdGenerator idGenerator;

    @EmbeddedId
    private ProductId id;

    @NotNull
    @Size(max = 320)
    private String name;

    @Embedded
    @JsonUnwrapped
    private @Valid ProductGeneralInfo generalInfo;

    private boolean available;

    @NotNull
    @Enumerated(value = EnumType.STRING)
    private ProductStatus status;

    @OneToMany(mappedBy = "aggRoot", fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true)
    @Fetch(FetchMode.SUBSELECT)
    @OrderBy("id desc")
    private List<Variant> variants = new ArrayList<>();

    @OneToMany(mappedBy = "aggRoot", fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true)
    @Fetch(FetchMode.SUBSELECT)
    @OrderBy("id desc")
    private List<Image> images = new ArrayList<>();

    @Size(max = 50)
    @Fetch(FetchMode.SUBSELECT)
    @ElementCollection
    @CollectionTable(name = "ProductTags", joinColumns = {
            @JoinColumn(name = "productId", referencedColumnName = "id"),
            @JoinColumn(name = "storeId", referencedColumnName = "storeId")
    })
    private List<Tag> tags = new ArrayList<>();

    @NotNull
    @CreationTimestamp
    private Instant createdOn;
    private Instant modifiedOn;

    public Product(
            ProductIdGenerator idGenerator,
            ProductId productId,
            String name,
            ProductGeneralInfo generalInfo,
            List<String> tags,
            List<Variant> variants,
            List<Image> images
    ) {
        this.idGenerator = idGenerator;
        this.id = productId;
        this.name = name;
        if (generalInfo != null) this.generalInfo = generalInfo;
        this.setTags(tags);
        this.setImages(images, null);
        this.setVariants(variants, null);
    }

    private void setVariants(List<Variant> newVariants, LinkedHashMap<Integer, VariantUpdateInfo> updateVariants) {
        if (newVariants == null && updateVariants == null) return;
        if (newVariants == null) newVariants = new ArrayList<>();
        if (updateVariants == null) updateVariants = new LinkedHashMap<>();

        var variantIds = updateVariants.keySet();
        var needRemoveVariants = this.variants.stream().filter(v -> !variantIds.contains(v.getId())).toList();
        needRemoveVariants.forEach(this::internalRemoveVariant);

        for (var variant : newVariants) {
            this.internalAddVariant(variant);
        }
        for (var variantId : variantIds) {
            internalUpdateVariant(variantId, updateVariants.get(variantId));
        }

        internalReoderVariant();
    }

    private void internalReoderVariant() {
        // sắp xếp lại theo modifiedOn
    }

    private void internalUpdateVariant(Integer variantId, VariantUpdateInfo variantUpdateInfo) {
        var variant = this.variants.stream().filter(v -> v.getId() == variantId).findFirst().orElse(null);
        if (variant == null) return;
        variant.update(variantUpdateInfo);
    }

    private void internalAddVariant(Variant variant) {
        variant.setAggRoot(this);
        if (!checkImageExisted(variant.getImageId())) {
            variant.setImage(null);
        }
        this.variants.add(variant);
    }

    private void internalRemoveVariant(Variant variant) {
        if (!this.variants.contains(variant)) return;
        this.variants.remove(variant);
    }

    private boolean checkImageExisted(Integer imageId) {
        if (imageId == null) return true;
        return this.images.stream().anyMatch(i -> ObjectUtils.equals(imageId, i.getId()));
    }

    public void setImages(List<Image> newImages, LinkedHashMap<Integer, ImageUpdatableInfo> updateImages) {
        if (newImages == null && updateImages == null) return;
        if (newImages == null) newImages = new ArrayList<>();
        if (updateImages == null) updateImages = new LinkedHashMap<>();

        var imageIds = updateImages.keySet();
        var needRemoveImages = this.images.stream().filter(i -> !imageIds.contains(i.getId())).toList();
        needRemoveImages.forEach(this::internalRemoveImage);
        for (var image : newImages) {
            internalAddImage(image);
        }

        for (var imageId : updateImages.keySet()) {
            internalUpdateImage(imageId, updateImages.get(imageId));
        }

        internalReoderImages();
        this.modifiedOn = Instant.now();
    }

    private void internalReoderImages() {
        this.images.stream().sorted(getImageComparator()).toList();
    }

    private Comparator<Image> getImageComparator() {
        return (i1, i2) -> {
            if (i1.getModifiedAt() != null && i2.getModifiedAt() != null) {
                return i1.getModifiedAt().compareTo(i2.getModifiedAt());
            } else if (i1.getModifiedAt() != null) {
                return 1;
            } else if (i2.getModifiedAt() != null) {
                return -1;
            } else {
                return 0;
            }
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
    }

    private void internalRemoveImage(Image image) {
        if (!this.images.contains(image)) return;

        image.setAggRoot(null);
        this.images.remove(image);
        this.variants.stream().filter(v -> ObjectUtils.compare(image.getId(), v.getImageId()) == 0).forEach(v -> v.setImage(null));
    }

    private void setTags(List<String> tags) {
        if (CollectionUtils.isEmpty(tags)) return;

        for (var tag : tags) {
            this.internalAddTag(tag);
        }
        var removeTags = this.tags.stream().filter(t -> !tags.contains(t.getName())).toList();
        for (var tag : removeTags) {
            this.internalRemoveTag(tag);
        }
        this.modifiedOn = Instant.now();
    }

    private void internalRemoveTag(Tag tag) {
        if (!this.tags.contains(tag)) return;
        this.tags.remove(tag);
    }

    private void internalAddTag(String tagName) {
        if (this.tags.stream().anyMatch(t -> StringUtils.equals(t.getName(), tagName))) return;
        var tag = new Tag(tagName);
        this.tags.add(tag);
    }

    public void setImageToVariants(int imageId, List<Integer> variantIds) {
        if (CollectionUtils.isEmpty(variantIds)) return;
        var image = this.images.stream().filter(i -> i.getId() == imageId).findFirst().orElse(null);
        if (image == null) return;
        for (var variant : this.variants) {
            if (variantIds.contains(variant.getId())) {
                variant.setImage(imageId);
            } else {
                variant.setImage(null);
            }
        }
    }

    public void setImageIdToVariants(int imageId, List<Integer> variantIds) {
        if (CollectionUtils.isEmpty(variantIds)) return;
        for (var variant : this.variants) {
            if (variantIds.contains(variant.getId())) {
                variant.setImage(imageId);
            } else {
                variant.setImage(null);
            }
        }
    }

    public enum ProductStatus {
        draft, active, archive
    }
}
