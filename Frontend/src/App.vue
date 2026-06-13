<template>
  <div id="app">

    <LoginView v-if="!auth.isLoggedIn" />

    <template v-else>
      <header class="tab-bar">
        <div class="brand">{{ brand }}</div>
        <nav class="tabs">
          <button
            v-for="t in tabs"
            :key="t.id"
            class="tab"
            :class="{ active: activeTab === t.id }"
            @click="activeTab = t.id"
          >
            <component :is="t.icon" class="tab-icon" />
            {{ t.label }}
          </button>
        </nav>
        <div class="spacer" />
        <select class="lang-select" v-model="theme.theme" @change="theme.setTheme(theme.theme)">
          <option v-for="th in theme.themes" :key="th.id" :value="th.id">{{ th.name }}</option>
        </select>
        <select class="lang-select" v-model="i18n.locale" @change="i18n.setLocale(i18n.locale)">
          <option v-for="l in i18n.locales" :key="l.id" :value="l.id">{{ l.name }}</option>
        </select>
        <button class="btn btn-ghost btn-sm" style="align-self:center" @click="showHelp = true">{{ i18n.t('app.help') }}</button>
        <button class="btn btn-ghost btn-sm" style="align-self:center" @click="auth.logout()">{{ i18n.t('app.signOut') }}</button>
      </header>

      <Teleport to="body">
        <div v-if="showHelp" class="modal-backdrop" @click.self="showHelp = false">
          <div class="modal-popup">
            <div class="modal-header">
              <span class="modal-header-title">{{ i18n.t('app.help') }}</span>
              <button type="button" class="modal-close" @click="showHelp = false">✕</button>
            </div>
            <div class="modal-body help-body">

              <h4>{{ i18n.t('app.helpModal.documentsTitle') }}</h4>
              <template v-if="auth.isAdmin">
                <p v-html="i18n.t('app.helpModal.documentsIntro')"></p>
                <ul>
                  <li v-html="i18n.t('app.helpModal.documentsItem1')"></li>
                  <li v-html="i18n.t('app.helpModal.documentsItem2')"></li>
                  <li v-html="i18n.t('app.helpModal.documentsItem3')"></li>
                  <li v-html="i18n.t('app.helpModal.documentsItem4')"></li>
                  <li v-html="i18n.t('app.helpModal.documentsItem5')"></li>
                </ul>
              </template>
              <p v-else v-html="i18n.t('app.helpModal.documentsIntroUser')"></p>

              <h4>{{ i18n.t('app.helpModal.searchTitle') }}</h4>
              <p v-html="i18n.t('app.helpModal.searchIntro')"></p>
              <ul>
                <li v-html="i18n.t('app.helpModal.searchItem1')"></li>
                <li v-html="i18n.t('app.helpModal.searchItem2')"></li>
                <li v-html="i18n.t('app.helpModal.searchItem3')"></li>
                <li v-html="i18n.t('app.helpModal.searchItem4')"></li>
                <li v-html="i18n.t('app.helpModal.searchItem5')"></li>
              </ul>
              <p class="help-tip-accent" v-html="i18n.t('app.helpModal.searchTip')"></p>

              <h4>{{ i18n.t('app.helpModal.chatTitle') }}</h4>
              <p v-html="i18n.t('app.helpModal.chatIntro')"></p>
              <ul>
                <li v-html="i18n.t('app.helpModal.chatItem1')"></li>
                <li v-html="i18n.t('app.helpModal.chatItem2')"></li>
                <li v-html="i18n.t('app.helpModal.chatItem3')"></li>
                <li v-html="i18n.t('app.helpModal.chatItem4')"></li>
                <li v-html="i18n.t('app.helpModal.chatItem5')"></li>
                <li v-html="i18n.t('app.helpModal.chatItem6')"></li>
              </ul>

            </div>
          </div>
        </div>
      </Teleport>

      <ExplorerView v-show="activeTab === 'browse'" />
      <SearchView   ref="searchRef" v-show="activeTab === 'search'" />
      <ChatView     ref="chatRef"   v-show="activeTab === 'chat'" />
    </template>

    <ConfirmDialog />
  </div>
</template>

<script setup lang="ts">
import ChatView from './views/ChatView.vue'
import { useAuthStore } from './stores/auth'
import { useI18nStore } from './stores/i18n'
import LoginView from './views/LoginView.vue'
import { useThemeStore } from './stores/theme'
import SearchView from './views/SearchView.vue'
import ExplorerView from './views/ExplorerView.vue'
import ConfirmDialog from './components/ConfirmDialog.vue'
import { ref, watch, computed, nextTick, h, onMounted, onUnmounted } from 'vue'


const auth = useAuthStore()
const i18n = useI18nStore()
const theme = useThemeStore()
theme.init()
const showHelp = ref(false)
const _host = window.location.hostname.split('.')[0]
const brand = _host.charAt(0).toUpperCase() + _host.slice(1)


const activeTab = ref<'browse' | 'search' | 'chat'>('browse')


const searchRef = ref<InstanceType<typeof SearchView> | null>(null)
const chatRef = ref<InstanceType<typeof ChatView> | null>(null)


watch(activeTab, tab =>
{
  nextTick(() =>
  {
    if (tab === 'search') searchRef.value?.focus()
    if (tab === 'chat') chatRef.value?.focus()
  })
})


const IconBrowse = { render: () => h('svg', { width: 14, height: 14, viewBox: '0 0 20 20', fill: 'currentColor' }, [
  h('path', { d: 'M2 6a2 2 0 012-2h4l2 2h6a2 2 0 012 2v6a2 2 0 01-2 2H4a2 2 0 01-2-2V6z' })
]) }


const IconSearch = { render: () => h('svg', { width: 14, height: 14, viewBox: '0 0 20 20', fill: 'currentColor' }, [
  h('path', { 'fill-rule': 'evenodd', d: 'M8 4a4 4 0 100 8 4 4 0 000-8zM2 8a6 6 0 1110.89 3.476l4.817 4.817a1 1 0 01-1.414 1.414l-4.816-4.816A6 6 0 012 8z', 'clip-rule': 'evenodd' })
]) }


const IconChat = { render: () => h('svg', { width: 14, height: 14, viewBox: '0 0 20 20', fill: 'currentColor' }, [
  h('path', { d: 'M2 5a2 2 0 012-2h7a2 2 0 012 2v4a2 2 0 01-2 2H9l-3 3v-3H4a2 2 0 01-2-2V5z' }),
  h('path', { d: 'M15 7v2a4 4 0 01-4 4H9.828l-1.766 1.767c.28.149.599.233.938.233h2l3 3v-3h2a2 2 0 002-2V9a2 2 0 00-2-2h-1z' })
]) }


function onKeydown(e: KeyboardEvent)
{
  if (e.key === 'Escape') showHelp.value = false
}


onMounted(() => window.addEventListener('keydown', onKeydown))
onUnmounted(() => window.removeEventListener('keydown', onKeydown))


const tabs = computed(() => [
  { id: 'browse' as const, label: i18n.t('app.tabs.browse'), icon: IconBrowse },
  { id: 'search' as const, label: i18n.t('app.tabs.search'), icon: IconSearch },
  { id: 'chat' as const, label: i18n.t('app.tabs.chat'), icon: IconChat },
])
</script>

<style>
* { box-sizing: border-box; margin: 0; padding: 0; }
body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif; background: var(--bg); }
a { text-decoration: none; }

#app {
  display: flex;
  flex-direction: column;
  height: 100vh;
  overflow: hidden;
}
</style>

<style scoped>
/* ── Tab bar ── */
.tab-bar {
  display: flex;
  align-items: flex-end;
  height: 42px;
  padding: 0 14px;
  background: var(--tab-bar-bg);
  border-bottom: 2px solid var(--tab-border);
  flex-shrink: 0;
  gap: 2px;
}

.brand {
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.06em;
  text-transform: uppercase;
  color: var(--text-muted);
  padding-right: 16px;
  border-right: 1px solid var(--tab-border);
  margin-right: 6px;
  white-space: nowrap;
  user-select: none;
  align-self: center;
}

.tabs {
  display: flex;
  align-items: flex-end;
  height: 100%;
  gap: 2px;
}

/* Browser-style tab: rounded top corners, open bottom */
.tab {
  display: flex;
  align-items: center;
  gap: 5px;
  height: 32px;
  padding: 0 15px;
  background: var(--tab-bg);
  border: 1px solid var(--tab-border);
  border-bottom: none;
  border-radius: 6px 6px 0 0;
  font-size: 12.5px;
  font-weight: 500;
  color: var(--tab-text);
  cursor: pointer;
  white-space: nowrap;
  /* drop 2px to overlap and hide the bar's border-bottom */
  position: relative;
  bottom: -2px;
  transition: background 0.1s, color 0.1s;
}

.tab:hover:not(.active) {
  background: var(--tab-hover-bg);
  color: var(--text);
}

/* Active tab looks flush with the page content */
.tab.active {
  background: var(--bg);
  color: var(--text);
  font-weight: 600;
  border-color: var(--tab-border);
  border-bottom: 2px solid var(--bg); /* covers the bar's bottom border */
  z-index: 1;
}

.tab-icon {
  flex-shrink: 0;
  opacity: 0.65;
}
.tab.active .tab-icon { opacity: 1; color: var(--accent); }

.lang-select {
  align-self: center;
  height: 26px;
  padding: 0 4px;
  font-size: 12px;
  font-weight: 600;
  border: 1px solid var(--tab-border);
  border-radius: 4px;
  background: var(--tab-bg);
  color: var(--text);
  cursor: pointer;
  margin-right: 6px;
}
.lang-select:last-of-type {
  margin-right: 32px;
}

</style>
