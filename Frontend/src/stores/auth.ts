import { defineStore } from 'pinia'
import { ref } from 'vue'
import { logout as apiLogout } from '../api/auth'

export const useAuthStore = defineStore('auth', () => {
  const isLoggedIn = ref(false)
  const tenant = ref('')

  function setLoggedIn(t: string) {
    isLoggedIn.value = true
    tenant.value = t
  }

  async function logout() {
    await apiLogout()
    isLoggedIn.value = false
    tenant.value = ''
  }

  return { isLoggedIn, tenant, setLoggedIn, logout }
})
