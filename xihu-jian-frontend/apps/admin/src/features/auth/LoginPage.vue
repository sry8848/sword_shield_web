<script setup lang="ts">
import { Hide, Lock, View } from '@element-plus/icons-vue'
import { ApiError, loginFieldErrors } from '@xhlj/shared'
import { ElButton, ElInput } from 'element-plus'
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { adminLoginDestination } from '../../router'
import { useAdminAuth } from './auth'

const auth = useAdminAuth()
const route = useRoute()
const router = useRouter()
const phone = ref('')
const password = ref('')
const showingPassword = ref(false)
const submitting = ref(false)
const fields = ref({ phone: '', password: '' })
const error = ref('')
const traceId = ref('')
const copyStatus = ref('')

/** 校验工作人员凭证，登录成功后交给路由守卫。 */
async function submit() {
  // 1. 校验表单并清理上次反馈。
  if (submitting.value)
    return
  const credentials = { phone: phone.value.trim(), password: password.value }
  fields.value = loginFieldErrors(credentials.phone, credentials.password)
  error.value = ''
  traceId.value = ''
  copyStatus.value = ''
  if (fields.value.phone || fields.value.password)
    return

  // 2. 建立会话，回到已验证的站内目标。
  submitting.value = true
  try {
    await auth.login(credentials)
    await router.replace(adminLoginDestination(route.query.redirect))
  }
  catch (failure) {
    if (failure instanceof ApiError && failure.code === 'ADMIN_ACCESS_REVOKED') {
      error.value = '你没有后台访问权限'
    }
    else if (failure instanceof ApiError && failure.status && failure.status < 500) {
      error.value = failure.message
    }
    else {
      error.value = '暂时无法登录，请稍后重试'
      traceId.value = failure instanceof ApiError ? failure.traceId ?? '' : ''
    }
  }
  finally {
    password.value = ''
    showingPassword.value = false
    submitting.value = false
  }
}

/** 复制完整追踪编号，并提供操作反馈。 */
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
  <section class="admin-card" aria-labelledby="login-heading">
    <div class="admin-emblem" aria-hidden="true">
      <Lock />
    </div>
    <h1 id="login-heading">
      运营后台登录
    </h1>
    <p class="admin-muted">
      请使用已获授权的大会工作人员账号。
    </p>
    <p v-if="auth.sessionMessage" class="session-notice" role="status">
      {{ auth.sessionMessage }}
    </p>

    <form class="admin-form" novalidate @submit.prevent="submit">
      <div class="admin-field">
        <label for="admin-phone">手机号</label>
        <ElInput
          id="admin-phone"
          v-model="phone"
          type="tel"
          inputmode="numeric"
          autocomplete="username"
          placeholder="请输入 11 位手机号"
          maxlength="11"
          :aria-invalid="!!fields.phone"
          :aria-describedby="fields.phone ? 'admin-phone-error' : undefined"
          required
          @input="fields.phone = ''"
        />
        <span v-if="fields.phone" id="admin-phone-error" class="field-error">{{ fields.phone }}</span>
      </div>
      <div class="admin-field">
        <label for="admin-password">密码</label>
        <ElInput
          id="admin-password"
          v-model="password"
          :type="showingPassword ? 'text' : 'password'"
          autocomplete="current-password"
          placeholder="请输入密码"
          :aria-invalid="!!fields.password"
          :aria-describedby="fields.password ? 'admin-password-error' : undefined"
          required
          @input="fields.password = ''"
        >
          <template #suffix>
            <button
              class="password-toggle"
              type="button"
              aria-label="显示密码"
              :aria-pressed="showingPassword"
              @click="showingPassword = !showingPassword"
            >
              <component :is="showingPassword ? Hide : View" aria-hidden="true" />
            </button>
          </template>
        </ElInput>
        <span v-if="fields.password" id="admin-password-error" class="field-error">{{ fields.password }}</span>
      </div>
      <div v-if="error" class="auth-error">
        <p role="alert">
          {{ error }}
        </p>
        <div v-if="traceId" class="trace">
          <code>{{ traceId }}</code>
          <button type="button" @click="copyTrace">
            复制追踪编号
          </button>
          <span v-if="copyStatus" role="status">{{ copyStatus }}</span>
        </div>
      </div>
      <ElButton class="admin-submit" type="primary" native-type="submit" :loading="submitting" :disabled="submitting">
        {{ submitting ? '登录中…' : '登录' }}
      </ElButton>
    </form>
  </section>
</template>
