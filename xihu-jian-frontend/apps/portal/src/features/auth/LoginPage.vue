<script setup lang="ts">
import { Hide, User, View } from '@element-plus/icons-vue'
import { ApiError, loginFieldErrors } from '@xhlj/shared'
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { portalLoginDestination } from '../../router'
import { useAuthStore } from './auth'

const auth = useAuthStore()
const route = useRoute()
const router = useRouter()
const phone = ref('')
const password = ref('')
const showingPassword = ref(false)
const submitting = ref(false)
const fields = ref({ phone: '', password: '' })
const errorMessage = ref('')
const traceId = ref('')
const copyStatus = ref('')

/** 校验凭证并在登录成功后返回已注册的站内页面。 */
async function submit() {
  // 1. 校验表单并清理上次反馈。
  if (submitting.value)
    return
  fields.value = loginFieldErrors(phone.value, password.value)
  errorMessage.value = ''
  traceId.value = ''
  copyStatus.value = ''
  if (fields.value.phone || fields.value.password)
    return

  // 2. 建立会话，替换登录页历史记录。
  submitting.value = true
  try {
    await auth.login({ phone: phone.value, password: password.value })
    await router.replace(portalLoginDestination(route.query.redirect))
  }
  catch (error) {
    if (error instanceof ApiError && error.status && error.status < 500) {
      errorMessage.value = error.message
    }
    else {
      errorMessage.value = '暂时无法登录，请稍后重试'
      traceId.value = error instanceof ApiError ? error.traceId ?? '' : ''
    }
  }
  finally {
    password.value = ''
    showingPassword.value = false
    submitting.value = false
  }
}

/** 复制完整追踪编号，并在表单中显示操作结果。 */
async function copyTrace() {
  try {
    await navigator.clipboard.writeText(traceId.value)
    copyStatus.value = '已复制'
  }
  catch {
    copyStatus.value = '复制失败，请选中编号复制'
  }
}
</script>

<template>
  <div class="login-stage">
    <section class="account-card login-card" aria-labelledby="login-heading">
      <div class="login-emblem" aria-hidden="true">
        <User />
      </div>
      <h1 id="login-heading">
        登录大会门户
      </h1>
      <p class="login-subtitle">
        登录后查看报名记录和入场凭证。
      </p>
      <p v-if="auth.sessionMessage" class="session-notice" role="status">
        {{ auth.sessionMessage }}
      </p>
      <form class="login-form" novalidate @submit.prevent="submit">
        <div class="login-field">
          <label for="phone">手机号</label>
          <input
            id="phone"
            v-model.trim="phone"
            type="tel"
            inputmode="numeric"
            autocomplete="username"
            placeholder="请输入 11 位手机号"
            maxlength="11"
            :aria-invalid="!!fields.phone"
            :aria-describedby="fields.phone ? 'phone-error' : undefined"
            required
            @input="fields.phone = ''"
          >
          <span v-if="fields.phone" id="phone-error" class="field-error">{{ fields.phone }}</span>
        </div>
        <div class="login-field">
          <label for="password">密码</label>
          <div class="password-field">
            <input
              id="password"
              v-model="password"
              :type="showingPassword ? 'text' : 'password'"
              autocomplete="current-password"
              placeholder="请输入密码"
              :aria-invalid="!!fields.password"
              :aria-describedby="fields.password ? 'password-error' : undefined"
              required
              @input="fields.password = ''"
            >
            <button
              class="password-toggle"
              type="button"
              aria-label="显示密码"
              :aria-pressed="showingPassword"
              @click="showingPassword = !showingPassword"
            >
              <component :is="showingPassword ? Hide : View" aria-hidden="true" />
            </button>
          </div>
          <span v-if="fields.password" id="password-error" class="field-error">{{ fields.password }}</span>
        </div>
        <div v-if="errorMessage" class="login-feedback">
          <p role="alert">
            {{ errorMessage }}
          </p>
          <div v-if="traceId" class="trace">
            <code>{{ traceId }}</code>
            <button type="button" @click="copyTrace">
              复制追踪编号
            </button>
            <span v-if="copyStatus" role="status">{{ copyStatus }}</span>
          </div>
        </div>
        <button class="primary-button login-submit" type="submit" :disabled="submitting">
          {{ submitting ? '登录中…' : '登录' }}
        </button>
      </form>
      <div class="login-return">
        <RouterLink :to="{ name: 'home' }">
          <span aria-hidden="true">←</span> 返回大会首页
        </RouterLink>
      </div>
    </section>
  </div>
</template>
