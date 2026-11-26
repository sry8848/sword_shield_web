import { VueQueryPlugin } from '@tanstack/vue-query'
import { createApp } from 'vue'
import { createRouter, createWebHashHistory } from 'vue-router'
import ActivityPreview from './ActivityPreview.vue'
import '../styles/index.css'
import './activityPreview.css'

if (import.meta.env.DEV && import.meta.env.MODE === 'activity-preview') {
  const router = createRouter({
    history: createWebHashHistory(),
    routes: [
      { path: '/', redirect: { name: 'events' } },
      {
        path: '/events',
        name: 'events',
        component: () => import('../features/activity/EventListPage.vue'),
        children: [
          { path: 'new', name: 'event-create', component: () => import('../features/activity/EventEditor.vue') },
          { path: ':eventId/edit', name: 'event-edit', component: () => import('../features/activity/EventEditor.vue') },
        ],
      },
    ],
  })

  // 1. 预览拥有单独的路由和查询缓存，评审过程独立于后台会话。
  createApp(ActivityPreview)
    .use(VueQueryPlugin, { queryClientConfig: { defaultOptions: { queries: { retry: false }, mutations: { retry: false } } } })
    .use(router)
    .mount('#app')
}
