import { defineConfig, devices } from '@playwright/test'

export default defineConfig({
  testDir: './e2e',
  fullyParallel: true,
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 2 : 0,
  workers: process.env.CI ? 1 : undefined,
  reporter: 'html',
  timeout: 45000,
  expect: {
    timeout: 10000,
  },
  use: {
    baseURL: 'http://localhost:4173',
    trace: 'on-first-retry',
    actionTimeout: 15000,
    navigationTimeout: 30000,
  },

  projects: [
    {
      name: 'Mobile Chrome',
      testIgnore: /escritorio\.spec\.ts/,
      use: {
        ...devices['Pixel 5'],
        viewport: { width: 390, height: 844 },
      },
    },
    {
      name: 'Desktop Chrome',
      testMatch: /escritorio\.spec\.ts/,
      use: { ...devices['Desktop Chrome'], viewport: { width: 1440, height: 900 } },
    },
  ],

  webServer: {
    // Build en modo e2e: usa .env.e2e (Supabase falso, sin clave de Maps).
    command: 'pnpm exec vite build --mode e2e && pnpm preview --port 4173 --strictPort',
    timeout: 240000,
    url: 'http://localhost:4173',
    reuseExistingServer: false,
  },
})
