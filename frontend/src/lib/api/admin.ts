import "server-only";

import { headers } from "next/headers";
import { basicAuthHeader, isAdminAuthorization } from "@/lib/auth";
import type { AdminProduct, AdminProductType, ImportResult, Page, ProductStatus } from "@/types/api";
import { query, request, type Result } from "./http";

/** Every admin call checks the caller again: proxy.ts is only the first gate. */
async function requireAdmin(): Promise<void> {
  if (!isAdminAuthorization((await headers()).get("authorization"))) {
    throw new Error("Unauthorized");
  }
}

async function adminRequest<T>(path: string, init: RequestInit = {}): Promise<Result<T>> {
  await requireAdmin();
  return request<T>(path, { ...init, headers: { ...init.headers, Authorization: basicAuthHeader() } });
}

async function adminGet<T>(path: string): Promise<T> {
  const result = await adminRequest<T>(path);
  if (!result.ok) {
    throw new Error(`Backend ${result.status} for ${path}: ${result.problem.detail ?? ""}`);
  }
  return result.data;
}

export function getAdminProducts(params: {
  status?: ProductStatus;
  typeCode?: string;
  q?: string;
  page?: number;
}): Promise<Page<AdminProduct>> {
  return adminGet("/api/admin/products" + query({ ...params, size: 100 }));
}

export function getAdminTypes(params: { q?: string; page?: number }): Promise<Page<AdminProductType>> {
  return adminGet("/api/admin/product-types" + query({ ...params, size: 100 }));
}

export function getRecentImports(): Promise<ImportResult[]> {
  return adminGet("/api/admin/imports");
}

export function importFile(file: File): Promise<Result<ImportResult>> {
  const body = new FormData();
  body.set("file", file);
  return adminRequest("/api/admin/imports", { method: "POST", body });
}

export function resolveReview(itemId: number): Promise<Result<AdminProduct>> {
  return adminRequest(`/api/admin/products/${itemId}/resolve`, { method: "POST" });
}

export function moveProduct(itemId: number, productTypeCode: string): Promise<Result<AdminProduct>> {
  return adminRequest(`/api/admin/products/${itemId}/move`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ productTypeCode }),
  });
}

export function markDiscontinued(itemId: number): Promise<Result<void>> {
  return adminRequest(`/api/admin/products/${itemId}/deactivate`, { method: "POST" });
}
