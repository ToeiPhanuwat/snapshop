// Sample backend data for component tests: each builder returns a valid value and takes the fields a test cares about.
import type { AdminProduct, FeedSyncRun, ImportPreview, ImportResult, Product } from "@/types/api";

export function feedRun(fields: Partial<FeedSyncRun> = {}): FeedSyncRun {
  return {
    runId: 1,
    importBatchId: 1,
    startedAt: "2026-11-17T03:00:00Z",
    finishedAt: "2026-11-17T03:05:00Z",
    requestedCount: 10,
    foundCount: 8,
    outcome: "DONE",
    message: null,
    ...fields,
  };
}

export function importResult(fields: Partial<ImportResult> = {}): ImportResult {
  return {
    batchId: 1,
    categoryCode: "DRPONG",
    sourceFiles: "drpong.csv",
    rowCount: 192,
    insertedCount: 12,
    priceChangedCount: 3,
    unchangedCount: 170,
    notInLatestCount: 7,
    newTypeCount: 2,
    importedAt: "2026-11-17T03:00:00Z",
    ...fields,
  };
}

export function adminProduct(fields: Partial<AdminProduct> = {}): AdminProduct {
  return {
    productId: 1,
    itemId: 1001,
    displayName: "Dr.PONG Sunscreen 30ml",
    shopName: "drpong official",
    shopType: "MALL",
    price: 399,
    status: "OK",
    productTypeCode: "PT000001",
    typeName: "Sunscreen 30ml",
    onUserSite: true,
    lastSeenAt: "2026-11-17T03:00:00Z",
    link: "https://shopee.co.th/product/1/1001",
    ...fields,
  };
}

export function offer(fields: Partial<Product> = {}): Product {
  return {
    itemId: 1001,
    displayName: "Dr.PONG Sunscreen 30ml",
    namePrefix: "Dr.PONG ",
    nameCore: "Sunscreen 30ml",
    shopName: "drpong official",
    shopType: "MALL",
    price: 399,
    soldText: null,
    link: "https://shopee.co.th/product/1/1001",
    productTypeCode: "PT000001",
    typeName: "Sunscreen 30ml",
    imageUrl: null,
    ...fields,
  };
}

export function importPreview(fields: Partial<ImportPreview> = {}): ImportPreview {
  return {
    fileName: "drpong.csv",
    rowCount: 0,
    insertedCount: 0,
    priceChangedCount: 0,
    unchangedCount: 0,
    notInLatestCount: 0,
    newTypeCount: 0,
    newTypes: [],
    priceChanges: [],
    toHide: [],
    toReview: [],
    feedItemCount: 0,
    ...fields,
  };
}

/** A promise the test settles itself, to look at the page while a Server Action is still running. */
export function deferred<T>() {
  let resolve!: (value: T) => void;
  const promise = new Promise<T>((r) => {
    resolve = r;
  });
  return { promise, resolve };
}
