// Mutation testing of the frontend (the counterpart of PIT in the backend): npm run mutation
/** @type {import('@stryker-mutator/api/core').PartialStrykerOptions} */
const config = {
  testRunner: "vitest",
  vitest: { configFile: "vitest.config.mts" },
  coverageAnalysis: "perTest",
  mutate: [
    "src/lib/**/*.ts",
    "src/proxy.ts",
    "src/app/admin/actions.ts",
    "src/components/**/*.tsx",
    "!src/**/*.test.{ts,tsx}",
  ],
  plugins: ["@stryker-mutator/*", "./stryker-ignore-styling.mjs"],
  ignorers: ["styling"],
  reporters: ["clear-text", "progress", "html"],
  htmlReporter: { fileName: "reports/mutation/index.html" },
  tempDirName: ".stryker-tmp",
  concurrency: 4,
};

export default config;
