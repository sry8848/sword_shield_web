<template>
  <div class="register-container">
    <el-card class="register-card">
      <div class="register-header">
        <div class="register-icon">✨</div>
        <h2>创建账号</h2>
        <p class="register-subtitle">加入 Sword Shield 社区</p>
      </div>
      <el-form :model="form" @submit.prevent="register" class="register-form">
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
          <el-input 
            v-model="form.confirmPassword" 
            type="password" 
            placeholder="请确认密码"
            size="large"
            prefix-icon="Lock"
            show-password
          />
        </el-form-item>
        <el-form-item>
          <el-button 
            type="primary" 
            @click="register" 
            :loading="loading"
            size="large"
            class="register-button"
          >
            注册
          </el-button>
        </el-form-item>
        <div class="register-footer">
          <span>已有账号？</span>
          <el-button link @click="$router.push('/login')" type="primary">
            立即登录
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
const form = reactive({ username: '', password: '', confirmPassword: '' })
const loading = ref(false)

async function register() {
  if (!form.username || !form.password || !form.confirmPassword) {
    ElMessage.warning('请填写所有字段')
    return
  }
  
  if (form.password !== form.confirmPassword) {
    ElMessage.warning('两次密码输入不一致')
    return
  }
  
  loading.value = true
  try {
    const { data } = await api.post('/user/register', form)
    if (data.code === 0) {
      ElMessage.success('注册成功，请登录')
      router.push('/login')
    } else {
      ElMessage.error(data.message || '注册失败')
    }
  } catch (error) {
    ElMessage.error('注册失败，请稍后重试')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.register-container {
  min-height: calc(100vh - 200px);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
}

.register-card {
  width: 100%;
  max-width: 420px;
  border-radius: 16px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.12);
  overflow: hidden;
}

.register-header {
  text-align: center;
  margin-bottom: 32px;
}

.register-icon {
  font-size: 64px;
  margin-bottom: 16px;
}

.register-header h2 {
  font-size: 28px;
  color: #333;
  margin: 0 0 8px 0;
}

.register-subtitle {
  color: #666;
  font-size: 14px;
  margin: 0;
}

.register-form {
  margin-top: 24px;
}

.register-button {
  width: 100%;
  background: linear-gradient(135deg, #ff0050 0%, #00f2ea 100%);
  border: none;
  font-size: 16px;
  font-weight: 600;
}

.register-footer {
  text-align: center;
  margin-top: 16px;
  color: #666;
  font-size: 14px;
}
</style>

