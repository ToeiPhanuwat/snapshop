import "server-only";

import type { Problem } from "@/types/api";

/** Backend base URL; only the Next.js server talks to the backend, never the browser. */
export function backendUrl(path: string): string {
  const base = process.env.BACKEND_URL ?? "http://localhost:8080";
  return base.replace(/\/$/, "") + path;
}

/** Query string from optional values; empty values are left out. */
export function query(params: Record<string, string | number | undefined | null>): string {
  const search = new URLSearchParams();
  for (const [key, value] of Object.entries(params)) {
    if (value !== undefined && value !== null && value !== "") {
      search.set(key, String(value));
    }
  }
  const text = search.toString();
  return text ? `?${text}` : "";
}

export type Result<T> = { ok: true; data: T } | { ok: false; status: number; problem: Problem };

/** Fetch JSON without caching: prices must always be current. */
export async function request<T>(path: string, init: RequestInit = {}): Promise<Result<T>> {
  const response = await fetch(backendUrl(path), { ...init, cache: "no-store" });
  if (response.status === 204) {
    return { ok: true, data: undefined as T };
  }
  const body = await response.json().catch(() => ({}));
  return response.ok ? { ok: true, data: body as T } : { ok: false, status: response.status, problem: body };
}

/** Same as request, but a failure that is not 404 is a bug or an outage, so it throws to the error page. */
export async function requestOrNull<T>(path: string, init?: RequestInit): Promise<T | null> {
  const result = await request<T>(path, init);
  if (result.ok) {
    return result.data;
  }
  if (result.status === 404) {
    return null;
  }
  throw new Error(`Backend ${result.status} for ${path}: ${result.problem.detail ?? ""}`);
}
