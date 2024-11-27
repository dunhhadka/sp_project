package org.example.product.product.domain.product.model;

import jakarta.annotation.Generated;
import jakarta.persistence.metamodel.EmbeddableType;
import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.StaticMetamodel;

@StaticMetamodel(ProductId.class)
@Generated("org.hibernate.processor.HibernateProcessor")
public abstract class ProductId_ {

	public static final String ID = "id";
	public static final String STORE_ID = "storeId";

	
	/**
	 * @see org.example.product.product.domain.product.model.ProductId#id
	 **/
	public static volatile SingularAttribute<ProductId, Integer> id;
	
	/**
	 * @see org.example.product.product.domain.product.model.ProductId#storeId
	 **/
	public static volatile SingularAttribute<ProductId, Integer> storeId;
	
	/**
	 * @see org.example.product.product.domain.product.model.ProductId
	 **/
	public static volatile EmbeddableType<ProductId> class_;

}

