package com.snapshop.model.entity;

import com.snapshop.exception.BusinessRuleException;
import com.snapshop.model.enums.TypeKind;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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

	@Enumerated(EnumType.STRING)
	@Column(name = "kind", nullable = false, updatable = false)
	private TypeKind kind;

	/** The rule group this name group was merged into; null when not merged. */
	@Column(name = "merged_into_type_id")
	private Long mergedIntoTypeId;

	/** When the admin confirmed this rule group; null until then, and again after an import adds an item to it. */
	@Column(name = "merge_reviewed_at")
	private Instant mergeReviewedAt;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	protected ProductType() {
	}

	public ProductType(String categoryCode, TypeKind kind, String typeName, String typeNameNorm, Instant createdAt) {
		this.categoryCode = categoryCode;
		this.kind = kind;
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

	public TypeKind getKind() {
		return kind;
	}

	public Long getMergedIntoTypeId() {
		return mergedIntoTypeId;
	}

	/** The admin merges this whole name group into a rule group; merging into the same one again changes nothing. */
	public void mergeInto(ProductType ruleGroup) {
		if (kind != TypeKind.NAME || ruleGroup.kind != TypeKind.RULE) {
			throw new BusinessRuleException("รวมได้เฉพาะกลุ่ม E เข้ากลุ่ม R");
		}
		if (mergedIntoTypeId != null && !mergedIntoTypeId.equals(ruleGroup.id)) {
			throw new BusinessRuleException("กลุ่ม " + code + " รวมเข้ากลุ่ม R อื่นแล้ว");
		}
		this.mergedIntoTypeId = ruleGroup.id;
	}

	public Instant getMergeReviewedAt() {
		return mergeReviewedAt;
	}

	/** The admin confirmed this rule group; confirming again keeps the first time. */
	public void markMergeReviewed(Instant at) {
		if (kind != TypeKind.RULE) {
			throw new BusinessRuleException("ยืนยันได้เฉพาะกลุ่ม R");
		}
		if (mergeReviewedAt == null) {
			mergeReviewedAt = at;
		}
	}

	/** A new item joined this rule group, so the admin should look at it again. */
	public void reopenMergeReview() {
		mergeReviewedAt = null;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

}
