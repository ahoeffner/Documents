import { api } from './index'

export async function login(username: string, password: string, tenant?: string) {
  const { data } = await api.post('/auth/login', { username, password, ...(tenant ? { tenant } : {}) })
  return data as { success: boolean; message?: string }
}

export async function logout() {
  const { data } = await api.post('/auth/logout')
  return data as { success: boolean }
}

export async function getTenants() {
  const { data } = await api.get('/auth/tenants')
  return data as { success: boolean; tenants: string[] }
}

export async function switchTenant(tenant: string) {
  const { data } = await api.post('/auth/switch', { tenant })
  return data as { success: boolean; message?: string }
}
