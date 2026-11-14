package com.snapshop.repository;

import com.snapshop.dto.request.AdminProductSearchRequest;
import com.snapshop.dto.request.ProductSearchRequest;
import com.snapshop.dto.request.ProductSort;
import com.snapshop.dto.request.TypeSearchRequest;
import com.snapshop.dto.response.AdminProductResponse;
import com.snapshop.dto.response.AdminProductTypeResponse;
import com.snapshop.dto.response.PageResponse;
import com.snapshop.dto.response.PriceHistoryResponse;
import com.snapshop.dto.response.ProductResponse;
import com.snapshop.dto.response.ProductTypeSummaryResponse;
import com.snapshop.model.enums.GroupingMethod;
import com.snapshop.model.enums.ProductStatus;
import com.snapshop.model.enums.ShopType;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * Read-side queries. The user site reads only the view {@code public_product}, which holds the visibility rule.
 * Results are mapped straight to response records (read models), so no entity is loaded just to be copied.
 */
@Repository
public class ProductQueryRepository {

	private static final String PRODUCT_COLUMNS = """
			item_id, name_prefix, name_core, shop_name, shop_type, price, sold_text, affiliate_url, product_url,
			product_type_code, type_name""";

	private static final String TYPE_SUMMARY = """
			SELECT product_type_code, type_name, count(*) AS offer_count, min(price) AS min_price, max(price) AS max_price
			FROM public_product""";

	private static final String ADMIN_PRODUCT = """
			SELECT p.product_id, p.item_id, p.name_prefix, p.name_core, p.shop_name, p.shop_type, p.price, p.status,
			       p.last_seen_at, p.affiliate_url, p.product_url, t.product_type_code, t.type_name,
			       EXISTS (SELECT 1 FROM public_product pp WHERE pp.product_id = p.product_id) AS on_user_site
			FROM marketplace_product p JOIN product_type t ON t.product_type_id = p.product_type_id""";

	private static final String ADMIN_TYPE = """
			SELECT t.product_type_code, t.type_name, t.grouping_method, t.created_at,
			       count(p.product_id) AS active_count,
			       count(p.product_id) FILTER (WHERE p.status = 'OK') AS visible_count,
			       EXISTS (SELECT 1 FROM public_product pp WHERE pp.product_type_id = t.product_type_id) AS on_user_site
			FROM product_type t
			LEFT JOIN marketplace_product p ON p.product_type_id = t.product_type_id AND p.active_flag = 'Y'""";

	private final JdbcClient jdbc;

	public ProductQueryRepository(JdbcClient jdbc) {
		this.jdbc = jdbc;
	}

	// ---- user site ----

	public PageResponse<ProductTypeSummaryResponse> findPublicTypes(TypeSearchRequest r) {
		SqlWhere where = new SqlWhere().containsText(r.q(), "type_name");
		List<ProductTypeSummaryResponse> items = jdbc.sql(TYPE_SUMMARY + where.sql()
				+ " GROUP BY product_type_code, type_name ORDER BY type_name, product_type_code"
				+ page(r.page(), r.size()))
				.params(where.params()).query(ProductQueryRepository::typeSummary).list();
		long total = count("SELECT count(DISTINCT product_type_code) FROM public_product" + where.sql(), where);
		return new PageResponse<>(items, r.page(), r.size(), total);
	}

	public Optional<ProductTypeSummaryResponse> findPublicType(String code) {
		return jdbc.sql(TYPE_SUMMARY + " WHERE product_type_code = :code GROUP BY product_type_code, type_name")
				.param("code", code).query(ProductQueryRepository::typeSummary).optional();
	}

	public PageResponse<ProductResponse> findPublicProducts(ProductSearchRequest r) {
		SqlWhere where = new SqlWhere()
				.ifPresent(r.typeCode(), "product_type_code = :typeCode", "typeCode")
				.ifPresent(r.shopType() == null ? null : r.shopType().name(), "shop_type = :shopType", "shopType")
				.containsText(r.q(), "name_raw", "type_name");
		List<ProductResponse> items = jdbc.sql("SELECT " + PRODUCT_COLUMNS + " FROM public_product" + where.sql()
				+ orderBy(r.sort()) + page(r.page(), r.size()))
				.params(where.params()).query(ProductQueryRepository::product).list();
		long total = count("SELECT count(*) FROM public_product" + where.sql(), where);
		return new PageResponse<>(items, r.page(), r.size(), total);
	}

	public Optional<ProductResponse> findPublicProduct(long itemId) {
		return jdbc.sql("SELECT " + PRODUCT_COLUMNS + " FROM public_product WHERE item_id = :itemId")
				.param("itemId", itemId).query(ProductQueryRepository::product).optional();
	}

	public List<ProductResponse> findPublicOffers(String typeCode) {
		return jdbc.sql("SELECT " + PRODUCT_COLUMNS + " FROM public_product WHERE product_type_code = :code"
				+ orderBy(ProductSort.PRICE_ASC))
				.param("code", typeCode).query(ProductQueryRepository::product).list();
	}

	public List<PriceHistoryResponse> findPriceHistory(long itemId) {
		return jdbc.sql("""
				SELECT price, created_at, deactivated_at FROM marketplace_product
				WHERE item_id = :itemId AND active_flag = 'N' AND deactivation_reason = 'PRICE_CHANGED'
				ORDER BY product_id DESC""")
				.param("itemId", itemId)
				.query((rs, i) -> new PriceHistoryResponse(rs.getBigDecimal("price"), instant(rs, "created_at"),
						instant(rs, "deactivated_at")))
				.list();
	}

	// ---- admin ----

	public PageResponse<AdminProductResponse> findAdminProducts(AdminProductSearchRequest r) {
		SqlWhere where = new SqlWhere().always("p.active_flag = 'Y'")
				.ifPresent(r.status() == null ? null : r.status().name(), "p.status = :status", "status")
				.ifPresent(r.typeCode(), "t.product_type_code = :typeCode", "typeCode")
				.containsText(r.q(), "p.name_raw", "p.shop_name", "t.type_name");
		List<AdminProductResponse> items = jdbc.sql(ADMIN_PRODUCT + where.sql()
				+ " ORDER BY t.type_name, p.price NULLS LAST, p.item_id" + page(r.page(), r.size()))
				.params(where.params()).query(ProductQueryRepository::adminProduct).list();
		long total = count("SELECT count(*) FROM marketplace_product p JOIN product_type t"
				+ " ON t.product_type_id = p.product_type_id" + where.sql(), where);
		return new PageResponse<>(items, r.page(), r.size(), total);
	}

	public Optional<AdminProductResponse> findAdminProduct(long itemId) {
		return jdbc.sql(ADMIN_PRODUCT + " WHERE p.active_flag = 'Y' AND p.item_id = :itemId")
				.param("itemId", itemId).query(ProductQueryRepository::adminProduct).optional();
	}

	public PageResponse<AdminProductTypeResponse> findAdminTypes(TypeSearchRequest r) {
		SqlWhere where = new SqlWhere().containsText(r.q(), "t.type_name", "t.product_type_code");
		List<AdminProductTypeResponse> items = jdbc.sql(ADMIN_TYPE + where.sql()
				+ " GROUP BY t.product_type_id ORDER BY t.type_name, t.product_type_code" + page(r.page(), r.size()))
				.params(where.params()).query(ProductQueryRepository::adminType).list();
		long total = count("SELECT count(*) FROM product_type t" + where.sql(), where);
		return new PageResponse<>(items, r.page(), r.size(), total);
	}

	// ---- mapping ----

	private long count(String sql, SqlWhere where) {
		return jdbc.sql(sql).params(where.params()).query(Long.class).single();
	}

	private static String page(int page, int size) {
		return " LIMIT " + size + " OFFSET " + (long) page * size;
	}

	private static String orderBy(ProductSort sort) {
		return switch (sort) {
			case PRICE_ASC -> " ORDER BY price, item_id";
			case PRICE_DESC -> " ORDER BY price DESC, item_id";
			case NAME -> " ORDER BY name_core, item_id";
		};
	}

	/** The one place that decides the link users click. */
	private static String link(ResultSet rs) throws SQLException {
		String affiliate = rs.getString("affiliate_url");
		return affiliate != null ? affiliate : rs.getString("product_url");
	}

	private static ProductTypeSummaryResponse typeSummary(ResultSet rs, int i) throws SQLException {
		return new ProductTypeSummaryResponse(rs.getString("product_type_code"), rs.getString("type_name"),
				rs.getInt("offer_count"), rs.getBigDecimal("min_price"), rs.getBigDecimal("max_price"));
	}

	private static ProductResponse product(ResultSet rs, int i) throws SQLException {
		String prefix = rs.getString("name_prefix");
		String core = rs.getString("name_core");
		return new ProductResponse(rs.getLong("item_id"), prefix + core, prefix, core, rs.getString("shop_name"),
				ShopType.valueOf(rs.getString("shop_type")), rs.getBigDecimal("price"), rs.getString("sold_text"),
				link(rs), rs.getString("product_type_code"), rs.getString("type_name"));
	}

	private static AdminProductResponse adminProduct(ResultSet rs, int i) throws SQLException {
		return new AdminProductResponse(rs.getLong("product_id"), rs.getLong("item_id"),
				rs.getString("name_prefix") + rs.getString("name_core"), rs.getString("shop_name"),
				ShopType.valueOf(rs.getString("shop_type")), rs.getBigDecimal("price"),
				ProductStatus.valueOf(rs.getString("status")), rs.getString("product_type_code"),
				rs.getString("type_name"), rs.getBoolean("on_user_site"), instant(rs, "last_seen_at"), link(rs));
	}

	private static AdminProductTypeResponse adminType(ResultSet rs, int i) throws SQLException {
		return new AdminProductTypeResponse(rs.getString("product_type_code"), rs.getString("type_name"),
				GroupingMethod.valueOf(rs.getString("grouping_method")), rs.getInt("active_count"),
				rs.getInt("visible_count"), rs.getBoolean("on_user_site"), instant(rs, "created_at"));
	}

	private static Instant instant(ResultSet rs, String column) throws SQLException {
		java.sql.Timestamp ts = rs.getTimestamp(column);
		return ts == null ? null : ts.toInstant();
	}

}
