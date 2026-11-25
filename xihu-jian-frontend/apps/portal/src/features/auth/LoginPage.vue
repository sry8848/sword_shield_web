<script setup lang="ts">
import { ApiError } from '@xhlj/shared'
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from './auth'

const auth = useAuthStore()
const route = useRoute()
const router = useRouter()
const phone = ref('')
const password = ref('')
const submitting = ref(false)
const errorMessage = ref('')

/** 提交表单并返回经过路由表校验的站内目标。 */
async function submit() {
  // 1. 提交期间保持一个登录请求，并清除上一条错误。
  if (submitting.value)
    return
  submitting.value = true
  errorMessage.value = ''

  try {
    // 2. 身份确认后，只允许回到已注册的受保护页面。
    await auth.login({ phone: phone.value, password: password.value })
    const redirect = route.query.redirect
    if (typeof redirect === 'string' && redirect.startsWith('/') && !redirect.startsWith('//')
      && !redirect.includes('\\') && router.resolve(redirect).meta.requiresAuth) {
      await router.replace(redirect)
    }
    else {
      await router.replace({ name: 'account' })
    }
  }
  catch (error) {
    errorMessage.value = error instanceof ApiError ? error.message : '暂时无法登录，请稍后重试'
  }
  finally {
    password.value = ''
    submitting.value = false
  }
}
</script>

<template>
  <section class="account-card" aria-labelledby="login-heading">
    <h1 id="login-heading">
      登录大会门户
    </h1>
    <p v-if="auth.sessionMessage" role="status">
      {{ auth.sessionMessage }}
    </p>
    <form class="login-form" @submit.prevent="submit">
      <label for="phone">手机号</label>
      <input
        id="phone"
        v-model.trim="phone"
        type="tel"
        inputmode="numeric"
        autocomplete="username"
        pattern="1[3-9][0-9]{9}"
        title="请输入 11 位中国大陆手机号"
        maxlength="11"
        required
      >
      <label for="password">密码</label>
      <input id="password" v-model="password" type="password" autocomplete="current-password" required>
      <p v-if="errorMessage" class="login-error" role="alert">
        {{ errorMessage }}
      </p>
      <button type="submit" :disabled="submitting">
        {{ submitting ? '登录中…' : '登录' }}
      </button>
    </form>
  </section>
</template>
