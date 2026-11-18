import { copyFileSync, mkdirSync } from "node:fs";
import path from "node:path";

/**
 * The sample file every run imports: six name groups, one price written as "1.0พัน", one item without a pair,
 * a rule group spread over two name groups (waits for review) and one that equals its name group (passes).
 */
export const sampleFile = path.join(__dirname, "fixtures", "e2e-import.csv");

export const SUNSCREEN = "Dr.PONG E2E Sunscreen SPF50";
export const SERUM = "Dr.PONG E2E Vitamin C Serum";
export const NIGHT_CREAM = "Dr.PONG E2E Night Cream";
/** Rule group over the name groups AQUA_GEL_SMALL and AQUA_GEL_BOX. */
export const AQUA_GEL = "E2E Aqua Gel";
export const AQUA_GEL_SMALL = "Dr.PONG E2E Aqua Gel 10 g";
export const AQUA_GEL_BOX = "Dr.PONG E2E Aqua Gel x6";
/** Name group whose items are exactly the rule group "E2E CoQ10". */
export const COQ10 = "Dr.PONG E2E CoQ10 30 mg";

/** The test stack's admin login from .env.test, for `test.use({ httpCredentials: adminLogin() })`. */
export function adminLogin(): { username: string; password: string } {
  const username = process.env.ADMIN_USERNAME;
  const password = process.env.ADMIN_PASSWORD;
  if (!username || !password) {
    throw new Error("ADMIN_USERNAME and ADMIN_PASSWORD must be set in .env.test");
  }
  return { username, password };
}

/** Puts a copy of the sample file in the test stack's inbox folder under `name`. */
export function putInInbox(name: string): void {
  const inbox = path.resolve(__dirname, "..", "..", process.env.INBOX_DIR ?? "");
  if (!inbox.endsWith(path.join("e2e", ".inbox"))) {
    throw new Error(`INBOX_DIR must be the test inbox (frontend/e2e/.inbox), not ${inbox}`);
  }
  mkdirSync(inbox, { recursive: true });
  copyFileSync(sampleFile, path.join(inbox, name));
}
