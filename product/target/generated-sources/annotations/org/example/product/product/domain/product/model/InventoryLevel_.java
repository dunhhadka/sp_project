package org.example.product.product.domain.product.model;

import jakarta.annotation.Generated;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.StaticMetamodel;
import java.math.BigDecimal;
import java.time.Instant;

@StaticMetamodel(InventoryLevel.class)
@Generated("org.hibernate.processor.HibernateProcessor")
public abstract class InventoryLevel_ {

	public static final String INCOMING = "incoming";
	public static final String COMMITTED = "committed";
	public static final String PRODUCT_ID = "productId";
	public static final String AVAILABLE = "available";
	public static final String UPDATE_AT = "updateAt";
	public static final String STORE_ID = "storeId";
	public static final String CREATE_AT = "createAt";
	public static final String INVENTORY_ITEM_ID = "inventoryItemId";
	public static final String ON_HAND = "onHand";
	public static final String LOCATION_ID = "locationId";
	public static final String ID = "id";
	public static final String VARIANT_ID = "variantId";

	
	/**
	 * @see org.example.product.product.domain.product.model.InventoryLevel#incoming
	 **/
	public static volatile SingularAttribute<InventoryLevel, BigDecimal> incoming;
	
	/**
	 * @see org.example.product.product.domain.product.model.InventoryLevel#committed
	 **/
	public static volatile SingularAttribute<InventoryLevel, BigDecimal> committed;
	
	/**
	 * @see org.example.product.product.domain.product.model.InventoryLevel#productId
	 **/
	public static volatile SingularAttribute<InventoryLevel, Integer> productId;
	
	/**
	 * @see org.example.product.product.domain.product.model.InventoryLevel#available
	 **/
	public static volatile SingularAttribute<InventoryLevel, BigDecimal> available;
	
	/**
	 * @see org.example.product.product.domain.product.model.InventoryLevel#updateAt
	 **/
	public static volatile SingularAttribute<InventoryLevel, Instant> updateAt;
	
	/**
	 * @see org.example.product.product.domain.product.model.InventoryLevel#storeId
	 **/
	public static volatile SingularAttribute<InventoryLevel, Integer> storeId;
	
	/**
	 * @see org.example.product.product.domain.product.model.InventoryLevel#createAt
	 **/
	public static volatile SingularAttribute<InventoryLevel, Instant> createAt;
	
	/**
	 * @see org.example.product.product.domain.product.model.InventoryLevel#inventoryItemId
	 **/
	public static volatile SingularAttribute<InventoryLevel, Integer> inventoryItemId;
	
	/**
	 * @see org.example.product.product.domain.product.model.InventoryLevel#onHand
	 **/
	public static volatile SingularAttribute<InventoryLevel, BigDecimal> onHand;
	
	/**
	 * @see org.example.product.product.domain.product.model.InventoryLevel#locationId
	 **/
	public static volatile SingularAttribute<InventoryLevel, Long> locationId;
	
	/**
	 * @see org.example.product.product.domain.product.model.InventoryLevel#id
	 **/
	public static volatile SingularAttribute<InventoryLevel, Integer> id;
	
	/**
	 * @see org.example.product.product.domain.product.model.InventoryLevel#variantId
	 **/
	public static volatile SingularAttribute<InventoryLevel, Integer> variantId;
	
	/**
	 * @see org.example.product.product.domain.product.model.InventoryLevel
	 **/
	public static volatile EntityType<InventoryLevel> class_;

}

