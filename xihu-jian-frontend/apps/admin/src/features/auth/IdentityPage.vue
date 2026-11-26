<script setup lang="ts">
import { adminRoleNames, useAdminAuth } from './auth'

const auth = useAdminAuth()
</script>

<template>
  <section v-if="auth.profile" class="admin-card" aria-labelledby="identity-heading">
    <h1 id="identity-heading">
      后台身份
    </h1>
    <dl class="identity-summary">
      <dt>手机号</dt>
      <dd>{{ auth.profile.user.maskedPhone }}</dd>
      <dt>后台角色</dt>
      <dd>{{ auth.profile.roles.map(role => adminRoleNames[role]).join('、') }}</dd>
    </dl>
  </section>
</template>
