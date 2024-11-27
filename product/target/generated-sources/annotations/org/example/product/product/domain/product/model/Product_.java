package org.example.product.product.domain.product.model;

import jakarta.annotation.Generated;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.ListAttribute;
import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.StaticMetamodel;
import java.time.Instant;
import org.example.product.product.domain.product.model.Product.ProductStatus;

@StaticMetamodel(Product.class)
@Generated("org.hibernate.processor.HibernateProcessor")
public abstract class Product_ {

	public static final String IMAGES = "images";
	public static final String MODIFIED_ON = "modifiedOn";
	public static final String NAME = "name";
	public static final String AVAILABLE = "available";
	public static final String ID = "id";
	public static final String GENERAL_INFO = "generalInfo";
	public static final String VARIANTS = "variants";
	public static final String CREATED_ON = "createdOn";
	public static final String STATUS = "status";
	public static final String TAGS = "tags";

	
	/**
	 * @see org.example.product.product.domain.product.model.Product#images
	 **/
	public static volatile ListAttribute<Product, Image> images;
	
	/**
	 * @see org.example.product.product.domain.product.model.Product#modifiedOn
	 **/
	public static volatile SingularAttribute<Product, Instant> modifiedOn;
	
	/**
	 * @see org.example.product.product.domain.product.model.Product#name
	 **/
	public static volatile SingularAttribute<Product, String> name;
	
	/**
	 * @see org.example.product.product.domain.product.model.Product#available
	 **/
	public static volatile SingularAttribute<Product, Boolean> available;
	
	/**
	 * @see org.example.product.product.domain.product.model.Product#id
	 **/
	public static volatile SingularAttribute<Product, ProductId> id;
	
	/**
	 * @see org.example.product.product.domain.product.model.Product#generalInfo
	 **/
	public static volatile SingularAttribute<Product, ProductGeneralInfo> generalInfo;
	
	/**
	 * @see org.example.product.product.domain.product.model.Product#variants
	 **/
	public static volatile ListAttribute<Product, Variant> variants;
	
	/**
	 * @see org.example.product.product.domain.product.model.Product
	 **/
	public static volatile EntityType<Product> class_;
	
	/**
	 * @see org.example.product.product.domain.product.model.Product#createdOn
	 **/
	public static volatile SingularAttribute<Product, Instant> createdOn;
	
	/**
	 * @see org.example.product.product.domain.product.model.Product#status
	 **/
	public static volatile SingularAttribute<Product, ProductStatus> status;
	
	/**
	 * @see org.example.product.product.domain.product.model.Product#tags
	 **/
	public static volatile ListAttribute<Product, Tag> tags;

}

