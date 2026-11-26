import { createPinia } from 'pinia'
import { createApp } from 'vue'
import App from './App.vue'
import { router } from './router'
import 'element-plus/es/components/input/style/css'
import 'element-plus/es/components/button/style/css'
import 'element-plus/es/components/message-box/style/css'
import './styles/index.css'

createApp(App).use(createPinia()).use(router).mount('#app')
