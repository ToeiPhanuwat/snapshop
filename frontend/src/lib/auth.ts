// Admin login: HTTP Basic with the same ADMIN_USERNAME / ADMIN_PASSWORD the backend uses.
// The one place that decides whether a request is from the admin (used by proxy.ts and every admin action).

/** Constant-time string comparison, so response time does not reveal how much of a guess was right. */
function sameText(a: string, b: string): boolean {
  let diff = a.length ^ b.length;
  for (let i = 0; i < Math.max(a.length, b.length); i++) {
    diff |= (a.charCodeAt(i) || 0) ^ (b.charCodeAt(i) || 0);
  }
  return diff === 0;
}

export function adminCredentials(): { username: string; password: string } {
  const username = process.env.ADMIN_USERNAME;
  const password = process.env.ADMIN_PASSWORD;
  if (!username || !password) {
    throw new Error("ADMIN_USERNAME and ADMIN_PASSWORD must be set");
  }
  return { username, password };
}

export function basicAuthHeader(): string {
  const { username, password } = adminCredentials();
  return "Basic " + Buffer.from(`${username}:${password}`).toString("base64");
}

export function isAdminAuthorization(header: string | null): boolean {
  if (!header?.startsWith("Basic ")) {
    return false;
  }
  return sameText(header, basicAuthHeader());
}
