import { fileURLToPath, URL } from 'node:url'
import { defineConfig } from 'vitest/config'
import vue from '@vitejs/plugin-vue'

// Standalone from vite.config.ts: tests need the Vue SFC compiler and the `@`
// alias, but not the dev server / proxy or the Element Plus auto-resolver —
// `el-*` tags fall back to slot-rendering, which is all these unit tests inspect.
export default defineConfig({
  plugins: [
    vue({ template: { compilerOptions: { isCustomElement: (tag) => tag.startsWith('el-') } } }),
  ],
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
