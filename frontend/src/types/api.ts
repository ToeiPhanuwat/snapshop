// Types that mirror the backend response DTOs (backend/src/main/java/com/snapshop/dto/response).

export type ShopType = "MALL" | "PREFERRED" | "REGULAR";
export type ProductStatus = "OK" | "NOT_IN_LATEST" | "TEST" | "OUT_OF_STOCK" | "REVIEW" | "PRICE_PENDING";
export type ProductSort = "PRICE_ASC" | "PRICE_DESC" | "NAME";

export interface Page<T> {
  items: T[];
  page: number;
  size: number;
  total: number;
}

export interface ProductTypeSummary {
  code: string;
  name: string;
  offerCount: number;
  minPrice: number;
  maxPrice: number;
  /** Image of the cheapest offer that has one; null when no offer has an image. */
  imageUrl: string | null;
}

export interface Product {
  itemId: number;
  displayName: string;
  namePrefix: string;
  nameCore: string;
  shopName: string;
  shopType: ShopType;
  price: number;
  soldText: string | null;
  link: string;
  productTypeCode: string;
  typeName: string;
  imageUrl: string | null;
}

export interface ProductTypeDetail extends ProductTypeSummary {
  offers: Product[];
}

export interface AdminProduct {
  productId: number;
  itemId: number;
  displayName: string;
  shopName: string;
  shopType: ShopType;
  price: number | null;
  status: ProductStatus;
  productTypeCode: string;
  typeName: string;
  onUserSite: boolean;
  lastSeenAt: string;
  link: string;
}

/** NAME = กลุ่ม E (same English name); RULE = กลุ่ม R (same formula word). */
export type TypeKind = "NAME" | "RULE";

export interface AdminProductType {
  code: string;
  name: string;
  kind: TypeKind;
  /** The rule group a name group was merged into; null when not merged. */
  mergedIntoCode: string | null;
  activeCount: number;
  visibleCount: number;
  onUserSite: boolean;
  createdAt: string;
}

export interface ImportResult {
  batchId: number;
  categoryCode: string;
  sourceFiles: string;
  rowCount: number;
  insertedCount: number;
  priceChangedCount: number;
  unchangedCount: number;
  notInLatestCount: number;
  newTypeCount: number;
  importedAt: string;
}

/** Body of a 4xx answer (Spring ProblemDetail). `errors` is set when an import file is rejected. */
export interface Problem {
  detail?: string;
  errors?: string[];
}

export interface InboxFile {
  name: string;
  size: number;
  modifiedAt: string;
}

export interface ItemLine {
  itemId: number;
  name: string;
}

/** What importing a file would change; nothing is written yet. */
export interface ImportPreview {
  fileName: string;
  rowCount: number;
  insertedCount: number;
  priceChangedCount: number;
  unchangedCount: number;
  notInLatestCount: number;
  newTypeCount: number;
  newTypes: string[];
  /** Names of the rule groups (กลุ่ม R) the import would create. */
  newRuleTypes: string[];
  priceChanges: (ItemLine & { oldPrice: number | null; newPrice: number })[];
  toHide: ItemLine[];
  toReview: ItemLine[];
  /** Items the system will look up in the feed once the import is done. */
  feedItemCount: number;
  /** New items in a rule group that will wait for review. */
  ruleReviewCount: number;
  /** New items in a rule group that pass because their rule and name groups agree. */
  autoPassedCount: number;
}

/** A rule group (กลุ่ม R) with items waiting for review, and the name groups (กลุ่ม E) its items sit in. */
export interface RuleGroupReview {
  code: string;
  name: string;
  pendingCount: number;
  nameGroups: RuleNameGroup[];
}

export interface RuleNameGroup {
  code: string;
  name: string;
  /** The rule group this name group was already merged into; null when not merged. */
  mergedIntoCode: string | null;
  items: RuleGroupItem[];
}

export interface RuleGroupItem {
  itemId: number;
  displayName: string;
  shopName: string;
  price: number | null;
  status: ProductStatus;
  /** Whether the item belongs to this rule group. */
  inRule: boolean;
}

export type FeedSyncOutcome = "RUNNING" | "DONE" | "INCOMPLETE" | "RATE_LIMITED" | "FAILED";

export interface FeedSyncRun {
  runId: number;
  importBatchId: number | null;
  startedAt: string;
  finishedAt: string | null;
  requestedCount: number;
  foundCount: number;
  outcome: FeedSyncOutcome;
  message: string | null;
}
