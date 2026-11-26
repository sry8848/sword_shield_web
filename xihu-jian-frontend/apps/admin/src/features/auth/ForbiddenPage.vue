<template>
  <section class="admin-card" aria-labelledby="forbidden-heading">
    <h1 id="forbidden-heading">
      没有页面访问权限
    </h1>
    <p>当前后台角色无法访问这个页面。</p>
    <RouterLink :to="{ name: 'identity' }">
      返回后台首页
    </RouterLink>
  </section>
</template>
