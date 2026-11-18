package com.snapshop.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import org.hibernate.annotations.Generated;

/** A product type ("กลุ่ม"): items of the same product sold by different shops. */
@Entity
@Table(name = "product_type")
public class ProductType {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "product_type_id")
	private Long id;

	/** Created by the database sequence, e.g. PT000001. */
	@Generated
	@Column(name = "product_type_code", insertable = false, updatable = false)
	private String code;

	@Column(name = "category_code", nullable = false)
	private String categoryCode;

	@Column(name = "type_name", nullable = false)
	private String typeName;

	@Column(name = "type_name_norm", nullable = false)
	private String typeNameNorm;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	protected ProductType() {
	}

	public ProductType(String categoryCode, String typeName, String typeNameNorm, Instant createdAt) {
		this.categoryCode = categoryCode;
		this.typeName = typeName;
		this.typeNameNorm = typeNameNorm;
		this.createdAt = createdAt;
	}

	public Long getId() {
		return id;
	}

	public String getCode() {
		return code;
	}

	public String getCategoryCode() {
		return categoryCode;
	}

	public String getTypeName() {
		return typeName;
	}

	public String getTypeNameNorm() {
		return typeNameNorm;
	}


	public Instant getCreatedAt() {
		return createdAt;
	}

}
