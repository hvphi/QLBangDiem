import { defineConfig } from "@playwright/test";

const chromiumExecutable = process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE;

export default defineConfig({
  testDir: "./e2e",
  testMatch: "*.spec.ts",
  timeout: 30_000,
  fullyParallel: false,
  workers: 1,
  outputDir: "../.local/e2e-results",
  reporter: "list",
  use: {
    baseURL: "http://127.0.0.1:3000",
    browserName: "chromium",
    trace: "retain-on-failure",
    launchOptions: chromiumExecutable ? { executablePath: chromiumExecutable } : {},
  },
});
