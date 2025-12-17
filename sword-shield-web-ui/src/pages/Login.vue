<template>
  <div class="auth-page">
    <div class="auth-card">
      <div class="auth-header">
        <div class="auth-tag">前台登录</div>
        <h1 class="auth-title">登录 Sword Shield 账号</h1>
        <p class="auth-subtitle">用于报名活动、下载资料和查看个人信息。</p>
      </div>
      <el-form :model="form" @submit.prevent="login" class="auth-form">
        <el-form-item>
          <el-input
            v-model="form.username"
            placeholder="请输入用户名"
            size="large"
            prefix-icon="User"
          />
        </el-form-item>
        <el-form-item>
          <el-input
            v-model="form.password"
            type="password"
            placeholder="请输入密码"
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
            class="auth-button"
          >
            登录
          </el-button>
        </el-form-item>
        <div class="auth-footer">
          <span>还没有账号？</span>
          <el-button link @click="$router.push('/register')" type="primary">
            去注册
          </el-button>
        </div>
      </el-form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import api from '../api'
import { ElMessage } from 'element-plus'

const router = useRouter()
const form = reactive({ username: '', password: '' })
const loading = ref(false)

async function login() {
  if (!form.username || !form.password) {
    ElMessage.warning('请输入用户名和密码')
    return
  }
  
  loading.value = true
  try {
    const { data } = await api.post('/user/login', form)
    if (data.code === 0) {
      localStorage.setItem('token', data.data.token)
      localStorage.setItem('userId', data.data.userId)
      localStorage.setItem('username', form.username)
      
      // Fetch user profile to get avatar and other info
      try {
        const profileRes = await api.get('/user/profile', {
          params: { id: data.data.userId }
        })
        if (profileRes.data.code === 0 && profileRes.data.data) {
          const user = profileRes.data.data
          if (user.avatarUrl) {
            localStorage.setItem('userAvatar', user.avatarUrl)
          }
          if (user.nickname) {
            localStorage.setItem('username', user.nickname)
          }
        }
      } catch (error) {
        console.warn('Failed to fetch user profile:', error)
      }
      
      // Dispatch custom event to notify App.vue
      window.dispatchEvent(new Event('user-login'))
      
      ElMessage.success('登录成功')
      router.push('/')
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
.auth-page {
  min-height: calc(100vh - 200px);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 32px 16px;
}

.auth-card {
  width: 100%;
  max-width: 480px;
  padding: 32px 32px 28px;
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.96);
  box-shadow: 0 18px 45px rgba(15, 23, 42, 0.16);
  border: 1px solid rgba(148, 163, 184, 0.25);
}

.auth-header {
  margin-bottom: 28px;
}

.auth-tag {
  display: inline-flex;
  align-items: center;
  padding: 2px 10px;
  border-radius: 999px;
  font-size: 11px;
  letter-spacing: 0.12em;
  text-transform: uppercase;
  color: #1d4ed8;
  background: rgba(219, 234, 254, 0.9);
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

.auth-button {
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
