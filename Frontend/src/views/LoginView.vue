<template>
  <div class="login-page">

    <div class="login-toolbar">
      <div class="locale-flags">
        <button
          v-for="l in i18n.locales"
          :key="l.id"
          type="button"
          class="flag-btn"
          :class="{ active: i18n.locale === l.id }"
          :title="l.name"
          @click="i18n.setLocale(l.id)"
        >{{ flagFor(l.id) }}</button>
      </div>
      <div class="theme-swatches">
        <button
          v-for="th in theme.themes"
          :key="th.id"
          type="button"
          class="swatch-btn"
          :class="{ active: theme.theme === th.id }"
          :title="th.name"
          :style="swatchStyle(th.id)"
          @click="theme.setTheme(th.id)"
        />
      </div>
    </div>

    <!-- Tenant picker shown after login when user has multiple tenants -->
    <div v-if="tenants.length > 1" class="login-card">
      <div class="login-header">
        <span class="login-brand">{{ i18n.t('login.brand') }}</span>
        <span class="login-subtitle">{{ i18n.t('login.selectTenant') }}</span>
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
        <span class="login-brand">{{ i18n.t('login.brand') }}</span>
        <span v-if="tenant" class="login-tenant">{{ tenant }}</span>
      </div>

      <form class="login-form" @submit.prevent="submit">
        <div class="login-field">
          <label class="field-label" for="username">{{ i18n.t('login.username') }}</label>
          <input
            id="username"
            ref="usernameInput"
            v-model="username"
            class="input"
            type="text"
            autocomplete="username"
            :placeholder="i18n.t('login.username')"
            :disabled="loading"
            required
          />
        </div>

        <div class="login-field">
          <label class="field-label" for="password">{{ i18n.t('login.password') }}</label>
          <input
            id="password"
            v-model="password"
            class="input"
            type="password"
            autocomplete="current-password"
            :placeholder="i18n.t('login.password')"
            :disabled="loading"
            required
          />
        </div>

        <div v-if="error" class="notice notice-error">{{ error }}</div>

        <button class="btn btn-primary btn-lg login-btn" type="submit" :disabled="loading">
          <span v-if="loading" class="spinner spinner-sm"></span>
          {{ loading ? i18n.t('login.signingIn') : i18n.t('login.signIn') }}
        </button>
      </form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { login, getTenants, switchTenant } from '../api/auth'
import { useAuthStore } from '../stores/auth'
import { useI18nStore } from '../stores/i18n'
import { useThemeStore } from '../stores/theme'


const auth = useAuthStore()
const i18n = useI18nStore()
const theme = useThemeStore()


const FLAGS: Record<string, string> = { en: '🇬🇧', da: '🇩🇰' }
const SWATCH_COLORS: Record<string, { bg: string; accent: string }> = {
  light:  { bg: '#ffffff', accent: '#a0abb7' },
  dark:   { bg: '#1a1d23', accent: '#5b6b7d' },
  blue:   { bg: '#ffffff', accent: '#3b82f6' },
  forest: { bg: '#f7faf7', accent: '#4d8061' },
  sepia:  { bg: '#faf3e7', accent: '#a9824a' },
  purple: { bg: '#1e1b2e', accent: '#9d7be0' }
}


function flagFor(locale: string): string
{
  return(FLAGS[locale] ?? '🌐')
}


function swatchStyle(id: string)
{
  const c = SWATCH_COLORS[id] ?? { bg: '#888', accent: '#aaa' }
  return({ background: `linear-gradient(135deg, ${c.bg} 50%, ${c.accent} 50%)` })
}


const tenant = ref('')
const username = ref('')
const password = ref('')
const loading = ref(false)
const error = ref('')
const usernameInput = ref<HTMLInputElement | null>(null)


const tenants = ref<string[]>([])
const switching = ref(false)
const switchingTo = ref('')


onMounted(() => usernameInput.value?.focus())


async function sha256(str: string): Promise<string>
{
  const buf = await crypto.subtle.digest('SHA-256', new TextEncoder().encode(str))
  return(Array.from(new Uint8Array(buf)).map(b => b.toString(16).padStart(2, '0')).join(''))
}


async function submit()
{
  error.value = ''
  loading.value = true
  try
  {
    const hashed = await sha256(password.value)
    const result = await login(username.value, hashed)
    if (!result.success)
    {
      error.value = `[${(result as any)._status}] ${result.message ?? '(no message)'}`
      return
    }
    if (result.tenant)
    {
      auth.setLoggedIn(result.tenant, result.admin ?? false)
      return
    }
    const res = await getTenants()
    if (res.tenants.length === 1)
    {
      const switched = await switchTenant(res.tenants[0])
      await finalizeTenant(res.tenants[0], switched.admin ?? false)
    }
    else if (res.tenants.length > 1)
    {
      tenants.value = res.tenants
    }
    else
    {
      error.value = i18n.t('login.noTenants')
    }
  }
  catch
  {
    error.value = i18n.t('login.unreachable')
  }
  finally
  {
    loading.value = false
  }
}


async function selectTenant(t: string)
{
  error.value = ''
  switching.value = true
  switchingTo.value = t
  try
  {
    const res = await switchTenant(t)
    if (res.success)
    {
      await finalizeTenant(t, res.admin ?? false)
    }
    else
    {
      error.value = res.message ?? i18n.t('login.switchFailed')
    }
  }
  catch
  {
    error.value = i18n.t('login.unreachable')
  }
  finally
  {
    switching.value = false
    switchingTo.value = ''
  }
}


async function finalizeTenant(t: string, admin: boolean)
{
  tenant.value = t
  tenants.value = []
  auth.setLoggedIn(t, admin)
}
</script>

<style scoped>
.login-page {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 18px;
  padding-bottom: 30vh;
  background: var(--bg-subtle);
}

.login-toolbar {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
}

.locale-flags,
.theme-swatches {
  display: flex;
  gap: 6px;
}

.flag-btn {
  width: 30px;
  height: 30px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  border: 1.5px solid transparent;
  border-radius: 6px;
  background: var(--bg-muted);
  cursor: pointer;
  transition: border-color 0.1s;
}
.flag-btn:hover { border-color: var(--border); }
.flag-btn.active { border-color: var(--accent); }

.swatch-btn {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  border: 2px solid var(--border);
  cursor: pointer;
  padding: 0;
  transition: border-color 0.1s, transform 0.1s;
}
.swatch-btn:hover { transform: scale(1.1); }
.swatch-btn.active { border-color: var(--accent); }

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
