import vue from '@vitejs/plugin-vue'
import { defineConfig, loadEnv } from 'vite'
import { activityMock } from './src/preview/activityMock'

export default defineConfig(({ mode }) => {
  if (mode === 'activity-preview') {
    return {
      plugins: [vue(), activityMock()],
      define: { 'import.meta.env.VITE_API_BASE_URL': JSON.stringify('') },
      server: { host: '127.0.0.1', port: 5184, strictPort: true },
    }
  }
  const env = loadEnv(mode, '.', '')
  return { plugins: [vue()], server: { port: 5174, proxy: { '/api': { target: env.VITE_PROXY_TARGET || 'http://localhost:8080', changeOrigin: true } } } }
})
