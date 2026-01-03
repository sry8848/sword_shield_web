/**
 * Vite 配置（开发/构建）。
 *
 * - 本地开发端口：5173
 * - 代理 `/api` → `http://localhost:8080`（对接后端网关/聚合服务）
 */
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        secure: false,
      },
    },
  },
})
