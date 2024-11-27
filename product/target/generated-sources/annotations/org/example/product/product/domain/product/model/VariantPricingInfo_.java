package org.example.product.product.domain.product.model;

import jakarta.annotation.Generated;
import jakarta.persistence.metamodel.EmbeddableType;
import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.StaticMetamodel;
import java.math.BigDecimal;

@StaticMetamodel(VariantPricingInfo.class)
@Generated("org.hibernate.processor.HibernateProcessor")
public abstract class VariantPricingInfo_ {

	public static final String TAXABLE = "taxable";
	public static final String PRICE = "price";
	public static final String COMPARE_AT_PRICE = "compareAtPrice";

	
	/**
	 * @see org.example.product.product.domain.product.model.VariantPricingInfo#taxable
	 **/
	public static volatile SingularAttribute<VariantPricingInfo, Boolean> taxable;
	
	/**
	 * @see org.example.product.product.domain.product.model.VariantPricingInfo#price
	 **/
	public static volatile SingularAttribute<VariantPricingInfo, BigDecimal> price;
	
	/**
	 * @see org.example.product.product.domain.product.model.VariantPricingInfo
	 **/
	public static volatile EmbeddableType<VariantPricingInfo> class_;
	
	/**
	 * @see org.example.product.product.domain.product.model.VariantPricingInfo#compareAtPrice
	 **/
	public static volatile SingularAttribute<VariantPricingInfo, BigDecimal> compareAtPrice;

}

