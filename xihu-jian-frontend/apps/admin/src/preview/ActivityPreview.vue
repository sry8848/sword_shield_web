<script setup lang="ts">
import { Calendar, Lock, User } from '@element-plus/icons-vue'
import { ElConfigProvider, ElIcon } from 'element-plus'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import 'element-plus/es/components/icon/style/css'
</script>

<template>
  <ElConfigProvider :locale="zhCn">
    <div class="workspace">
      <aside class="workspace-sidebar" aria-label="运营后台">
        <div class="workspace-brand">
          <span class="brand-icon"><ElIcon><Lock /></ElIcon></span>
          <div>
            <strong>西湖论剑</strong>
            <span>大会运营后台</span>
          </div>
        </div>
        <nav class="workspace-nav" aria-label="会务管理">
          <p>会务管理</p>
          <RouterLink :to="{ name: 'events' }">
            <ElIcon><Calendar /></ElIcon>
            <span>活动与议程</span>
          </RouterLink>
        </nav>
      </aside>

      <div class="workspace-body">
        <header class="workspace-toolbar">
          <div class="workspace-location" aria-label="当前位置">
            <span>会务管理</span><span aria-hidden="true">/</span><strong>活动与议程</strong>
          </div>
          <div class="workspace-account">
            <span class="preview-label">原型预览</span>
            <span class="account-name"><ElIcon><User /></ElIcon>会务运营</span>
          </div>
        </header>
        <main class="workspace-content">
          <RouterView />
        </main>
      </div>
    </div>
  </ElConfigProvider>
</template>

<style scoped>
.workspace { display: grid; grid-template-columns: 212px minmax(0, 1fr); min-height: 100vh; }
.workspace-sidebar { background: var(--color-surface); border-right: 1px solid var(--color-border); }
.workspace-brand { display: flex; align-items: center; gap: 12px; min-height: 72px; padding: 12px 20px; }
.brand-icon { display: grid; place-items: center; flex: none; width: 36px; height: 36px; border-radius: 9px; background: var(--color-text); color: var(--color-surface); font-size: 18px; }
.workspace-brand strong, .workspace-brand > div > span { display: block; }
.workspace-brand strong { font-size: 16px; line-height: 1.4; }
.workspace-brand > div > span { color: var(--color-muted); font-size: 12px; line-height: 1.5; }
.workspace-nav { padding: 20px 12px; }
.workspace-nav p { margin: 0 0 8px 12px; color: var(--color-muted); font-size: 12px; }
.workspace-nav a { display: flex; align-items: center; gap: 10px; min-height: 40px; padding: 8px 12px; border-radius: 8px; color: var(--color-primary); background: var(--color-info-bg); text-decoration: none; font-size: 14px; font-weight: 600; }
.workspace-toolbar { display: flex; flex-wrap: wrap; align-items: center; justify-content: space-between; gap: 12px; min-height: 64px; padding: 8px 28px; border-bottom: 1px solid var(--color-border); background: var(--color-surface); font-size: 13px; }
.workspace-location, .workspace-account, .account-name { display: flex; align-items: center; gap: 12px; white-space: nowrap; }
.workspace-location { color: var(--color-muted); }
.workspace-location strong { color: var(--color-text); font-weight: 600; }
.preview-label { padding: 4px 8px; border: 1px solid var(--color-border); border-radius: 6px; color: var(--color-muted); font-size: 12px; }
.account-name { gap: 6px; }
.workspace-content { padding: 32px 28px 48px; }
@media (max-width: 767px) {
  .workspace { grid-template-columns: minmax(0, 1fr); align-content: start; }
  .workspace-sidebar { display: flex; flex-wrap: wrap; justify-content: space-between; align-items: center; border-right: 0; border-bottom: 1px solid var(--color-border); }
  .workspace-brand { min-height: 56px; padding-inline: 12px; }
  .workspace-nav { padding: 4px 12px; }
  .workspace-nav p, .workspace-location, .account-name { display: none; }
  .workspace-toolbar { justify-content: flex-end; min-height: 36px; padding: 4px 12px; }
  .workspace-content { padding: 20px 12px 32px; }
}
</style>
