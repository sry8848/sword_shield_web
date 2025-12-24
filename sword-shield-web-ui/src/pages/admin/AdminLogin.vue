<template>
  <div class="auth-page admin-auth-page">
    <div class="auth-card admin-auth-card">
      <router-link class="auth-switch" to="/login">前台登录</router-link>
      <div class="auth-header">
        <div class="auth-tag admin-tag">后台登录</div>
        <h1 class="auth-title">登录 西湖论剑 管理后台</h1>
        <p class="auth-subtitle">仅限大会工作人员使用，用于管理活动、媒体与数据。</p>
      </div>
      <el-form :model="form" @submit.prevent="login" class="auth-form">
        <el-form-item>
          <el-input 
            v-model="form.username" 
            placeholder="管理员用户名"
            size="large"
            prefix-icon="User"
          />
        </el-form-item>
        <el-form-item>
          <el-input 
            v-model="form.password" 
            type="password" 
            placeholder="管理员密码"
            size="large"
            prefix-icon="Lock"
            show-password
          />
        </el-form-item>
        <el-form-item>
          <el-button 
            type="primary" 
            @click="login" 
            :loading="loading"
            size="large"
            class="auth-button admin-auth-button"
          >
            登录后台
          </el-button>
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import adminApi from '../../adminApi'
import { ElMessage } from 'element-plus'

const router = useRouter()
const route = useRoute()
const form = reactive({ username: '', password: '' })
const loading = ref(false)

async function login() {
  if (!form.username || !form.password) {
    ElMessage.warning('请输入用户名和密码')
    return
  }
  
  loading.value = true
  try {
    const { data } = await adminApi.post('/admin/auth/login', form)
    if (data.code === 0) {
      localStorage.setItem('adminToken', data.data.token)
      localStorage.setItem('adminId', data.data.adminId)
      localStorage.setItem('adminUsername', data.data.username)
      localStorage.setItem('adminNickname', data.data.nickname)
      ElMessage.success('登录成功')
      const redirect = (route.query.redirect as string) || '/admin/dashboard'
      router.push(redirect)
    } else {
      ElMessage.error(data.message || '登录失败')
    }
  } catch (error) {
    ElMessage.error('登录失败，请稍后重试')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.auth-page.admin-auth-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 32px 16px;
  background: radial-gradient(circle at top, #e3eeff 0, #f3f6fb 45%, #eef2f7 100%);
}

.auth-card.admin-auth-card {
  width: 100%;
  max-width: 480px;
  padding: 32px 32px 28px;
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.96);
  box-shadow: 0 18px 45px rgba(15, 23, 42, 0.16);
  border: 1px solid rgba(148, 163, 184, 0.25);
  position: relative;
}

.auth-switch {
  position: absolute;
  top: 18px;
  right: 18px;
  z-index: 1;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 6px 14px 12px;
  border-radius: 10px 10px 6px 6px;
  font-size: 11px;
  letter-spacing: 0.08em;
  text-decoration: none;
  font-weight: 600;
  color: #1d4ed8;
  background: rgba(219, 234, 254, 0.95);
  box-shadow: 0 8px 18px rgba(30, 64, 175, 0.2);
  transition: transform 0.2s ease, box-shadow 0.2s ease;
  cursor: pointer;
}

.auth-switch::after {
  content: '';
  position: absolute;
  left: 0;
  right: 0;
  bottom: -10px;
  margin: 0 auto;
  width: 0;
  height: 0;
  border-left: 18px solid transparent;
  border-right: 18px solid transparent;
  border-top: 10px solid rgba(219, 234, 254, 0.95);
}

.auth-switch:hover {
  transform: translateY(-2px);
  box-shadow: 0 10px 22px rgba(30, 64, 175, 0.26);
}

.auth-switch:active {
  transform: translateY(0);
}

.auth-header {
  margin-bottom: 28px;
}

.admin-tag {
  display: inline-flex;
  align-items: center;
  padding: 2px 10px;
  border-radius: 999px;
  font-size: 11px;
  letter-spacing: 0.12em;
  text-transform: uppercase;
  color: #1d4ed8;
  background: rgba(219, 234, 254, 0.95);
}

.auth-title {
  margin-top: 14px;
  font-size: 22px;
  font-weight: 600;
  color: #111827;
}

.auth-subtitle {
  margin-top: 4px;
  font-size: 13px;
  color: #6b7280;
}

.auth-form {
  margin-top: 8px;
}

.admin-auth-button {
  width: 100%;
  background: linear-gradient(135deg, #2563eb 0%, #1d4ed8 40%, #0f172a 100%);
  border: none;
  font-size: 15px;
  font-weight: 600;
}

.auth-footer {
  text-align: center;
  margin-top: 8px;
  color: #6b7280;
  font-size: 13px;
}
</style>
