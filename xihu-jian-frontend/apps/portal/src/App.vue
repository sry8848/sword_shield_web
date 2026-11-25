<script setup lang="ts">
import { watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from './features/auth/auth'

const auth = useAuthStore()
const route = useRoute()
const router = useRouter()

// 会话在已打开的私有页面上失效或退出时，守卫不会再次运行。
watch(() => auth.user, (user, previous) => {
  if (!user && previous && route.meta.requiresAuth)
    void router.replace({ name: 'login', query: { redirect: route.fullPath } })
})
</script>

<template>
  <header class="portal-header">
    <nav class="portal-nav" aria-label="主导航">
      <RouterLink to="/" class="portal-brand">
        西湖论剑
      </RouterLink>
      <RouterLink :to="{ name: 'account' }">
        我的账号
      </RouterLink>
    </nav>
  </header>
  <main class="portal-main">
    <RouterView />
  </main>
</template>
