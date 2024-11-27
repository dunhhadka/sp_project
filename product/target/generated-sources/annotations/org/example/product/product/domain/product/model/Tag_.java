package org.example.product.product.domain.product.model;

import jakarta.annotation.Generated;
import jakarta.persistence.metamodel.EmbeddableType;
import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.StaticMetamodel;

@StaticMetamodel(Tag.class)
@Generated("org.hibernate.processor.HibernateProcessor")
public abstract class Tag_ {

	public static final String NAME = "name";

	
	/**
	 * @see org.example.product.product.domain.product.model.Tag#name
	 **/
	public static volatile SingularAttribute<Tag, String> name;
	
	/**
	 * @see org.example.product.product.domain.product.model.Tag
	 **/
	public static volatile EmbeddableType<Tag> class_;

}

