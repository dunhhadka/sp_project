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
import org.apache.commons.lang3.ObjectUtils;

import java.time.Instant;
import java.util.Objects;

@Entity
@Getter
@Table(name = "ProductVariants")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Variant {

    @JsonIgnore
    @ManyToOne
    @Setter(AccessLevel.PACKAGE)
    @JoinColumn(name = "storeId", referencedColumnName = "storeId")
    @JoinColumn(name = "productId", referencedColumnName = "id")
    private Product aggRoot;

    @Id
    private int id;

    private int inventoryItemId;

    @NotNull
    @Size(max = 500)
    private String title = VariantOptionInfo.DEFAULT_OPTION_VARIANT;

    @Embedded
    @JsonUnwrapped
    private @Valid VariantIdentityInfo identityInfo;

    @Embedded
    @JsonUnwrapped
    private @Valid VariantPricingInfo pricingInfo;

    @Embedded
    @JsonUnwrapped
    private @Valid VariantOptionInfo optionInfo;

    @Embedded
    @JsonUnwrapped
    private VariantInventoryInfo inventoryInfo;

    @Embedded
    @JsonUnwrapped
    private @Valid VariantPhysicalInfo physicalInfo;

    @NotNull
    @Enumerated(value = EnumType.STRING)
    private VariantType type;

    private Integer imagePosition;

    private Integer imageId;

    private Instant createdOn;

    private Instant modifiedOn;

    public Variant(
            Integer id,
            Integer inventoryItemId,
            VariantIdentityInfo identityInfo,
            VariantPricingInfo pricingInfo,
            VariantOptionInfo optionInfo,
            VariantInventoryInfo inventoryInfo,
            VariantPhysicalInfo physicalInfo,
            Integer imageId
    ) {
        this.id = id;
        this.inventoryItemId = inventoryItemId;
        if (identityInfo != null)
            this.identityInfo = identityInfo;
        if (pricingInfo != null)
            this.pricingInfo = pricingInfo;

        this.setOptionInfo(optionInfo);
        if (inventoryInfo != null) {
            this.inventoryInfo = inventoryInfo;
        }
        this.setPhysicalInfo(physicalInfo);

        this.createdOn = Instant.now();
        this.modifiedOn = Instant.now();

        this.type = VariantType.normal;
    }

    private void setOptionInfo(VariantOptionInfo optionInfo) {
        if (optionInfo == null || Objects.equals(this.optionInfo, optionInfo))
            return;

        // sử lý thêm thay đổi property
        this.optionInfo = optionInfo;
        this.title = optionInfo.getTitle();
        this.modifiedOn = Instant.now();
    }

    private void setPhysicalInfo(VariantPhysicalInfo physicalInfo) {
        if (physicalInfo == null || Objects.equals(this.physicalInfo, physicalInfo))
            return;
        this.physicalInfo = physicalInfo;
        this.modifiedOn = Instant.now();
    }

    public void setImage(Integer imageId) {
        if (ObjectUtils.equals(imageId, this.imageId)) return;
        this.imageId = imageId;
        this.modifiedOn = Instant.now();
    }

    public void update(VariantUpdateInfo variantUpdateInfo) {
        ///
    }

    public enum VariantType {
        normal, combo, packsize
    }
}
