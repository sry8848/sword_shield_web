<script setup lang="ts">
import { ApiError } from '@xhlj/shared'
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from './auth'

const auth = useAuthStore()
const route = useRoute()
const router = useRouter()
const retrying = ref(false)
const signingOut = ref(false)
const logoutError = ref('')

/** 重试读取账号，确认没有有效会话时返回登录。 */
async function retry() {
  retrying.value = true
  try {
    await auth.retryAccount()
    if (route.name === 'account' && !auth.user && !auth.accountError)
      await router.replace({ name: 'login', query: { redirect: route.fullPath } })
  }
  finally {
    retrying.value = false
  }
}

/** 退出失败时保留当前页面，允许用户重新提交退出。 */
async function logout() {
  signingOut.value = true
  logoutError.value = ''
  try {
    await auth.logout()
  }
  catch (error) {
    logoutError.value = error instanceof ApiError ? error.message : '暂时无法退出，请稍后重试'
  }
  finally {
    signingOut.value = false
  }
}
</script>

<template>
  <section class="account-card" aria-labelledby="account-heading">
    <h1 id="account-heading">
      我的账号
    </h1>
    <dl v-if="auth.user" class="identity-summary">
      <dt>账号 ID</dt>
      <dd>{{ auth.user.id }}</dd>
      <dt>手机号</dt>
      <dd>{{ auth.user.maskedPhone }}</dd>
    </dl>
    <p v-if="auth.accountError || logoutError" class="login-error" role="alert">
      {{ logoutError || auth.accountError }}
    </p>
    <p v-if="retrying" role="status">
      正在读取账号…
    </p>
    <div class="account-actions">
      <button v-if="auth.accountError" type="button" :disabled="retrying || signingOut" @click="retry">
        重试
      </button>
      <button v-if="auth.user" type="button" :disabled="signingOut" @click="logout">
        {{ signingOut ? '退出中…' : '退出登录' }}
      </button>
    </div>
  </section>
</template>
