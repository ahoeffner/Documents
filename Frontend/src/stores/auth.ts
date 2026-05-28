import { ref } from 'vue'
import { defineStore } from 'pinia'
import { logout as apiLogout } from '../api/auth'


export const useAuthStore = defineStore('auth', () =>
{
  const isLoggedIn = ref(false)
  const isAdmin = ref(false)
  const tenant = ref('')


  function setLoggedIn(t: string, admin: boolean)
  {
    isLoggedIn.value = true
    isAdmin.value = admin
    tenant.value = t
  }


  async function logout()
  {
    await apiLogout()
    isLoggedIn.value = false
    isAdmin.value = false
    tenant.value = ''
  }


  return({ isLoggedIn, isAdmin, tenant, setLoggedIn, logout })
})
