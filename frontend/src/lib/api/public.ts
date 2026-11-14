import "server-only";

import type { Page, ProductTypeDetail, ProductTypeSummary } from "@/types/api";
import { query, requestOrNull } from "./http";

export async function getProductTypes(params: { q?: string; page?: number }): Promise<Page<ProductTypeSummary>> {
  const page = await requestOrNull<Page<ProductTypeSummary>>(
    "/api/product-types" + query({ q: params.q, page: params.page, size: 24 }),
  );
  if (!page) {
    throw new Error("Backend returned 404 for /api/product-types");
  }
  return page;
}

export function getProductType(code: string): Promise<ProductTypeDetail | null> {
  return requestOrNull<ProductTypeDetail>(`/api/product-types/${encodeURIComponent(code)}`);
}
