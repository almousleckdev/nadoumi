import { defineConfig } from '@playwright/test'

const BACKEND = process.env.NUXT_BACKEND_BASE_URL ?? 'http://localhost:8080'

export default defineConfig({
  testDir: './tests/e2e',
  timeout: 30_000,
  fullyParallel: false,
  retries: process.env.CI ? 1 : 0,
  reporter: process.env.CI ? 'github' : 'list',
  use: {
    baseURL: 'http://localhost:3000',
    trace: 'on-first-retry',
  },
  webServer: {
    command: 'pnpm build && pnpm preview',
    url: 'http://localhost:3000',
    timeout: 120_000,
    reuseExistingServer: !process.env.CI,
    env: { NUXT_BACKEND_BASE_URL: BACKEND },
  },
})
