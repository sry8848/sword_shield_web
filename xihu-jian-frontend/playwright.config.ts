import process from 'node:process'
import { defineConfig, devices } from '@playwright/test'

export default defineConfig({
  testDir: './tests/e2e',
  fullyParallel: true,
  reporter: 'list',
  use: { baseURL: 'http://127.0.0.1:5173', trace: 'retain-on-failure' },
  projects: [{ name: 'chromium', use: { ...devices['Desktop Chrome'] } }],
  webServer: [
    { command: 'pnpm dev:portal', url: 'http://127.0.0.1:5173', env: { VITE_USE_MOCK: 'true' }, reuseExistingServer: !process.env.CI, timeout: 120_000 },
    { command: 'pnpm dev:admin', url: 'http://127.0.0.1:5174', env: { VITE_USE_MOCK: 'true' }, reuseExistingServer: !process.env.CI, timeout: 120_000 },
  ],
})
