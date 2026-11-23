<template>
  <header class="portal-header">
    <nav class="portal-nav" aria-label="主导航">
      <RouterLink to="/" class="portal-brand">
        西湖论剑
      </RouterLink>
      <RouterLink :to="{ name: 'account' }">
        我的账号
      </RouterLink>
    </nav>
  </header>
  <main class="portal-main">
    <RouterView />
  </main>
</template>
