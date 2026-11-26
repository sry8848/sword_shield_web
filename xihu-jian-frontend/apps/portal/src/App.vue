<script setup lang="ts">
import { Lock, User } from '@element-plus/icons-vue'
import { ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from './features/auth/auth'

const auth = useAuthStore()
const route = useRoute()
const router = useRouter()
const accountMenu = ref<HTMLDetailsElement>()
const signingOut = ref(false)
const logoutError = ref('')

// 会话在当前私有页失效时没有新的导航，守卫不会自动执行。
watch(() => auth.user, (user, previous) => {
  if (!user && previous && auth.sessionMessage && route.meta.requiresAuth)
    void router.replace({ name: 'login', query: { redirect: route.fullPath } })
})

/** 关闭全站账号菜单。 */
function closeAccountMenu() {
  if (accountMenu.value)
    accountMenu.value.open = false
}

/** 退出成功后保留公开页，个人空间回到大会首页。 */
async function logout() {
  if (signingOut.value)
    return
  signingOut.value = true
  logoutError.value = ''

  // 1. 等待接口确认退出，失败后保留当前身份。
  try {
    await auth.logout()
    if (route.meta.requiresAuth)
      await router.replace({ name: 'home' })
  }
  catch {
    logoutError.value = '暂时无法退出，请稍后重试'
  }
  finally {
    signingOut.value = false
  }
}
</script>

<template>
  <header class="portal-header">
    <nav class="portal-nav" aria-label="主导航">
      <RouterLink :to="{ name: 'home' }" class="portal-brand" aria-label="西湖论剑">
        <span class="brand-mark" aria-hidden="true"><Lock /></span>
        <strong>西湖论剑</strong>
        <span class="brand-caption" aria-hidden="true">数字安全大会</span>
      </RouterLink>
      <div class="portal-nav-links">
        <RouterLink :to="{ name: 'home' }" class="portal-home">
          大会首页
        </RouterLink>
        <details v-if="auth.user" ref="accountMenu" class="portal-account" @keydown.esc="closeAccountMenu">
          <summary><User aria-hidden="true" /><span>我的</span></summary>
          <div class="account-menu">
            <RouterLink :to="{ name: 'account' }" @click="closeAccountMenu">
              个人概览
            </RouterLink>
            <button type="button" :disabled="signingOut" @click="logout">
              {{ signingOut ? '退出中…' : '退出登录' }}
            </button>
          </div>
        </details>
        <RouterLink
          v-else-if="auth.ready"
          :to="route.name === 'login' ? route.fullPath : { name: 'login', query: { redirect: route.fullPath } }"
          class="portal-login"
        >
          登录
        </RouterLink>
      </div>
    </nav>
    <p v-if="logoutError" class="portal-nav-error" role="alert">
      {{ logoutError }}
    </p>
  </header>
  <main :class="route.name === 'login' ? 'portal-login-main' : 'portal-main'">
    <RouterView />
  </main>
</template>
