import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { api, TOKEN_KEY } from '../api/client'
import type { User } from '@xhlj/shared'
export const useAuthStore = defineStore('admin-auth', () => {
  const user = ref<User | null>(null)
  const accessToken = ref(sessionStorage.getItem(TOKEN_KEY))
  const isAuthenticated = computed(() => Boolean(accessToken.value))
  let hydrated = false
  async function login(username: string, password: string) {
    const result = await api.auth.login({ username, password })
    accessToken.value = result.accessToken; user.value = result.user
    sessionStorage.setItem(TOKEN_KEY, result.accessToken)
  }
  async function hydrate() {
    if (hydrated) return
    hydrated = true
    if (!accessToken.value) return
    try { user.value = await api.auth.me() }
    catch { logout() }
  }
  function logout() { user.value = null; accessToken.value = null; sessionStorage.removeItem(TOKEN_KEY) }
  return { user, accessToken, isAuthenticated, login, hydrate, logout }
})
