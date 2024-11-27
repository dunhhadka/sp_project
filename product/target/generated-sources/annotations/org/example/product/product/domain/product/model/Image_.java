package org.example.product.product.domain.product.model;

import jakarta.annotation.Generated;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.StaticMetamodel;
import java.time.Instant;

@StaticMetamodel(Image.class)
@Generated("org.hibernate.processor.HibernateProcessor")
public abstract class Image_ {

	public static final String AGG_ROOT = "aggRoot";
	public static final String PHYSICAL_INFO = "physicalInfo";
	public static final String CREATED_AT = "createdAt";
	public static final String FILE_NAME = "fileName";
	public static final String SRC = "src";
	public static final String MODIFIED_AT = "modifiedAt";
	public static final String ALT = "alt";
	public static final String ID = "id";
	public static final String POSITION = "position";

	
	/**
	 * @see org.example.product.product.domain.product.model.Image#aggRoot
	 **/
	public static volatile SingularAttribute<Image, Product> aggRoot;
	
	/**
	 * @see org.example.product.product.domain.product.model.Image#physicalInfo
	 **/
	public static volatile SingularAttribute<Image, ImagePhysicalInfo> physicalInfo;
	
	/**
	 * @see org.example.product.product.domain.product.model.Image#createdAt
	 **/
	public static volatile SingularAttribute<Image, Instant> createdAt;
	
	/**
	 * @see org.example.product.product.domain.product.model.Image#fileName
	 **/
	public static volatile SingularAttribute<Image, String> fileName;
	
	/**
	 * @see org.example.product.product.domain.product.model.Image#src
	 **/
	public static volatile SingularAttribute<Image, String> src;
	
	/**
	 * @see org.example.product.product.domain.product.model.Image#modifiedAt
	 **/
	public static volatile SingularAttribute<Image, Instant> modifiedAt;
	
	/**
	 * @see org.example.product.product.domain.product.model.Image#alt
	 **/
	public static volatile SingularAttribute<Image, String> alt;
	
	/**
	 * @see org.example.product.product.domain.product.model.Image#id
	 **/
	public static volatile SingularAttribute<Image, Integer> id;
	
	/**
	 * @see org.example.product.product.domain.product.model.Image#position
	 **/
	public static volatile SingularAttribute<Image, Integer> position;
	
	/**
	 * @see org.example.product.product.domain.product.model.Image
	 **/
	public static volatile EntityType<Image> class_;

}

