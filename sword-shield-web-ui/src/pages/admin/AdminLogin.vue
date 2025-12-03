<template>
  <div class="admin-login-container">
    <el-card class="login-card">
      <div class="login-header">
        <div class="admin-icon">🔐</div>
        <h2>管理员登录</h2>
        <p class="login-subtitle">西湖论剑·后台管理系统</p>
      </div>
      <el-form :model="form" @submit.prevent="login" class="login-form">
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
            class="login-button"
          >
            登录后台
          </el-button>
        </el-form-item>
        <div class="login-footer">
          <el-button link @click="$router.push('/')" type="info">
            返回前台首页
          </el-button>
        </div>
      </el-form>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import adminApi from '../../adminApi'
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
    const { data } = await adminApi.post('/auth/login', form)
    if (data.code === 0) {
      localStorage.setItem('adminToken', data.data.token)
      localStorage.setItem('adminId', data.data.adminId)
      localStorage.setItem('adminUsername', data.data.username)
      localStorage.setItem('adminNickname', data.data.nickname)
      ElMessage.success('登录成功')
      router.push('/admin/dashboard')
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
.admin-login-container {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.login-card {
  width: 100%;
  max-width: 420px;
  border-radius: 16px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.3);
  overflow: hidden;
}

.login-header {
  text-align: center;
  margin-bottom: 32px;
}

.admin-icon {
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
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
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
