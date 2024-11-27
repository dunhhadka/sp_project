package org.example.product.product.domain.product.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.tuple.Triple;
import org.example.product.ddd.NestedDomainEvent;
import org.example.product.product.application.converters.ListIntegerConverter;
import org.example.product.product.domain.product.event.ObjectPropertyChange;
import org.example.product.product.domain.product.event.PropertyChanged;
import org.hibernate.annotations.DynamicUpdate;

import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.time.Instant;
import java.util.List;

@Entity
@Getter
@DynamicUpdate
@Table(name = "variants")
@NoArgsConstructor
public class Variant extends NestedDomainEvent<Product> {

    @JsonIgnore
    @ManyToOne
    @Setter
    @JoinColumn(name = "storeId", referencedColumnName = "storeId")
    @JoinColumn(name = "productId", referencedColumnName = "id")
    private Product aggRoot;

    @Id
    private int id;
    private int inventoryItemId;
    @Enumerated(value = EnumType.STRING)
    private VariantType type;

    @Valid
    @Embedded
    @JsonUnwrapped
    private VariantIdentityInfo identityInfo = new VariantIdentityInfo();

    @Valid
    @Embedded
    @JsonUnwrapped
    private VariantPricingInfo pricingInfo = new VariantPricingInfo();

    @Valid
    @Embedded
    @JsonUnwrapped
    private VariantOptionInfo optionInfo = new VariantOptionInfo();

    @NotBlank
    @Size(max = 1500)
    private String title = VariantOptionInfo.DEFAULT_OPTION_VALUE;

    @Valid
    @JsonUnwrapped
    @Embedded
    private VariantInventoryManagementInfo inventoryManagementInfo = new VariantInventoryManagementInfo();

    @Min(-1000)
    @Max(1000)
    @Setter
    private int inventoryQuantity;

    @Valid
    @JsonUnwrapped
    @Embedded
    private VariantPhysicalInfo physicalInfo = new VariantPhysicalInfo();

    @Min(0)
    @Max(20000000)
    private int grams;

    @Convert(converter = ListIntegerConverter.class)
    private List<Integer> imageIds;

    @Setter
    private int position = Integer.MAX_VALUE;

    private Instant createdOn;

    private Instant modifiedOn;

    public Variant(
            int id,
            int inventoryItemId,
            VariantIdentityInfo identityInfo,
            VariantPricingInfo pricingInfo,
            VariantOptionInfo optionInfo,
            VariantInventoryManagementInfo inventoryManagementInfo,
            VariantPhysicalInfo physicalInfo,
            Integer inventoryQuantity,
            List<Integer> imageIds
    ) {
        this.id = id;
        this.inventoryItemId = inventoryItemId;
        if (identityInfo != null) {
            this.identityInfo = identityInfo;
        }
        if (physicalInfo != null) {
            this.pricingInfo = pricingInfo;
        }
        setOptionInfo(optionInfo);
        if (inventoryManagementInfo != null) {
            this.inventoryManagementInfo = inventoryManagementInfo;
        }
        setPhysicalInfo(physicalInfo);
        this.inventoryQuantity = inventoryQuantity;
        this.imageIds = imageIds;
        this.createdOn = Instant.now();
        this.modifiedOn = Instant.now();
        this.type = VariantType.normal;
    }

    private void setPhysicalInfo(VariantPhysicalInfo physicalInfo) {
        if (physicalInfo == null || physicalInfo.sameAs(this.physicalInfo)) {
            return;
        }

        var diffs = this.physicalInfo.getDiffs(physicalInfo);
        this.physicalInfo = physicalInfo;
        this.grams = physicalInfo.weightInGram();
        this.modifiedOn = Instant.now();

        this.addDomainEvents(new PropertyChanged(this.id, diffs.stream().map(ObjectPropertyChange::new).toList()));
    }

    private void setOptionInfo(VariantOptionInfo optionInfo) {
        if (optionInfo == null || optionInfo.sameAs(this.optionInfo)) {
            return;
        }

        var diffs = this.optionInfo.getDiffs(optionInfo);

        this.optionInfo = optionInfo;
        this.title = optionInfo.getTitle();
        this.modifiedOn = Instant.now();

        this.addDomainEvents(new PropertyChanged(this.id, diffs.stream().map(ObjectPropertyChange::new).toList()));
    }

    public void removeImage(int imageId) {
        if (CollectionUtils.isNotEmpty(this.imageIds) || !imageIds.contains(imageId)) return;
        this.imageIds.remove(imageId);
    }

    public void update(VariantUpdateInfo updatableInfo) {

        setOptionInfo(updatableInfo.getOptionInfo());

        if (updatableInfo.getIdentityInfo() != null && !this.getIdentityInfo().sameAs(updatableInfo.getIdentityInfo())) {
            this.addDomainEvents(
                    new PropertyChanged(this.id,
                            this.getIdentityInfo()
                                    .getDiffs(updatableInfo.getIdentityInfo()).stream()
                                    .map(ObjectPropertyChange::new).toList()
                    )
            );
            this.identityInfo = updatableInfo.getIdentityInfo();
        }

        if (updatableInfo.getPricingInfo() != null && !this.getPricingInfo().sameAs(updatableInfo.getPricingInfo())) {
            this.addDomainEvents(
                    new PropertyChanged(this.id,
                            this.pricingInfo
                                    .getDiffs(updatableInfo.getPricingInfo()).stream()
                                    .map(ObjectPropertyChange::new).toList()
                    )
            );
            this.pricingInfo = updatableInfo.getPricingInfo();
        }

        if (updatableInfo.getInventoryManagementInfo() != null && !this.inventoryManagementInfo.sameAs(updatableInfo.getInventoryManagementInfo())) {
            this.addDomainEvents(
                    new PropertyChanged(this.id,
                            this.inventoryManagementInfo
                                    .getDiffs(updatableInfo.getInventoryManagementInfo()).stream()
                                    .map(ObjectPropertyChange::new).toList()
                    )
            );
            this.inventoryManagementInfo = updatableInfo.getInventoryManagementInfo();
        }

        setPhysicalInfo(updatableInfo.getPhysicalInfo());

        this.updateImageIds(updatableInfo.getImageIds());

        this.position = updatableInfo.getPosition();

        this.type = updatableInfo.getType() == null ? VariantType.normal : updatableInfo.getType();

        if (this.inventoryQuantity != updatableInfo.getInventoryQuantity()) {
            this.inventoryQuantity = updatableInfo.getInventoryQuantity();
            this.addDomainEvents(
                    new PropertyChanged(
                            this.getId(),
                            List.of(new ObjectPropertyChange(Triple.of("inventory_quantity", this.inventoryQuantity, updatableInfo.getOldInventoryQuantity())))
                    )
            );
        }

    }


    private void updateImageIds(List<Integer> imageIds) {
        this.imageIds = imageIds;
    }

    public enum VariantType {
        normal, combo, packsize
    }
}
