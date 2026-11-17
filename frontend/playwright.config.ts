import { existsSync } from "node:fs";
import path from "node:path";
import { defineConfig, devices } from "@playwright/test";

// The test stack's values (admin login, passwords); see .env.test.example at the repository root.
const envFile = path.join(__dirname, "..", ".env.test");
if (!existsSync(envFile)) {
  throw new Error(`Missing ${envFile}: copy .env.test.example to .env.test and set the passwords`);
}
process.loadEnvFile(envFile);

/**
 * End-to-end tests against the docker compose stack started by `npm run e2e:up` (project "snapshop-e2e").
 * Every test shares one database, so they run one at a time: "seed" imports the sample file first.
 */
export default defineConfig({
  testDir: "./e2e",
  fullyParallel: false,
  workers: 1,
  retries: 0,
  reporter: [["list"], ["html", { open: "never" }]],
  globalSetup: "./e2e/global-setup.ts",
  use: {
    baseURL: "http://localhost:3000",
    trace: "retain-on-failure",
    ...devices["Desktop Chrome"],
  },
  projects: [
    { name: "seed", testMatch: "admin-import.spec.ts" },
    { name: "e2e", testIgnore: "admin-import.spec.ts", dependencies: ["seed"] },
  ],
});
