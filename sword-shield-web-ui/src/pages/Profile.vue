<template>
  <div class="profile-container">
    <el-card class="profile-card">
      <div class="profile-header">
        <div class="avatar-section">
          <el-avatar :size="100" :src="form.avatarUrl || undefined" class="large-avatar">
            {{ form.nickname?.charAt(0) || 'U' }}
          </el-avatar>
          <label class="upload-label">
            <input type="file" @change="onFile" accept="image/*" hidden ref="fileInput" />
            <el-button type="primary" size="small" :loading="uploading" @click="triggerUpload" :icon="Camera">
              {{ uploading ? '上传中...' : '更换头像' }}
            </el-button>
          </label>
        </div>
        <h2 class="profile-title">
          <el-icon class="title-icon"><Avatar /></el-icon>
          个人信息
        </h2>
      </div>

      <el-divider />

      <el-form :model="form" label-width="100px" class="profile-form">
        <el-form-item label="用户名">
          <el-input 
            v-model="form.username" 
            disabled
            size="large"
          />
        </el-form-item>
        
        <el-form-item label="昵称">
          <el-input 
            v-model="form.nickname" 
            placeholder="请输入昵称"
            size="large"
          />
        </el-form-item>
        
        <el-form-item label="性别">
          <el-radio-group v-model="form.gender" size="large">
            <el-radio label="男">男</el-radio>
            <el-radio label="女">女</el-radio>
            <el-radio label="保密">保密</el-radio>
          </el-radio-group>
        </el-form-item>
        
        <el-form-item label="手机号">
          <el-input 
            v-model="form.phone" 
            placeholder="请输入手机号"
            size="large"
            maxlength="11"
          />
        </el-form-item>
        
        <el-form-item label="邮箱">
          <el-input 
            v-model="form.email" 
            placeholder="请输入邮箱"
            size="large"
            type="email"
          />
        </el-form-item>
        
        <el-form-item>
          <el-button 
            type="primary" 
            @click="save" 
            :loading="saving"
            size="large"
            class="save-button"
          >
            保存修改
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, onMounted } from 'vue'
import { Avatar, Camera } from '@element-plus/icons-vue'
import api from '../api'
import { ElMessage } from 'element-plus'

const form = reactive<any>({ 
  id: null, 
  username: '', 
  nickname: '', 
  gender: '保密', 
  phone: '', 
  email: '', 
  avatarUrl: '' 
})
const saving = ref(false)
const uploading = ref(false)
const fileInput = ref<HTMLInputElement | null>(null)

function triggerUpload() {
  fileInput.value?.click()
}

async function load() {
  const userId = localStorage.getItem('userId')
  if (!userId) return
  
  try {
    const { data } = await api.get('/user/profile', { params: { id: userId } })
    if (data.code === 0) {
      Object.assign(form, data.data)
      // Update localStorage with latest info
      if (data.data.nickname) {
        localStorage.setItem('username', data.data.nickname)
      }
      if (data.data.avatarUrl) {
        localStorage.setItem('userAvatar', data.data.avatarUrl)
      }
    }
  } catch (error) {
    ElMessage.error('加载用户信息失败')
  }
}

async function save() {
  if (!form.nickname?.trim()) {
    ElMessage.warning('请输入昵称')
    return
  }
  
  // Validate phone number if provided
  if (form.phone && !/^1[3-9]\d{9}$/.test(form.phone)) {
    ElMessage.warning('请输入正确的手机号码')
    return
  }
  
  // Validate email if provided
  if (form.email && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.email)) {
    ElMessage.warning('请输入正确的邮箱地址')
    return
  }
  
  saving.value = true
  try {
    const { data } = await api.put('/user/profile', form)
    if (data.code === 0) {
      ElMessage.success('保存成功')
      // Update localStorage
      localStorage.setItem('username', form.nickname)
      if (form.avatarUrl) {
        localStorage.setItem('userAvatar', form.avatarUrl)
      }
      // Dispatch custom event to notify App.vue
      window.dispatchEvent(new Event('user-login'))
    } else {
      ElMessage.error(data.message || '保存失败')
    }
  } catch (error) {
    ElMessage.error('保存失败，请稍后重试')
  } finally {
    saving.value = false
  }
}

async function onFile(e: Event) {
  const files = (e.target as HTMLInputElement).files
  if (!files || files.length === 0) return
  
  const file = files[0]
  
  // Validate file size (max 5MB)
  if (file.size > 5 * 1024 * 1024) {
    ElMessage.warning('图片大小不能超过5MB')
    return
  }
  
  // Validate file type
  if (!file.type.startsWith('image/')) {
    ElMessage.warning('请选择图片文件')
    return
  }
  
  uploading.value = true
  try {
    const fd = new FormData()
    fd.append('file', file)
    
    const { data } = await api.post('/file/upload', fd, { 
      headers: { 'Content-Type': 'multipart/form-data' } 
    })
    
    if (data.code === 0) {
      const pres = await api.get('/file/presigned', { 
        params: { objectName: data.data.objectName } 
      })
      form.avatarUrl = pres.data.data
      ElMessage.success('头像上传成功，请点击保存修改')
    } else {
      ElMessage.error(data.message || '上传失败')
    }
  } catch (error: any) {
    console.error('头像上传失败:', error)
    const msg = error.response?.data?.message || error.message || '上传失败，请稍后重试'
    ElMessage.error(msg)
  } finally {
    uploading.value = false
    // Reset file input
    if (fileInput.value) {
      fileInput.value.value = ''
    }
  }
}

onMounted(load)
</script>

<style scoped>
.profile-container {
  max-width: 800px;
  margin: 0 auto;
}

.profile-card {
  border-radius: 16px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1);
}

.profile-header {
  text-align: center;
  margin-bottom: 24px;
}

.avatar-section {
  margin-bottom: 24px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
}

.large-avatar {
  background: linear-gradient(135deg, #ff0050 0%, #00f2ea 100%);
  font-size: 40px;
  font-weight: bold;
  color: white;
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.4);
}

.upload-label {
  cursor: pointer;
}

.profile-title {
  font-size: 28px;
  color: #333;
  margin: 0;
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
}

.title-icon {
  color: #409eff;
}

.profile-form {
  margin-top: 32px;
}

.profile-form :deep(.el-form-item__label) {
  font-weight: 500;
  color: #666;
}

.profile-form :deep(.el-input__inner) {
  border-radius: 8px;
}

.profile-form :deep(.el-radio-group) {
  display: flex;
  gap: 24px;
}

.save-button {
  width: 100%;
  background: linear-gradient(135deg, #ff0050 0%, #00f2ea 100%);
  border: none;
  font-size: 16px;
  font-weight: 600;
  margin-top: 8px;
}

.save-button:hover {
  opacity: 0.9;
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.4);
}
</style>
