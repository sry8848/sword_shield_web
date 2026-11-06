<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '../stores/auth'
const auth = useAuthStore(); const route = useRoute(); const router = useRouter()
const mockMode = import.meta.env.VITE_USE_MOCK === 'true'
const loading = ref(false); const form = reactive({ account: mockMode ? 'user' : '', password: mockMode ? 'user123' : '' })
async function submit() {
  if (loading.value) return
  loading.value = true
  try {
    await auth.login(form.account, form.password); ElMessage.success('登录成功')
    const redirect = typeof route.query.redirect === 'string' && /^\/(?![\/\\])/.test(route.query.redirect) ? route.query.redirect : '/'
    router.push(redirect)
  }
  catch (error) { ElMessage.error(error instanceof Error ? error.message : '登录失败') }
  finally { loading.value = false }
}
</script>
<template>
  <section class="page-container login-wrap"><div class="login-card surface-card"><div class="login-aside"><span>WEST LAKE SWORD CONFERENCE</span><h1>连接观点<br />探索未来</h1><p>登录后管理你的大会报名与参会信息。</p></div><div class="login-form"><h2>欢迎回来</h2><p>使用大会账号登录</p>
    <el-form label-position="top" @submit.prevent="submit"><el-form-item label="用户名"><el-input v-model="form.account" size="large" autocomplete="username" /></el-form-item><el-form-item label="密码"><el-input v-model="form.password" type="password" size="large" show-password autocomplete="current-password" @keyup.enter="submit" /></el-form-item><el-button type="primary" size="large" :loading="loading" class="submit" @click="submit">登录</el-button></el-form>
    <div v-if="mockMode" class="demo-tip">本地 Mock 演示：user / user123</div></div></div></section>
</template>
<style scoped>
.login-wrap { min-height: 68vh; display: grid; place-items: center; padding-top: 45px; padding-bottom: 45px; }
.login-card { display: grid; grid-template-columns: 1fr 1fr; width: min(850px,100%); overflow: hidden; }
.login-aside { padding: 55px 42px; color: white; background: linear-gradient(145deg,#0b3472,#287ed0); }
.login-aside span { font-size: 10px; letter-spacing: 2px; color: #a8d2ff; } .login-aside h1 { font-size: 42px; line-height: 1.3; margin: 50px 0 20px; } .login-aside p { color: #d1e5ff; }
.login-form { padding: 48px 42px; } .login-form h2 { margin: 0 0 8px; font-size: 25px; } .login-form > p { color: #8290a5; font-size: 13px; margin-bottom: 28px; }
.submit { width: 100%; } .demo-tip { color: #8b98aa; font-size: 11px; margin-top: 18px; line-height: 1.6; }
@media (max-width:650px) { .login-card { grid-template-columns: 1fr; } .login-aside { padding: 28px; } .login-aside h1 { margin: 22px 0 10px; font-size: 30px; } .login-form { padding: 28px; } }
</style>
