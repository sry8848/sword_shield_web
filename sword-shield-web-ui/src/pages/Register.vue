<template>
  <div class="auth-page">
    <div class="auth-card">
      <div class="auth-header">
        <div class="auth-tag">注册账号</div>
        <h1 class="auth-title">加入 西湖论剑 平台</h1>
        <p class="auth-subtitle">创建账号以报名活动、下载大会资料并参与互动。</p>
      </div>
      <el-form :model="form" @submit.prevent="register" class="auth-form">
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
            placeholder="请再次输入密码"
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
            class="auth-button"
          >
            注册
          </el-button>
        </el-form-item>
        <div class="auth-footer">
          <span>已经有账号？</span>
          <el-button link @click="$router.push('/login')" type="primary">
            去登录
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
.auth-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 32px 16px;
  background: radial-gradient(circle at top, #e3eeff 0, #f3f6fb 45%, #eef2f7 100%);
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
  color: #0f766e;
  background: rgba(209, 250, 229, 0.9);
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
  background: linear-gradient(135deg, #22c55e 0%, #16a34a 40%, #0f172a 100%);
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
