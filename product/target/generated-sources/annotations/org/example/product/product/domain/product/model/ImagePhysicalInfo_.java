package org.example.product.product.domain.product.model;

import jakarta.annotation.Generated;
import jakarta.persistence.metamodel.EmbeddableType;
import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.StaticMetamodel;

@StaticMetamodel(ImagePhysicalInfo.class)
@Generated("org.hibernate.processor.HibernateProcessor")
public abstract class ImagePhysicalInfo_ {

	public static final String SIZE = "size";
	public static final String WIDTH = "width";
	public static final String HEIGHT = "height";

	
	/**
	 * @see org.example.product.product.domain.product.model.ImagePhysicalInfo#size
	 **/
	public static volatile SingularAttribute<ImagePhysicalInfo, Integer> size;
	
	/**
	 * @see org.example.product.product.domain.product.model.ImagePhysicalInfo#width
	 **/
	public static volatile SingularAttribute<ImagePhysicalInfo, Integer> width;
	
	/**
	 * @see org.example.product.product.domain.product.model.ImagePhysicalInfo
	 **/
	public static volatile EmbeddableType<ImagePhysicalInfo> class_;
	
	/**
	 * @see org.example.product.product.domain.product.model.ImagePhysicalInfo#height
	 **/
	public static volatile SingularAttribute<ImagePhysicalInfo, Integer> height;

}

