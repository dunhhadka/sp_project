package org.example.product.product.domain.product.model;

import jakarta.annotation.Generated;
import jakarta.persistence.metamodel.EmbeddableType;
import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.StaticMetamodel;

@StaticMetamodel(VariantPhysicalInfo.class)
@Generated("org.hibernate.processor.HibernateProcessor")
public abstract class VariantPhysicalInfo_ {

	public static final String UNIT = "unit";
	public static final String WEIGHT = "weight";
	public static final String WEIGHT_UNIT = "weightUnit";

	
	/**
	 * @see org.example.product.product.domain.product.model.VariantPhysicalInfo#unit
	 **/
	public static volatile SingularAttribute<VariantPhysicalInfo, String> unit;
	
	/**
	 * @see org.example.product.product.domain.product.model.VariantPhysicalInfo#weight
	 **/
	public static volatile SingularAttribute<VariantPhysicalInfo, Double> weight;
	
	/**
	 * @see org.example.product.product.domain.product.model.VariantPhysicalInfo
	 **/
	public static volatile EmbeddableType<VariantPhysicalInfo> class_;
	
	/**
	 * @see org.example.product.product.domain.product.model.VariantPhysicalInfo#weightUnit
	 **/
	public static volatile SingularAttribute<VariantPhysicalInfo, String> weightUnit;

}

