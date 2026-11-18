<template>
  <div class="panel">
    <div v-if="error" class="panel-error">{{ error }}</div>
    <div v-else-if="loaded && !items.length" class="panel-empty">{{ i18n.t('home.panelEmpty') }}</div>

    <div v-for="item in items" :key="item.id" class="panel-row" :class="{ unavailable: !item.available }">
      <span class="panel-icon">
        <svg v-if="item.domain === 'light'" width="16" height="16" viewBox="0 0 20 20" fill="currentColor">
          <path d="M11 3a1 1 0 10-2 0v1a1 1 0 102 0V3zM15.657 5.757a1 1 0 00-1.414-1.414l-.707.707a1 1 0 001.414 1.414l.707-.707zM18 10a1 1 0 01-1 1h-1a1 1 0 110-2h1a1 1 0 011 1zM5.05 6.464A1 1 0 106.464 5.05l-.707-.707a1 1 0 00-1.414 1.414l.707.707zM5 10a1 1 0 01-1 1H3a1 1 0 110-2h1a1 1 0 011 1zM8 16v-1h4v1a2 2 0 11-4 0zM12 14c.015-.34.208-.646.477-.859a4 4 0 10-4.954 0c.27.213.462.519.476.859h4.002z"/>
        </svg>
        <svg v-else width="16" height="16" viewBox="0 0 20 20" fill="currentColor">
          <path fill-rule="evenodd" d="M3 5a2 2 0 012-2h10a2 2 0 012 2v8a2 2 0 01-2 2h-2.22l.123.489.804.804A1 1 0 0113 18H7a1 1 0 01-.707-1.707l.804-.804L7.22 15H5a2 2 0 01-2-2V5zm5.771 7H5V5h10v7H8.771z" clip-rule="evenodd"/>
        </svg>
      </span>

      <span class="panel-name">{{ item.name }}</span>

      <label class="switch" :title="item.on ? i18n.t('home.turnOff') : i18n.t('home.turnOn')">
        <input type="checkbox" :checked="item.on" :disabled="!item.available || busy[item.id]" @change="toggle(item)" />
        <span class="switch-track"></span>
      </label>

      <input
        type="range"
        min="0"
        :max="item.max ?? 100"
        step="5"
        class="level"
        :value="item.level"
        :disabled="!item.available || busy[item.id]"
        :title="item.domain === 'light' ? i18n.t('home.brightness') : i18n.t('home.volume')"
        @input="item.level = Number(($event.target as HTMLInputElement).value)"
        @change="setLevel(item)"
      />
      <button
        v-if="item.domain === 'media_player'"
        type="button"
        class="btn btn-ghost btn-sm mute-btn"
        :class="{ muted: item.muted }"
        :disabled="!item.available || busy[item.id]"
        :title="item.muted ? i18n.t('home.unmute') : i18n.t('home.mute')"
        @click="toggleMute(item)"
      >
        <svg v-if="item.muted" width="14" height="14" viewBox="0 0 20 20" fill="currentColor">
          <path fill-rule="evenodd" d="M9.383 3.076A1 1 0 0110 4v12a1 1 0 01-1.707.707L4.586 13H2a1 1 0 01-1-1V8a1 1 0 011-1h2.586l3.707-3.707a1 1 0 011.09-.217zM12.293 7.293a1 1 0 011.414 0L15 8.586l1.293-1.293a1 1 0 111.414 1.414L16.414 10l1.293 1.293a1 1 0 01-1.414 1.414L15 11.414l-1.293 1.293a1 1 0 01-1.414-1.414L13.586 10l-1.293-1.293a1 1 0 010-1.414z" clip-rule="evenodd"/>
        </svg>
        <svg v-else width="14" height="14" viewBox="0 0 20 20" fill="currentColor">
          <path fill-rule="evenodd" d="M9.383 3.076A1 1 0 0110 4v12a1 1 0 01-1.707.707L4.586 13H2a1 1 0 01-1-1V8a1 1 0 011-1h2.586l3.707-3.707a1 1 0 011.09-.217zM14.657 2.929a1 1 0 011.414 0A9.972 9.972 0 0119 10a9.972 9.972 0 01-2.929 7.071 1 1 0 01-1.414-1.414A7.971 7.971 0 0017 10c0-2.21-.894-4.208-2.343-5.657a1 1 0 010-1.414zm-2.829 2.828a1 1 0 011.415 0A5.983 5.983 0 0115 10a5.984 5.984 0 01-1.757 4.243 1 1 0 01-1.415-1.415A3.984 3.984 0 0013 10a3.983 3.983 0 00-1.172-2.828 1 1 0 010-1.415z" clip-rule="evenodd"/>
        </svg>
      </button>
      <span v-else class="panel-value">{{ item.available ? item.level + '%' : 'NA' }}</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { PanelItem } from '../types'
import { useI18nStore } from '../stores/i18n'
import { getPanel, controlPanel } from '../api/home'
import { ref, reactive, onMounted, onUnmounted } from 'vue'


const REFRESH_MS = 30000


const i18n = useI18nStore()
const items = ref<PanelItem[]>([])
const error = ref('')
const loaded = ref(false)
const busy = reactive<Record<string, boolean>>({})


let timer: ReturnType<typeof setInterval> | null = null
let reloadTimer: ReturnType<typeof setTimeout> | null = null


async function load()
{
  try
  {
    const res = await getPanel()
    items.value = res.data.items as PanelItem[]
    error.value = ''
    loaded.value = true
  }
  catch
  {
    error.value = i18n.t('home.panelError')
  }
}


function replace(updated: PanelItem)
{
  const i = items.value.findIndex(x => x.id === updated.id)
  if (i >= 0) items.value[i] = updated
}


// Home Assistant applies changes asynchronously; reload shortly after to show the real state
function reloadSoon()
{
  if (reloadTimer) clearTimeout(reloadTimer)
  reloadTimer = setTimeout(load, 1500)
}


async function send(item: PanelItem, action: string, value?: number)
{
  busy[item.id] = true
  try
  {
    const res = await controlPanel(item.id, action, value)
    replace(res.data.item as PanelItem)
    error.value = ''
  }
  catch
  {
    error.value = i18n.t('home.panelError')
  }
  finally
  {
    busy[item.id] = false
    reloadSoon()
  }
}


function toggle(item: PanelItem)
{
  const action = item.on ? 'off' : 'on'
  item.on = !item.on
  send(item, action)
}


function setLevel(item: PanelItem)
{
  if (item.domain === 'light') item.on = item.level > 0
  send(item, 'level', item.level)
}


function toggleMute(item: PanelItem)
{
  const action = item.muted ? 'unmute' : 'mute'
  item.muted = !item.muted
  send(item, action)
}


onMounted(() =>
{
  load()
  timer = setInterval(() =>
  {
    if (document.visibilityState === 'visible') load()
  }, REFRESH_MS)
})

onUnmounted(() =>
{
  if (timer) clearInterval(timer)
  if (reloadTimer) clearTimeout(reloadTimer)
})

defineExpose({ load })
</script>

<style scoped>
.panel {
  display: flex;
  flex-direction: column;
  gap: 2px;
  padding: 6px 12px;
  background: var(--bg-subtle);
  border: 1px solid var(--border);
  border-radius: 8px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.08);
}

.panel-empty {
  font-size: 12px;
  color: var(--text-muted);
  max-width: 260px;
  padding: 4px 0;
}

.panel-error {
  font-size: 12px;
  color: var(--danger);
  padding: 2px 0 4px;
}

.panel-row {
  display: grid;
  grid-template-columns: 16px 64px 36px 110px 36px;
  justify-content: start;
  align-items: center;
  gap: 8px;
  min-height: 32px;
}
.panel-row.unavailable { opacity: 0.45; }

.panel-icon { color: var(--text-muted); display: inline-flex; }
.panel-name { font-size: 13px; color: var(--text); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }

.panel-value {
  font-size: 12px;
  font-variant-numeric: tabular-nums;
  color: var(--text-muted);
  text-align: right;
}

.level {
  width: 100%;
  accent-color: var(--accent);
  cursor: pointer;
}
.level:disabled { cursor: default; }

/* ── On/off switch ── */
.switch {
  position: relative;
  display: inline-block;
  width: 36px;
  height: 20px;
}
.switch input {
  opacity: 0;
  width: 0;
  height: 0;
}
.switch-track {
  position: absolute;
  inset: 0;
  background: var(--border-input);
  border-radius: 10px;
  cursor: pointer;
  transition: background 0.15s;
}
.switch-track::before {
  content: '';
  position: absolute;
  width: 14px;
  height: 14px;
  left: 3px;
  top: 3px;
  background: var(--bg);
  border-radius: 50%;
  transition: transform 0.15s;
}
.switch input:checked + .switch-track { background: var(--accent); }
.switch input:checked + .switch-track::before { transform: translateX(16px); }
.switch input:disabled + .switch-track { cursor: default; }
.switch input:focus-visible + .switch-track { box-shadow: 0 0 0 3px var(--accent-ring); }

.mute-btn { padding: 4px 6px; color: var(--text-muted); justify-self: end; }
.mute-btn.muted { color: var(--danger); }

@media (max-width: 520px) {
  .panel-row { grid-template-columns: 16px minmax(60px, 1fr) 36px minmax(90px, 140px) 36px; gap: 6px; }
}
</style>
