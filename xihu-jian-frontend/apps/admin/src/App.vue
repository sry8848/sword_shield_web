<script setup lang="ts">
import { Lock } from '@element-plus/icons-vue'
import { ElButton, ElMessageBox } from 'element-plus'
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { adminRoleNames, useAdminAuth } from './features/auth/auth'
import { adminLoginDestination } from './router'

const auth = useAdminAuth()
const route = useRoute()
const router = useRouter()
const retrying = ref(false)
const signingOut = ref(false)
const logoutError = ref('')
const copyStatus = ref('')
const roleNames = computed(() => auth.profile?.roles.map(role => adminRoleNames[role]).join('、'))

/** 重试本次导航的身份检查，并重新进入路由权限判断。 */
async function retry() {
  retrying.value = true
  copyStatus.value = ''
  try {
    await auth.checkIdentity()
    if (auth.sessionError)
      return
    if (route.name === 'login' && auth.profile)
      await router.replace(adminLoginDestination(route.query.redirect))
    else if (route.meta.requiresAuth && !auth.profile)
      await router.replace({ name: 'login', query: { redirect: route.fullPath } })
    else if (route.meta.roles && !route.meta.roles.some(role => auth.profile?.roles.includes(role)))
      await router.replace({ name: 'forbidden' })
  }
  finally {
    retrying.value = false
  }
}

/** 确认退出成功后打开后台登录页。 */
async function logout() {
  const confirmed = await ElMessageBox.confirm('确定退出运营后台？', '退出登录', {
    confirmButtonText: '确认退出',
    cancelButtonText: '取消',
  }).then(() => true, () => false)
  if (!confirmed)
    return

  // 1. 保留当前视图，直至服务端确认退出。
  signingOut.value = true
  logoutError.value = ''
  try {
    await auth.logout()
    await router.replace({ name: 'login' })
  }
  catch {
    logoutError.value = '暂时无法退出，请稍后重试'
  }
  finally {
    signingOut.value = false
  }
}

/** 复制会话恢复失败的追踪编号。 */
async function copyTrace(traceId: string) {
  try {
    await navigator.clipboard.writeText(traceId)
    copyStatus.value = '已复制'
  }
  catch {
    copyStatus.value = '复制失败，请选中编号复制'
  }
}
</script>

<template>
  <header class="admin-header">
    <div class="admin-toolbar">
      <div class="admin-brand">
        <span class="brand-mark" aria-hidden="true"><Lock /></span>
        <strong>西湖论剑</strong>
        <span class="admin-brand-caption">大会运营管理</span>
      </div>
      <details v-if="auth.profile && route.meta.requiresAuth && !auth.sessionError" class="admin-account">
        <summary>账号</summary>
        <div class="admin-menu">
          <p>{{ roleNames }}</p>
          <ElButton :disabled="signingOut" @click="logout">
            {{ signingOut ? '退出中…' : '退出登录' }}
          </ElButton>
        </div>
      </details>
    </div>
    <p v-if="logoutError" class="toolbar-error" role="alert">
      {{ logoutError }}
    </p>
  </header>
  <main class="admin-main">
    <p v-if="!route.name" role="status">
      正在确认登录状态…
    </p>
    <section v-else-if="auth.sessionError" class="admin-card">
      <h1>暂时无法确认后台登录状态</h1>
      <p role="alert">
        请稍后重试。
      </p>
      <div v-if="auth.sessionError.traceId" class="trace">
        <code>{{ auth.sessionError.traceId }}</code>
        <button type="button" @click="copyTrace(auth.sessionError.traceId)">
          复制追踪编号
        </button>
        <span v-if="copyStatus" role="status">{{ copyStatus }}</span>
      </div>
      <ElButton type="primary" :loading="retrying" @click="retry">
        重试
      </ElButton>
    </section>
    <RouterView v-else />
  </main>
</template>
