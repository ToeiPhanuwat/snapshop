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

	List<MarketplaceProduct> findByItemIdInAndActiveFlag(Collection<Long> itemIds, ActiveFlag activeFlag);

	/** Current items the feed still has to supply: waiting for a price, or never looked up. */
	@Query("""
			select p.itemId from MarketplaceProduct p
			where p.activeFlag = :flag and (p.status = :pending or p.feedSyncedAt is null)
			order by p.itemId""")
	List<Long> findItemIdsToLookUp(@Param("flag") ActiveFlag flag, @Param("pending") ProductStatus pending);

	Optional<MarketplaceProduct> findByItemIdAndActiveFlag(long itemId, ActiveFlag activeFlag);

	List<MarketplaceProduct> findByItemIdOrderByIdDesc(long itemId);

}
