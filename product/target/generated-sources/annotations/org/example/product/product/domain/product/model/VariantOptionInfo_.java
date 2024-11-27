package org.example.product.product.domain.product.model;

import jakarta.annotation.Generated;
import jakarta.persistence.metamodel.EmbeddableType;
import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.StaticMetamodel;

@StaticMetamodel(VariantOptionInfo.class)
@Generated("org.hibernate.processor.HibernateProcessor")
public abstract class VariantOptionInfo_ {

	public static final String OPTION3 = "option3";
	public static final String OPTION1 = "option1";
	public static final String OPTION2 = "option2";

	
	/**
	 * @see org.example.product.product.domain.product.model.VariantOptionInfo#option3
	 **/
	public static volatile SingularAttribute<VariantOptionInfo, String> option3;
	
	/**
	 * @see org.example.product.product.domain.product.model.VariantOptionInfo#option1
	 **/
	public static volatile SingularAttribute<VariantOptionInfo, String> option1;
	
	/**
	 * @see org.example.product.product.domain.product.model.VariantOptionInfo#option2
	 **/
	public static volatile SingularAttribute<VariantOptionInfo, String> option2;
	
	/**
	 * @see org.example.product.product.domain.product.model.VariantOptionInfo
	 **/
	public static volatile EmbeddableType<VariantOptionInfo> class_;

}

