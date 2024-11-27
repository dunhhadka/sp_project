package org.example.product.product.domain.product.model;

import jakarta.annotation.Generated;
import jakarta.persistence.metamodel.EmbeddableType;
import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.StaticMetamodel;

@StaticMetamodel(ProductGeneralInfo.class)
@Generated("org.hibernate.processor.HibernateProcessor")
public abstract class ProductGeneralInfo_ {

	public static final String SUMMARY = "summary";
	public static final String VENDOR = "vendor";
	public static final String DESCRIPTION = "description";
	public static final String TITLE = "title";
	public static final String PRODUCT_TYPE = "productType";

	
	/**
	 * @see org.example.product.product.domain.product.model.ProductGeneralInfo#summary
	 **/
	public static volatile SingularAttribute<ProductGeneralInfo, String> summary;
	
	/**
	 * @see org.example.product.product.domain.product.model.ProductGeneralInfo#vendor
	 **/
	public static volatile SingularAttribute<ProductGeneralInfo, String> vendor;
	
	/**
	 * @see org.example.product.product.domain.product.model.ProductGeneralInfo#description
	 **/
	public static volatile SingularAttribute<ProductGeneralInfo, String> description;
	
	/**
	 * @see org.example.product.product.domain.product.model.ProductGeneralInfo#title
	 **/
	public static volatile SingularAttribute<ProductGeneralInfo, String> title;
	
	/**
	 * @see org.example.product.product.domain.product.model.ProductGeneralInfo
	 **/
	public static volatile EmbeddableType<ProductGeneralInfo> class_;
	
	/**
	 * @see org.example.product.product.domain.product.model.ProductGeneralInfo#productType
	 **/
	public static volatile SingularAttribute<ProductGeneralInfo, String> productType;

}

