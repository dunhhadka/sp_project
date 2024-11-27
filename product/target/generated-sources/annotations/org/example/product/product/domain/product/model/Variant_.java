package org.example.product.product.domain.product.model;

import jakarta.annotation.Generated;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.StaticMetamodel;
import java.time.Instant;
import org.example.product.product.domain.product.model.Variant.VariantType;

@StaticMetamodel(Variant.class)
@Generated("org.hibernate.processor.HibernateProcessor")
public abstract class Variant_ {

	public static final String IMAGE_ID = "imageId";
	public static final String IDENTITY_INFO = "identityInfo";
	public static final String INVENTORY_INFO = "inventoryInfo";
	public static final String IMAGE_POSITION = "imagePosition";
	public static final String TITLE = "title";
	public static final String TYPE = "type";
	public static final String CREATED_ON = "createdOn";
	public static final String PRICING_INFO = "pricingInfo";
	public static final String INVENTORY_ITEM_ID = "inventoryItemId";
	public static final String AGG_ROOT = "aggRoot";
	public static final String PHYSICAL_INFO = "physicalInfo";
	public static final String MODIFIED_ON = "modifiedOn";
	public static final String OPTION_INFO = "optionInfo";
	public static final String ID = "id";

	
	/**
	 * @see org.example.product.product.domain.product.model.Variant#imageId
	 **/
	public static volatile SingularAttribute<Variant, Integer> imageId;
	
	/**
	 * @see org.example.product.product.domain.product.model.Variant#identityInfo
	 **/
	public static volatile SingularAttribute<Variant, VariantIdentityInfo> identityInfo;
	
	/**
	 * @see org.example.product.product.domain.product.model.Variant#inventoryInfo
	 **/
	public static volatile SingularAttribute<Variant, VariantInventoryInfo> inventoryInfo;
	
	/**
	 * @see org.example.product.product.domain.product.model.Variant#imagePosition
	 **/
	public static volatile SingularAttribute<Variant, Integer> imagePosition;
	
	/**
	 * @see org.example.product.product.domain.product.model.Variant#title
	 **/
	public static volatile SingularAttribute<Variant, String> title;
	
	/**
	 * @see org.example.product.product.domain.product.model.Variant#type
	 **/
	public static volatile SingularAttribute<Variant, VariantType> type;
	
	/**
	 * @see org.example.product.product.domain.product.model.Variant#createdOn
	 **/
	public static volatile SingularAttribute<Variant, Instant> createdOn;
	
	/**
	 * @see org.example.product.product.domain.product.model.Variant#pricingInfo
	 **/
	public static volatile SingularAttribute<Variant, VariantPricingInfo> pricingInfo;
	
	/**
	 * @see org.example.product.product.domain.product.model.Variant#inventoryItemId
	 **/
	public static volatile SingularAttribute<Variant, Integer> inventoryItemId;
	
	/**
	 * @see org.example.product.product.domain.product.model.Variant#aggRoot
	 **/
	public static volatile SingularAttribute<Variant, Product> aggRoot;
	
	/**
	 * @see org.example.product.product.domain.product.model.Variant#physicalInfo
	 **/
	public static volatile SingularAttribute<Variant, VariantPhysicalInfo> physicalInfo;
	
	/**
	 * @see org.example.product.product.domain.product.model.Variant#modifiedOn
	 **/
	public static volatile SingularAttribute<Variant, Instant> modifiedOn;
	
	/**
	 * @see org.example.product.product.domain.product.model.Variant#optionInfo
	 **/
	public static volatile SingularAttribute<Variant, VariantOptionInfo> optionInfo;
	
	/**
	 * @see org.example.product.product.domain.product.model.Variant#id
	 **/
	public static volatile SingularAttribute<Variant, Integer> id;
	
	/**
	 * @see org.example.product.product.domain.product.model.Variant
	 **/
	public static volatile EntityType<Variant> class_;

}

