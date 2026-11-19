package com.snapshop.service;

import com.snapshop.dto.response.RuleGroupReviewResponse;
import com.snapshop.dto.response.RuleGroupReviewResponse.Item;
import com.snapshop.dto.response.RuleGroupReviewResponse.NameGroup;
import com.snapshop.exception.BusinessRuleException;
import com.snapshop.exception.NotFoundException;
import com.snapshop.model.entity.MarketplaceProduct;
import com.snapshop.model.entity.ProductType;
import com.snapshop.model.enums.ActiveFlag;
import com.snapshop.model.enums.ProductStatus;
import com.snapshop.model.enums.TypeKind;
import com.snapshop.repository.ImportLockRepository;
import com.snapshop.repository.MarketplaceProductRepository;
import com.snapshop.repository.ProductTypeRepository;
import com.snapshop.service.importing.ProductName;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The admin compares a rule group (กลุ่ม R) with the name groups (กลุ่ม E) its items sit in, ticks the name groups to
 * merge into it, and confirms. Confirming merges the ticked name groups whole, passes every item of the rule group and
 * marks it confirmed, so it leaves the list until an import adds an item to it.
 */
@Service
@Transactional
public class RuleGroupReviewService {

	private final MarketplaceProductRepository products;
	private final ProductTypeRepository types;
	private final ImportLockRepository lock;

	public RuleGroupReviewService(MarketplaceProductRepository products, ProductTypeRepository types,
			ImportLockRepository lock) {
		this.products = products;
		this.types = types;
		this.lock = lock;
	}

	/**
	 * Rule groups with items waiting for review, and rule groups not yet confirmed whose items sit in two name groups
	 * or more (nothing waits there, but the name groups can be merged).
	 */
	@Transactional(readOnly = true)
	public List<RuleGroupReviewResponse> pending() {
		Set<Long> ruleTypeIds = new TreeSet<>(products.findRuleTypeIdsWithStatus(ActiveFlag.Y, ProductStatus.REVIEW));
		ruleTypeIds.addAll(products.findUnconfirmedRuleTypeIdsSpanningNameGroups(ActiveFlag.Y));
		return types.findAllById(ruleTypeIds).stream()
				.sorted(Comparator.comparing(ProductType::getTypeName).thenComparing(ProductType::getCode))
				.map(this::review)
				.toList();
	}

	/** Same lock as the import, so an import never runs half-way through a confirmation. Idempotent. */
	public void confirm(String ruleCode, List<String> mergeTypeCodes) {
		lock.lockForThisTransaction();
		ProductType rule = types.findByCode(ruleCode).filter(t -> t.getKind() == TypeKind.RULE)
				.orElseThrow(() -> new NotFoundException("ไม่พบกลุ่ม R " + ruleCode));
		List<MarketplaceProduct> members = products.findByRuleTypeIdAndActiveFlag(rule.getId(), ActiveFlag.Y);
		Set<Long> nameTypeIds = nameTypeIds(members);
		for (String code : mergeTypeCodes) {
			ProductType nameGroup = types.findByCode(code)
					.orElseThrow(() -> new NotFoundException("ไม่พบกลุ่มสินค้า " + code));
			if (!nameTypeIds.contains(nameGroup.getId())) {
				throw new BusinessRuleException("กลุ่ม " + code + " ไม่มีสินค้าของกลุ่ม R " + ruleCode);
			}
			nameGroup.mergeInto(rule);
		}
		members.stream().filter(p -> p.getStatus() == ProductStatus.REVIEW).forEach(MarketplaceProduct::passReview);
		rule.markMergeReviewed(Instant.now());
	}

	private RuleGroupReviewResponse review(ProductType rule) {
		List<MarketplaceProduct> members = products.findByRuleTypeIdAndActiveFlag(rule.getId(), ActiveFlag.Y);
		Set<Long> nameTypeIds = nameTypeIds(members);
		Map<Long, List<MarketplaceProduct>> itemsByType = products
				.findByProductTypeIdInAndActiveFlagOrderByItemId(nameTypeIds, ActiveFlag.Y).stream()
				.collect(Collectors.groupingBy(MarketplaceProduct::getProductTypeId));
		List<ProductType> nameGroups = types.findAllById(nameTypeIds).stream()
				.sorted(Comparator.comparing(ProductType::getCode)).toList();
		Map<Long, String> codeById = codes(nameGroups);
		int pending = (int) members.stream().filter(p -> p.getStatus() == ProductStatus.REVIEW).count();
		return new RuleGroupReviewResponse(rule.getCode(), rule.getTypeName(), pending, nameGroups.stream()
				.map(t -> new NameGroup(t.getCode(), t.getTypeName(), codeById.get(t.getMergedIntoTypeId()),
						itemsByType.get(t.getId()).stream().map(p -> item(p, rule)).toList()))
				.toList());
	}

	/** Codes of the rule groups the name groups were merged into. */
	private Map<Long, String> codes(List<ProductType> nameGroups) {
		Set<Long> mergedInto = nameGroups.stream().map(ProductType::getMergedIntoTypeId).filter(Objects::nonNull)
				.collect(Collectors.toSet());
		return types.findAllById(mergedInto).stream()
				.collect(Collectors.toMap(ProductType::getId, ProductType::getCode));
	}

	private static Set<Long> nameTypeIds(List<MarketplaceProduct> members) {
		return members.stream().map(MarketplaceProduct::getProductTypeId)
				.collect(Collectors.toCollection(TreeSet::new));
	}

	private static Item item(MarketplaceProduct p, ProductType rule) {
		return new Item(p.getItemId(), new ProductName(p.getNamePrefix(), p.getNameCore()).raw(), p.getShopName(),
				p.getPrice(), p.getStatus(), rule.getId().equals(p.getRuleTypeId()));
	}

}
