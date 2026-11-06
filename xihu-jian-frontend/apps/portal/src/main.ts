import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import App from './App.vue'
import { router } from './router'
import { http } from './api/client'
import './styles/index.css'

async function bootstrap() {
  if (import.meta.env.VITE_USE_MOCK === 'true') {
    const { installMockApi } = await import('@xhlj/shared/mocks')
    installMockApi(http)
  }
  const app = createApp(App)
  app.use(createPinia())
  app.use(router)
  app.use(ElementPlus)
  await router.isReady()
  app.mount('#app')
}

bootstrap()
