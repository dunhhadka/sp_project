package org.example.product.product.domain.product.model;

import jakarta.annotation.Generated;
import jakarta.persistence.metamodel.EmbeddableType;
import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.StaticMetamodel;

@StaticMetamodel(VariantInventoryInfo.class)
@Generated("org.hibernate.processor.HibernateProcessor")
public abstract class VariantInventoryInfo_ {

	public static final String REQUIRE_SHIPPING = "requireShipping";
	public static final String INVENTORY_MANAGEMENT = "inventoryManagement";
	public static final String INVENTORY_QUANTITY = "inventoryQuantity";
	public static final String QUANTITY_ADJUSTABLE = "quantityAdjustable";
	public static final String OLD_INVENTORY_QUANTITY = "oldInventoryQuantity";

	
	/**
	 * @see org.example.product.product.domain.product.model.VariantInventoryInfo#requireShipping
	 **/
	public static volatile SingularAttribute<VariantInventoryInfo, Boolean> requireShipping;
	
	/**
	 * @see org.example.product.product.domain.product.model.VariantInventoryInfo#inventoryManagement
	 **/
	public static volatile SingularAttribute<VariantInventoryInfo, String> inventoryManagement;
	
	/**
	 * @see org.example.product.product.domain.product.model.VariantInventoryInfo#inventoryQuantity
	 **/
	public static volatile SingularAttribute<VariantInventoryInfo, Integer> inventoryQuantity;
	
	/**
	 * @see org.example.product.product.domain.product.model.VariantInventoryInfo#quantityAdjustable
	 **/
	public static volatile SingularAttribute<VariantInventoryInfo, Integer> quantityAdjustable;
	
	/**
	 * @see org.example.product.product.domain.product.model.VariantInventoryInfo
	 **/
	public static volatile EmbeddableType<VariantInventoryInfo> class_;
	
	/**
	 * @see org.example.product.product.domain.product.model.VariantInventoryInfo#oldInventoryQuantity
	 **/
	public static volatile SingularAttribute<VariantInventoryInfo, Integer> oldInventoryQuantity;

}

