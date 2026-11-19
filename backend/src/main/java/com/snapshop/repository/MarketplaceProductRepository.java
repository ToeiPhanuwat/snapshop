package com.snapshop.repository;

import com.snapshop.model.entity.MarketplaceProduct;
import com.snapshop.model.enums.ActiveFlag;
import com.snapshop.model.enums.ProductStatus;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MarketplaceProductRepository extends JpaRepository<MarketplaceProduct, Long> {

	@Query("""
			select p from MarketplaceProduct p
			where p.activeFlag = :flag
			  and p.productTypeId in (select t.id from ProductType t where t.categoryCode = :category)""")
	List<MarketplaceProduct> findInCategory(@Param("category") String category, @Param("flag") ActiveFlag flag);

	/** Product type of every version of every item in the category, oldest version first. */
	@Query("""
			select new com.snapshop.repository.ItemType(p.itemId, p.productTypeId) from MarketplaceProduct p
			where p.productTypeId in (select t.id from ProductType t where t.categoryCode = :category)
			order by p.id""")
	List<ItemType> findItemTypesInCategory(@Param("category") String category);

	/** Rule group of every version of every item in the category that has one, oldest version first. */
	@Query("""
			select new com.snapshop.repository.ItemType(p.itemId, p.ruleTypeId) from MarketplaceProduct p
			where p.ruleTypeId is not null
			  and p.productTypeId in (select t.id from ProductType t where t.categoryCode = :category)
			order by p.id""")
	List<ItemType> findItemRuleTypesInCategory(@Param("category") String category);

	List<MarketplaceProduct> findByItemIdInAndActiveFlag(Collection<Long> itemIds, ActiveFlag activeFlag);

	/** Rule groups that still have current items in the given status. */
	@Query("""
			select distinct p.ruleTypeId from MarketplaceProduct p
			where p.activeFlag = :flag and p.status = :status and p.ruleTypeId is not null""")
	List<Long> findRuleTypeIdsWithStatus(@Param("flag") ActiveFlag flag, @Param("status") ProductStatus status);

	/** Rule groups the admin has not confirmed whose current items sit in two name groups or more. */
	@Query("""
			select p.ruleTypeId from MarketplaceProduct p, ProductType r
			where r.id = p.ruleTypeId and r.mergeReviewedAt is null and p.activeFlag = :flag
			group by p.ruleTypeId
			having count(distinct p.productTypeId) >= 2""")
	List<Long> findUnconfirmedRuleTypeIdsSpanningNameGroups(@Param("flag") ActiveFlag flag);

	List<MarketplaceProduct> findByRuleTypeIdAndActiveFlag(long ruleTypeId, ActiveFlag activeFlag);

	List<MarketplaceProduct> findByProductTypeIdInAndActiveFlagOrderByItemId(Collection<Long> productTypeIds,
			ActiveFlag activeFlag);

	/** Current items the feed still has to supply: waiting for a price, or never looked up. */
	@Query("""
			select p.itemId from MarketplaceProduct p
			where p.activeFlag = :flag and (p.status = :pending or p.feedSyncedAt is null)
			order by p.itemId""")
	List<Long> findItemIdsToLookUp(@Param("flag") ActiveFlag flag, @Param("pending") ProductStatus pending);

	Optional<MarketplaceProduct> findByItemIdAndActiveFlag(long itemId, ActiveFlag activeFlag);

	List<MarketplaceProduct> findByItemIdOrderByIdDesc(long itemId);

}
