package org.example.product.product.domain.product.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.annotations.DynamicUpdate;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "images")
@DynamicUpdate
@Getter
@NoArgsConstructor
public class Image {

    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "storeId", referencedColumnName = "storeId")
    @JoinColumn(name = "productId", referencedColumnName = "id")
    @Setter
    private Product aggRoot;

    @Id
    private int id;

    @NotNull
    @Setter
    private int position = Integer.MAX_VALUE;

    @NotNull
    private Instant createdOn;

    @NotNull
    private Instant modifiedOn;

    @Size(max = 255)
    private String alt;

    @NotNull
    private String src;

    @NotNull
    private String fileName;

    @Embedded
    @JsonUnwrapped
    private ImagePhysicalInfo physicalInfo;

    public Image(
            Integer id,
            int position,
            String alt,
            String src,
            String fileName,
            ImagePhysicalInfo physicalInfo
    ) {
        this.id = id;
        this.position = position;
        this.alt = alt;
        this.src = src;
        this.fileName = fileName;
        if (physicalInfo != null) {
            this.physicalInfo = physicalInfo;
        }
        this.createdOn = Instant.now();
        this.modifiedOn = Instant.now();
    }

    public void update(ImageUpdatableInfo imageUpdate) {
        if (!StringUtils.equals(this.alt, imageUpdate.getAlt())) {
            this.alt = imageUpdate.getAlt();
        }
        if (!StringUtils.equals(this.src, imageUpdate.getSrc())) {
            this.src = imageUpdate.getSrc();
        }
        if (!StringUtils.equals(this.fileName, imageUpdate.getFileName())) {
            this.fileName = imageUpdate.getFileName();
        }
        if (!Objects.equals(this.position, imageUpdate.getPosition())) {
            this.position = imageUpdate.getPosition();
        }
        if (!Objects.equals(this.physicalInfo, imageUpdate.getPhysicalInfo())) {
            this.physicalInfo = imageUpdate.getPhysicalInfo();
        }

        this.modifiedOn = Instant.now();
    }
}
