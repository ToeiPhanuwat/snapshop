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

export interface AdminProductType {
  code: string;
  name: string;
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
  priceChanges: (ItemLine & { oldPrice: number | null; newPrice: number })[];
  toHide: ItemLine[];
  toReview: ItemLine[];
  /** Items the system will look up in the feed once the import is done. */
  feedItemCount: number;
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
