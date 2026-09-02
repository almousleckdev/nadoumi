import { fileURLToPath, URL } from 'node:url'
import { defineConfig } from 'vitest/config'
import vue from '@vitejs/plugin-vue'

// Standalone from vite.config.ts: tests need the Vue SFC compiler and the `@`
// alias, but not the dev server / proxy. Element Plus is registered as a real
// plugin in tests (tests/helpers.ts) so `el-*` components resolve normally.
export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: { '@': fileURLToPath(new URL('./src', import.meta.url)) },
  },
  test: {
    environment: 'happy-dom',
    include: ['tests/**/*.test.ts'],
    globals: false,
    setupFiles: ['tests/setup.ts'],
  },
})
