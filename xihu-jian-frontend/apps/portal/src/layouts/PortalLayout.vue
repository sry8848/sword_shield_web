<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const active = computed(() => route.path.startsWith('/activities') ? '/activities' : route.path)
function logout() { auth.logout(); router.push('/') }
</script>

<template>
  <div class="portal-shell">
    <header class="portal-header"><div class="page-container header-inner">
      <RouterLink class="brand" to="/"><span class="brand-mark">西</span><span><strong>西湖论剑</strong><small>大会门户</small></span></RouterLink>
      <nav class="main-nav">
        <RouterLink to="/" :class="{ active: active === '/' }">首页</RouterLink>
        <RouterLink to="/activities" :class="{ active: active === '/activities' }">议程与参会</RouterLink>
        <a href="/#about">了解大会</a><a href="/#activities">大会活动</a>
      </nav>
      <div class="header-actions">
        <template v-if="auth.isAuthenticated"><RouterLink class="user-link" to="/me/registrations">{{ auth.user?.displayName || '个人中心' }}</RouterLink><el-button text @click="logout">退出</el-button></template>
        <RouterLink v-else to="/login"><el-button type="primary" round>登录 / 注册</el-button></RouterLink>
      </div>
    </div></header>
    <main><RouterView /></main>
    <footer class="portal-footer"><div class="page-container footer-inner"><div><strong>西湖论剑 · 大会门户</strong><p>连接观点、技术与产业实践</p></div><span>大会信息以官方发布为准</span></div></footer>
  </div>
</template>

<style scoped>
.portal-header { position: sticky; top: 0; z-index: 20; background: rgba(255,255,255,.94); border-bottom: 1px solid #e8edf5; backdrop-filter: blur(14px); }
.header-inner { height: 76px; display: flex; align-items: center; gap: 42px; }
.brand { display: flex; align-items: center; gap: 10px; min-width: 180px; color: #173c78; }
.brand-mark { display: grid; place-items: center; width: 40px; height: 40px; border-radius: 12px; background: linear-gradient(135deg,#155eef,#62a4ff); color: white; font-size: 22px; font-weight: 800; }
.brand strong,.brand small { display: block; } .brand strong { font-size: 17px; letter-spacing: 1px; } .brand small { color: #8290a5; font-size: 11px; margin-top: 2px; }
.main-nav { display: flex; align-items: center; gap: 30px; flex: 1; height: 100%; }
.main-nav a { color: #58677e; font-size: 14px; height: 100%; display: flex; align-items: center; position: relative; white-space: nowrap; }
.main-nav a.active,.main-nav a:hover { color: #155eef; }
.main-nav a.active::after { content: ""; position: absolute; bottom: 0; left: 0; right: 0; height: 3px; background: #155eef; border-radius: 3px 3px 0 0; }
.header-actions { display: flex; align-items: center; gap: 8px; white-space: nowrap; } .user-link { color: #155eef; font-size: 14px; }
.portal-footer { margin-top: 50px; background: #102b54; color: #d9e6fa; }
.footer-inner { min-height: 130px; display: flex; justify-content: space-between; align-items: center; gap: 20px; }
.footer-inner strong { font-size: 16px; } .footer-inner p,.footer-inner span { color: #9fb3d0; font-size: 13px; }
@media (max-width: 760px) { .header-inner { height: auto; min-height: 68px; flex-wrap: wrap; gap: 10px; padding-top: 12px; padding-bottom: 10px; } .brand { min-width: 0; flex: 1; } .main-nav { order: 3; width: 100%; flex: none; height: 36px; gap: 22px; overflow-x: auto; } .footer-inner { align-items: flex-start; flex-direction: column; justify-content: center; padding: 24px 0; } }
</style>
