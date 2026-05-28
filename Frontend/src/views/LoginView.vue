<template>
  <div class="login-page">
    <!-- Tenant picker shown after login when user has multiple tenants -->
    <div v-if="tenants.length > 1" class="login-card">
      <div class="login-header">
        <span class="login-brand">Documents</span>
        <span class="login-subtitle">Select a tenant</span>
      </div>
      <div class="tenant-list">
        <button
          v-for="t in tenants"
          :key="t"
          class="tenant-item"
          :disabled="switching"
          @click="selectTenant(t)"
        >
          {{ t }}
          <span v-if="switching && switchingTo === t" class="spinner spinner-sm" />
        </button>
      </div>
      <div v-if="error" class="tenant-error notice notice-error">{{ error }}</div>
    </div>

    <!-- Login form -->
    <div v-else class="login-card">
      <div class="login-header">
        <span class="login-brand">Documents</span>
        <span v-if="tenant" class="login-tenant">{{ tenant }}</span>
      </div>

      <form class="login-form" @submit.prevent="submit">
        <div class="login-field">
          <label class="field-label" for="username">Username</label>
          <input
            id="username"
            ref="usernameInput"
            v-model="username"
            class="input"
            type="text"
            autocomplete="username"
            placeholder="Username"
            :disabled="loading"
            required
          />
        </div>

        <div class="login-field">
          <label class="field-label" for="password">Password</label>
          <input
            id="password"
            v-model="password"
            class="input"
            type="password"
            autocomplete="current-password"
            placeholder="Password"
            :disabled="loading"
            required
          />
        </div>

        <div v-if="error" class="notice notice-error">{{ error }}</div>

        <button class="btn btn-primary btn-lg login-btn" type="submit" :disabled="loading">
          <span v-if="loading" class="spinner spinner-sm"></span>
          {{ loading ? 'Signing in…' : 'Sign in' }}
        </button>
      </form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { login, getTenants, switchTenant } from '../api/auth'
import { useAuthStore } from '../stores/auth'

const auth = useAuthStore()

const tenant = ref('')
const username = ref('')
const password = ref('')
const loading = ref(false)
const error = ref('')
const usernameInput = ref<HTMLInputElement | null>(null)

const tenants = ref<string[]>([])
const switching = ref(false)
const switchingTo = ref('')

onMounted(() => {
  tenant.value = window.location.pathname.split('/').filter(Boolean)[0] ?? ''
  usernameInput.value?.focus()
})

async function sha256(str: string): Promise<string> {
  const buf = await crypto.subtle.digest('SHA-256', new TextEncoder().encode(str))
  return Array.from(new Uint8Array(buf)).map(b => b.toString(16).padStart(2, '0')).join('')
}

async function submit() {
  error.value = ''
  loading.value = true
  try {
    const hashed = await sha256(username.value + password.value)
    const result = await login(username.value, hashed, tenant.value || undefined)
    if (!result.success) {
      error.value = result.message ?? 'Invalid credentials.'
      return
    }
    if (tenant.value) {
      auth.setLoggedIn(tenant.value)
      return
    }
    // No tenant in URL — discover which tenants the user can access
    const res = await getTenants()
    if (res.tenants.length === 1) {
      await finalizeTenant(res.tenants[0])
    } else if (res.tenants.length > 1) {
      tenants.value = res.tenants
    } else {
      error.value = 'No tenants available for this account.'
    }
  } catch {
    error.value = 'Could not reach the server.'
  } finally {
    loading.value = false
  }
}

async function selectTenant(t: string) {
  error.value = ''
  switching.value = true
  switchingTo.value = t
  try {
    const res = await switchTenant(t)
    if (res.success) {
      await finalizeTenant(t)
    } else {
      error.value = res.message ?? 'Could not switch tenant.'
    }
  } catch {
    error.value = 'Could not reach the server.'
  } finally {
    switching.value = false
    switchingTo.value = ''
  }
}

async function finalizeTenant(t: string) {
  history.pushState({}, '', '/' + t)
  tenant.value = t
  tenants.value = []
  auth.setLoggedIn(t)
}
</script>

<style scoped>
.login-page {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--bg-subtle);
}

.login-card {
  width: min(360px, 90vw);
  background: var(--bg);
  border: 1px solid var(--border);
  border-radius: 10px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.10);
  overflow: hidden;
}

.login-header {
  display: flex;
  align-items: baseline;
  gap: 10px;
  padding: 18px 22px 14px;
  border-bottom: 1px solid var(--border);
  background: var(--tab-bar-bg);
}

.login-brand {
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.06em;
  text-transform: uppercase;
  color: var(--text-muted);
}

.login-tenant {
  font-size: 13px;
  font-weight: 600;
  color: var(--text);
  text-transform: capitalize;
}

.login-subtitle {
  font-size: 13px;
  color: var(--text-muted);
}

.login-form {
  display: flex;
  flex-direction: column;
  gap: 14px;
  padding: 22px;
}

.login-field {
  display: flex;
  flex-direction: column;
  gap: 5px;
}

.login-btn {
  width: 100%;
  margin-top: 4px;
}

.tenant-list {
  display: flex;
  flex-direction: column;
  padding: 12px;
  gap: 6px;
}

.tenant-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 40px;
  padding: 0 14px;
  background: var(--bg-subtle);
  border: 1.5px solid var(--border);
  border-radius: 6px;
  font-size: 13px;
  font-weight: 500;
  color: var(--text);
  text-transform: capitalize;
  cursor: pointer;
  transition: background 0.1s, border-color 0.1s;
}

.tenant-item:hover:not(:disabled) {
  background: var(--accent-ring);
  border-color: var(--accent);
  color: var(--accent-dark);
}

.tenant-item:disabled { opacity: 0.5; cursor: default; }

.tenant-error { margin: 0 12px 12px; }
</style>
