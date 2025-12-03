<template>
  <div class="login-container">
    <el-card class="login-card">
      <div class="login-header">
        <div class="login-icon">🔒</div>
        <h2>欢迎回来</h2>
        <p class="login-subtitle">登录你的账号</p>
      </div>
      <el-form :model="form" @submit.prevent="login" class="login-form">
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
            class="login-button"
          >
            登录
          </el-button>
        </el-form-item>
        <div class="login-footer">
          <span>还没有账号？</span>
          <el-button link @click="$router.push('/register')" type="primary">
            立即注册
          </el-button>
        </div>
      </el-form>
    </el-card>
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
.login-container {
  min-height: calc(100vh - 200px);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
}

.login-card {
  width: 100%;
  max-width: 420px;
  border-radius: 16px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.12);
  overflow: hidden;
}

.login-header {
  text-align: center;
  margin-bottom: 32px;
}

.login-icon {
  font-size: 64px;
  margin-bottom: 16px;
}

.login-header h2 {
  font-size: 28px;
  color: #333;
  margin: 0 0 8px 0;
}

.login-subtitle {
  color: #666;
  font-size: 14px;
  margin: 0;
}

.login-form {
  margin-top: 24px;
}

.login-button {
  width: 100%;
  background: linear-gradient(135deg, #ff0050 0%, #00f2ea 100%);
  border: none;
  font-size: 16px;
  font-weight: 600;
}

.login-footer {
  text-align: center;
  margin-top: 16px;
  color: #666;
  font-size: 14px;
}
</style>
