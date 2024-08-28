package org.example.product.product.application.model.response;

import lombok.Getter;
import lombok.Setter;
import org.example.product.product.domain.product.model.Variant;

@Setter
@Getter
public class ProductVariantResponse {
    private int id;
    private int productId;
    private Integer inventoryItemId;
    private String title;
    private String barcode;
    private String sku;
    private String price;
    private String compareAtPrice;
    private boolean taxable;

    private String option1;
    private String option2;
    private String option3;

    private String inventoryManagement;
    private Integer inventoryQuantity;
    private Integer oldInventoryQuantity;
    private Integer quantityAdjustable;
    private Boolean requireShipping;

    private Double weight;
    private String weightUnit;
    private String unit;

    private Variant.VariantType type;

    private Integer imagePosition;
    private Integer imageId;

    private Product product;

    private Image image;

    @Getter
    @Setter
    public static class Product {
        private int id;
        private String name;
        private String vendor;
        private String productType;
        private String tags;
        private org.example.product.product.domain.product.model.Product.ProductStatus status;
    }

    @Getter
    @Setter
    public static class Image {
        private String alt;
        private String src;
    }
}
