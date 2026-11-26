<script setup lang="ts">
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from './auth'

const auth = useAuthStore()
const route = useRoute()
const router = useRouter()
const retrying = ref(false)

/** 重试读取账号，确认没有有效会话时返回登录。 */
async function retry() {
  const hadIdentity = !!auth.user
  retrying.value = true
  try {
    await auth.retryAccount()
    // 已登录时由根布局处理会话结束，重试不能覆盖主动退出的去向。
    if (!hadIdentity && route.name === 'account' && !auth.user && !auth.accountError)
      await router.replace({ name: 'login', query: { redirect: route.fullPath } })
  }
  finally {
    retrying.value = false
  }
}
</script>

<template>
  <section class="account-card" aria-labelledby="account-heading">
    <h1 id="account-heading">
      个人概览
    </h1>
    <dl v-if="auth.user" class="identity-summary">
      <dt>账号 ID</dt>
      <dd>{{ auth.user.id }}</dd>
      <dt>手机号</dt>
      <dd>{{ auth.user.maskedPhone }}</dd>
    </dl>
    <p v-if="auth.accountError" class="login-error" role="alert">
      {{ auth.accountError }}
    </p>
    <p v-if="retrying" role="status">
      正在读取账号…
    </p>
    <button v-if="auth.accountError" class="primary-button" type="button" :disabled="retrying" @click="retry">
      重试
    </button>
  </section>
</template>
