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

@Entity
@Table(name = "ProductImages")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Image {

    @JsonIgnore
    @ManyToOne
    @Setter(AccessLevel.PACKAGE)
    @JoinColumn(name = "storeId", referencedColumnName = "storeId")
    @JoinColumn(name = "productId", referencedColumnName = "id")
    private Product aggRoot;

    @Id
    private int id;

    @NotNull
    private String src;
    @NotNull
    private String fileName;
    @NotNull
    @Size(max = 255)
    private String alt;
    @NotNull
    private int position;

    @Embedded
    @JsonUnwrapped
    private @Valid ImagePhysicalInfo physicalInfo;

    private Instant createdAt;
    private Instant modifiedAt;

    public Image(
            Integer id,
            String src,
            String fileName,
            String alt,
            Integer position,
            ImagePhysicalInfo physicalInfo
    ) {
        this.id = id;
        this.src = src;
        this.fileName = fileName;
        this.alt = alt;
        this.position = position;
        this.physicalInfo = physicalInfo;

        this.createdAt = Instant.now();
        this.modifiedAt = Instant.now();
    }

    public void update(ImageUpdatableInfo imageUpdatableInfo) {
        this.alt = imageUpdatableInfo.getAlt();
        this.src = imageUpdatableInfo.getSrc();
        this.fileName = imageUpdatableInfo.getFileName();
        this.position = imageUpdatableInfo.getPosition();
        this.setPhysicalInfo(imageUpdatableInfo.getPhysicalInfo());
    }

    private void setPhysicalInfo(ImagePhysicalInfo physicalInfo) {
        if (physicalInfo == null || ObjectUtils.equals(physicalInfo, this.getPhysicalInfo()))
            return;
        this.physicalInfo = physicalInfo;
    }
}
