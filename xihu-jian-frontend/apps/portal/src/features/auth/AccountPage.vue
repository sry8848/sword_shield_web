<script setup lang="ts">
import { useAuthStore } from './auth'

const auth = useAuthStore()
</script>

<template>
  <section v-if="auth.user" class="account-card" aria-labelledby="account-heading">
    <h1 id="account-heading">
      我的账号
    </h1>
    <dl class="identity-summary">
      <dt>账号 ID</dt>
      <dd>{{ auth.user.id }}</dd>
      <dt>手机号</dt>
      <dd>{{ auth.user.maskedPhone }}</dd>
    </dl>
  </section>
</template>
