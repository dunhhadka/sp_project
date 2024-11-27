package org.example.product.product.domain.product.model;

import jakarta.annotation.Generated;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.StaticMetamodel;
import java.time.Instant;

@StaticMetamodel(InventoryItem.class)
@Generated("org.hibernate.processor.HibernateProcessor")
public abstract class InventoryItem_ {

	public static final String REQUIRE_SHIPPING = "requireShipping";
	public static final String CREATED_AT = "createdAt";
	public static final String PRODUCT_ID = "productId";
	public static final String MODIFIED_AT = "modifiedAt";
	public static final String TRACKED = "tracked";
	public static final String ID = "id";
	public static final String VARIANT_ID = "variantId";
	public static final String STORE_ID = "storeId";
	public static final String SKU = "sku";
	public static final String BARCODE = "barcode";

	
	/**
	 * @see org.example.product.product.domain.product.model.InventoryItem#requireShipping
	 **/
	public static volatile SingularAttribute<InventoryItem, Boolean> requireShipping;
	
	/**
	 * @see org.example.product.product.domain.product.model.InventoryItem#createdAt
	 **/
	public static volatile SingularAttribute<InventoryItem, Instant> createdAt;
	
	/**
	 * @see org.example.product.product.domain.product.model.InventoryItem#productId
	 **/
	public static volatile SingularAttribute<InventoryItem, Integer> productId;
	
	/**
	 * @see org.example.product.product.domain.product.model.InventoryItem#modifiedAt
	 **/
	public static volatile SingularAttribute<InventoryItem, Instant> modifiedAt;
	
	/**
	 * @see org.example.product.product.domain.product.model.InventoryItem#tracked
	 **/
	public static volatile SingularAttribute<InventoryItem, Boolean> tracked;
	
	/**
	 * @see org.example.product.product.domain.product.model.InventoryItem#id
	 **/
	public static volatile SingularAttribute<InventoryItem, Integer> id;
	
	/**
	 * @see org.example.product.product.domain.product.model.InventoryItem#variantId
	 **/
	public static volatile SingularAttribute<InventoryItem, Integer> variantId;
	
	/**
	 * @see org.example.product.product.domain.product.model.InventoryItem#storeId
	 **/
	public static volatile SingularAttribute<InventoryItem, Integer> storeId;
	
	/**
	 * @see org.example.product.product.domain.product.model.InventoryItem#sku
	 **/
	public static volatile SingularAttribute<InventoryItem, String> sku;
	
	/**
	 * @see org.example.product.product.domain.product.model.InventoryItem
	 **/
	public static volatile EntityType<InventoryItem> class_;
	
	/**
	 * @see org.example.product.product.domain.product.model.InventoryItem#barcode
	 **/
	public static volatile SingularAttribute<InventoryItem, String> barcode;

}

