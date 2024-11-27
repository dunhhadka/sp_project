package org.example.product.product.domain.product.model;

import jakarta.annotation.Generated;
import jakarta.persistence.metamodel.EmbeddableType;
import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.StaticMetamodel;

@StaticMetamodel(VariantIdentityInfo.class)
@Generated("org.hibernate.processor.HibernateProcessor")
public abstract class VariantIdentityInfo_ {

	public static final String SKU = "sku";
	public static final String BARCODE = "barcode";

	
	/**
	 * @see org.example.product.product.domain.product.model.VariantIdentityInfo#sku
	 **/
	public static volatile SingularAttribute<VariantIdentityInfo, String> sku;
	
	/**
	 * @see org.example.product.product.domain.product.model.VariantIdentityInfo
	 **/
	public static volatile EmbeddableType<VariantIdentityInfo> class_;
	
	/**
	 * @see org.example.product.product.domain.product.model.VariantIdentityInfo#barcode
	 **/
	public static volatile SingularAttribute<VariantIdentityInfo, String> barcode;

}

