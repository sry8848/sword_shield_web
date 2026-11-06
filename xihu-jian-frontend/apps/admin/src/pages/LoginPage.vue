<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '../stores/auth'
const auth = useAuthStore(); const route = useRoute(); const router = useRouter()
const mockMode = import.meta.env.VITE_USE_MOCK === 'true'
const loading = ref(false); const form = reactive({ account: mockMode ? 'admin' : '', password: mockMode ? 'admin123' : '' })
async function submit() {
  if (loading.value) return
  loading.value = true
  try {
    await auth.login(form.account, form.password)
    ElMessage.success('登录成功')
    const redirect = typeof route.query.redirect === 'string' && /^\/(?![\/\\])/.test(route.query.redirect) ? route.query.redirect : '/'
    router.push(redirect)
  }
  catch (error) { ElMessage.error(error instanceof Error ? error.message : '登录失败') }
  finally { loading.value = false }
}
</script>
<template><div class="login-page"><div class="login-card"><div class="brand-mark">西</div><h1>大会运营管理平台</h1><p>登录后管理活动、报名与现场执行</p>
  <el-form label-position="top" @submit.prevent="submit"><el-form-item label="管理员账号"><el-input v-model="form.account" size="large" /></el-form-item><el-form-item label="密码"><el-input v-model="form.password" type="password" size="large" show-password @keyup.enter="submit" /></el-form-item><el-button type="primary" size="large" class="submit" :loading="loading" @click="submit">登录管理后台</el-button></el-form>
  <div v-if="mockMode" class="tip">Mock 演示账号：admin / admin123</div></div></div></template>
<style scoped>
.login-page { min-height: 100vh; display: grid; place-items: center; padding: 24px; background: linear-gradient(135deg,#0d2b59,#1e5eae); }
.login-card { width: min(420px,100%); background: white; border-radius: 14px; padding: 38px; box-shadow: 0 24px 70px rgba(0,0,0,.18); }
.brand-mark { width: 46px; height: 46px; display: grid; place-items: center; border-radius: 12px; background: #155eef; color: white; font-size: 24px; font-weight: 800; }
h1 { font-size: 22px; margin: 22px 0 8px; } p,.tip { color: #8491a3; font-size: 12px; } .submit { width: 100%; margin-top: 8px; } .tip { margin-top: 18px; }
</style>
