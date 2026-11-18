package com.snapshop.model.entity;

import com.snapshop.exception.BusinessRuleException;
import com.snapshop.model.enums.ActiveFlag;
import com.snapshop.model.enums.DeactivationReason;
import com.snapshop.model.enums.ProductStatus;
import com.snapshop.model.enums.ShopType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * One price version of an item. Fields describing the listing are fixed once the version is created, except those
 * the feed fills in later: price (when the file only had an abbreviated one), shop type, image and the extras.
 */
@Entity
@Table(name = "marketplace_product")
public class MarketplaceProduct {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "product_id")
	private Long id;

	@Column(name = "item_id", nullable = false, updatable = false)
	private long itemId;

	@Column(name = "shop_id", nullable = false, updatable = false)
	private long shopId;

	@Column(name = "shop_name", nullable = false, updatable = false)
	private String shopName;

	@Enumerated(EnumType.STRING)
	@Column(name = "shop_type", nullable = false)
	private ShopType shopType;

	@Column(name = "name_prefix", nullable = false, updatable = false)
	private String namePrefix;

	@Column(name = "name_core", nullable = false, updatable = false)
	private String nameCore;


	@Column(name = "product_type_id", nullable = false)
	private long productTypeId;

	@Column(name = "price")
	private BigDecimal price;

	@Column(name = "sold_text", updatable = false)
	private String soldText;

	@Column(name = "commission_rate", updatable = false)
	private BigDecimal commissionRate;

	@Column(name = "commission_amount", updatable = false)
	private BigDecimal commissionAmount;

	@Column(name = "product_url", nullable = false, updatable = false)
	private String productUrl;

	@Column(name = "affiliate_url", updatable = false)
	private String affiliateUrl;

	@Column(name = "image_url")
	private String imageUrl;

	@Column(name = "list_price")
	private BigDecimal listPrice;

	@Column(name = "stock")
	private Integer stock;

	@Column(name = "item_rating")
	private BigDecimal itemRating;

	@Column(name = "shop_rating")
	private BigDecimal shopRating;

	@Column(name = "feed_synced_at")
	private Instant feedSyncedAt;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false)
	private ProductStatus status;

	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.CHAR)
	@Column(name = "active_flag", nullable = false, length = 1)
	private ActiveFlag activeFlag;

	@Column(name = "import_batch_id", nullable = false, updatable = false)
	private long importBatchId;

	@Column(name = "last_seen_batch_id", nullable = false)
	private long lastSeenBatchId;

	@Column(name = "last_seen_at", nullable = false)
	private Instant lastSeenAt;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "deactivated_at")
	private Instant deactivatedAt;

	@Enumerated(EnumType.STRING)
	@Column(name = "deactivation_reason")
	private DeactivationReason deactivationReason;

	protected MarketplaceProduct() {
	}

	/** A new current version, first seen in import batch {@code batchId}. */
	public MarketplaceProduct(Listing listing, long productTypeId, ProductStatus status, long batchId, Instant now) {
		this.itemId = listing.itemId();
		this.shopId = listing.shopId();
		this.shopName = listing.shopName();
		this.shopType = ShopType.REGULAR;
		this.namePrefix = listing.namePrefix();
		this.nameCore = listing.nameCore();
		this.price = listing.price();
		this.soldText = listing.soldText();
		this.commissionRate = listing.commissionRate();
		this.commissionAmount = listing.commissionAmount();
		this.productUrl = listing.productUrl();
		this.affiliateUrl = listing.affiliateUrl();
		this.productTypeId = productTypeId;
		this.status = status;
		this.activeFlag = ActiveFlag.Y;
		this.importBatchId = batchId;
		this.lastSeenBatchId = batchId;
		this.lastSeenAt = now;
		this.createdAt = now;
	}

	/** What the import file says about an item. */
	public record Listing(long itemId, long shopId, String shopName, String namePrefix, String nameCore,
			BigDecimal price, String soldText, BigDecimal commissionRate, BigDecimal commissionAmount,
			String productUrl, String affiliateUrl) {
	}

	/** What the feed says about the shop and the item; a null value means the feed has none and keeps what is stored. */
	public record FeedExtras(ShopType shopType, BigDecimal listPrice, Integer stock, BigDecimal itemRating,
			BigDecimal shopRating, String imageUrl) {
	}

	public void applyFeed(FeedExtras extras, Instant now) {
		this.shopType = extras.shopType();
		this.listPrice = extras.listPrice();
		this.stock = extras.stock();
		this.itemRating = extras.itemRating();
		this.shopRating = extras.shopRating();
		if (extras.imageUrl() != null) {
			this.imageUrl = extras.imageUrl();
		}
		this.feedSyncedAt = now;
	}

	/** The file had no usable price; the feed's price makes an item that was waiting for it ready to show. */
	public void fillPriceFromFeed(BigDecimal feedPrice) {
		this.price = feedPrice;
		if (status == ProductStatus.PRICE_PENDING) {
			this.status = ProductStatus.OK;
		}
	}

	/** The next price version of this item: same listing and extras, new price, first seen in {@code batchId}. */
	public MarketplaceProduct nextPriceVersion(BigDecimal newPrice, long batchId, Instant now) {
		MarketplaceProduct next = new MarketplaceProduct();
		next.itemId = itemId;
		next.shopId = shopId;
		next.shopName = shopName;
		next.shopType = shopType;
		next.namePrefix = namePrefix;
		next.nameCore = nameCore;
		next.productTypeId = productTypeId;
		next.price = newPrice;
		next.soldText = soldText;
		next.commissionRate = commissionRate;
		next.commissionAmount = commissionAmount;
		next.productUrl = productUrl;
		next.affiliateUrl = affiliateUrl;
		next.imageUrl = imageUrl;
		next.listPrice = listPrice;
		next.stock = stock;
		next.itemRating = itemRating;
		next.shopRating = shopRating;
		next.feedSyncedAt = feedSyncedAt;
		next.status = status;
		next.activeFlag = ActiveFlag.Y;
		next.importBatchId = batchId;
		next.lastSeenBatchId = batchId;
		next.lastSeenAt = now;
		next.createdAt = now;
		return next;
	}

	public void markSeen(ProductStatus newStatus, long batchId, Instant now) {
		this.status = newStatus;
		this.lastSeenBatchId = batchId;
		this.lastSeenAt = now;
	}

	public void markNotInLatest() {
		this.status = ProductStatus.NOT_IN_LATEST;
	}

	public void deactivate(DeactivationReason reason, Instant now) {
		this.activeFlag = ActiveFlag.N;
		this.deactivationReason = reason;
		this.deactivatedAt = now;
	}

	/** Admin checked the item. An item without a known price cannot be shown, so it stays in REVIEW. */
	public void resolveReview() {
		if (status != ProductStatus.REVIEW) {
			throw new BusinessRuleException("สินค้า " + itemId + " ไม่ได้อยู่ในสถานะ REVIEW");
		}
		if (price == null) {
			throw new BusinessRuleException("สินค้า " + itemId + " ยังไม่มีราคา รอนำเข้ารอบที่มีราคาเต็ม");
		}
		this.status = ProductStatus.OK;
	}

	public void moveTo(long newProductTypeId) {
		this.productTypeId = newProductTypeId;
	}

	public Long getId() {
		return id;
	}

	public long getItemId() {
		return itemId;
	}

	public long getShopId() {
		return shopId;
	}

	public String getShopName() {
		return shopName;
	}

	public ShopType getShopType() {
		return shopType;
	}

	public String getNamePrefix() {
		return namePrefix;
	}

	public String getNameCore() {
		return nameCore;
	}

	public long getProductTypeId() {
		return productTypeId;
	}

	public BigDecimal getPrice() {
		return price;
	}

	public String getSoldText() {
		return soldText;
	}

	public BigDecimal getCommissionRate() {
		return commissionRate;
	}

	public BigDecimal getCommissionAmount() {
		return commissionAmount;
	}

	public String getProductUrl() {
		return productUrl;
	}

	public String getAffiliateUrl() {
		return affiliateUrl;
	}

	public String getImageUrl() {
		return imageUrl;
	}

	public BigDecimal getListPrice() {
		return listPrice;
	}

	public Integer getStock() {
		return stock;
	}

	public BigDecimal getItemRating() {
		return itemRating;
	}

	public BigDecimal getShopRating() {
		return shopRating;
	}

	public Instant getFeedSyncedAt() {
		return feedSyncedAt;
	}

	public ProductStatus getStatus() {
		return status;
	}

	public ActiveFlag getActiveFlag() {
		return activeFlag;
	}

	public long getLastSeenBatchId() {
		return lastSeenBatchId;
	}

	public Instant getLastSeenAt() {
		return lastSeenAt;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getDeactivatedAt() {
		return deactivatedAt;
	}

	public DeactivationReason getDeactivationReason() {
		return deactivationReason;
	}

}
