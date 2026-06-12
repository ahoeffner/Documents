<template>
  <Teleport to="body">
    <div v-if="visible" class="modal-backdrop" @click.self="$emit('close')">
      <div class="modal-popup link-modal">
        <div class="modal-header">
          <span class="modal-header-title">{{ title ?? i18n.t('explorer.linkToFolder') }}</span>
          <button type="button" class="modal-close" @click="$emit('close')">✕</button>
        </div>

        <div class="link-search-row">
          <input
            ref="searchEl"
            v-model="filterQuery"
            type="text"
            class="link-search-input"
            :placeholder="i18n.t('linkFolderModal.filterPlaceholder')"
            @keydown.enter.prevent="pickFirst"
          />
        </div>

        <template v-if="!filterQuery && recentFolders.length">
          <div class="link-section-label">{{ i18n.t('linkFolderModal.recent') }}</div>
          <div class="chip-row">
            <button
              v-for="f in recentFolders"
              :key="f.id"
              class="folder-chip"
              :class="{ active: selected === f.id }"
              @click="selected = f.id"
            >{{ f.name }}</button>
          </div>
        </template>

        <div class="link-section-label">{{ filterQuery ? i18n.t('linkFolderModal.results') : i18n.t('linkFolderModal.allFolders') }}</div>
        <div class="folder-list">
          <button
            v-for="f in filteredFolders"
            :key="f.id"
            class="folder-list-item"
            :class="{ active: selected === f.id }"
            @click="selected = f.id"
          >{{ f.name }}</button>
          <div v-if="!filteredFolders.length" class="folder-list-empty">{{ i18n.t('linkFolderModal.noMatch') }}</div>
        </div>

        <div class="modal-actions">
          <button class="btn btn-ghost btn-sm" @click="$emit('close')">{{ i18n.t('common.cancel') }}</button>
          <button class="btn btn-primary btn-sm" :disabled="!selected" @click="confirm">{{ props.confirmLabel ?? i18n.t('linkFolderModal.link') }}</button>
        </div>
      </div>
    </div>
  </Teleport>
</template>

<script setup lang="ts">
import { useI18nStore } from '../stores/i18n'
import { useCategoriesStore } from '../stores/categories'
import { ref, computed, watch, nextTick, onMounted, onUnmounted } from 'vue'


const props = defineProps<{ visible: boolean; title?: string; confirmLabel?: string }>()
const emit = defineEmits<{ close: []; confirm: [fldid: number] }>()


const categoriesStore = useCategoriesStore()
const i18n = useI18nStore()
const selected = ref<number | null>(null)
const filterQuery = ref('')
const recentFolders = ref<{ id: number; name: string }[]>([])
const searchEl = ref<HTMLInputElement | null>(null)

const RECENT_KEY = 'doc-link-recent-folders'


const filteredFolders = computed(() =>
{
  const q = filterQuery.value.trim().replace(/^\//, '')
  if (!q) return(categoriesStore.categories)
  const lower = q.toLowerCase()
  return(categoriesStore.categories.filter(f => f.name.toLowerCase().includes(lower)))
})


function loadRecent()
{
  try
  {
    const raw = localStorage.getItem(RECENT_KEY)
    recentFolders.value = raw ? JSON.parse(raw) : []
  }
  catch
  {
    recentFolders.value = []
  }
}


function saveRecent(folder: { id: number; name: string })
{
  const list = recentFolders.value.filter(f => f.id !== folder.id)
  list.unshift(folder)
  recentFolders.value = list.slice(0, 3)
  localStorage.setItem(RECENT_KEY, JSON.stringify(recentFolders.value))
}


function pickFirst()
{
  if (filteredFolders.value.length > 0)
    selected.value = filteredFolders.value[0].id
}


function confirm()
{
  if (!selected.value) return
  const folder = categoriesStore.categories.find(f => f.id === selected.value)
    ?? recentFolders.value.find(f => f.id === selected.value)
  if (folder) saveRecent({ id: folder.id, name: folder.name })
  emit('confirm', selected.value)
}


function onKeydown(e: KeyboardEvent)
{
  if (e.key === 'Escape') emit('close')
}


watch(() => props.visible, async val =>
{
  if (val)
  {
    selected.value = null
    filterQuery.value = ''
    loadRecent()
    categoriesStore.load()
    await nextTick()
    searchEl.value?.focus()
  }
})


onMounted(() =>
{
  loadRecent()
  window.addEventListener('keydown', onKeydown)
})
onUnmounted(() => window.removeEventListener('keydown', onKeydown))
</script>

<style scoped>
.link-modal {
  width: min(420px, 92vw);
  height: min(520px, 80vh);
}

.link-search-row {
  padding: 8px 12px;
  border-bottom: 1px solid var(--border);
  flex-shrink: 0;
}

.link-search-input {
  width: 100%;
  height: 34px;
  padding: 0 10px;
  border: 1px solid var(--border-input);
  border-radius: 5px;
  background: var(--bg-muted);
  color: var(--text);
  font-size: 13px;
  font-family: inherit;
  outline: none;
  box-sizing: border-box;
}
.link-search-input:focus { border-color: var(--accent); }
.link-search-input::placeholder { color: var(--text-faint); }

.link-section-label {
  padding: 8px 14px 4px;
  font-size: 10px;
  font-weight: 600;
  color: var(--text-muted);
  text-transform: uppercase;
  letter-spacing: 0.5px;
  flex-shrink: 0;
}

.chip-row {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  padding: 0 14px 8px;
  flex-shrink: 0;
}

.folder-chip {
  padding: 3px 10px;
  border: 1px solid var(--border);
  border-radius: 4px;
  background: var(--bg-muted);
  color: var(--text);
  font-size: 12px;
  font-family: inherit;
  cursor: pointer;
}
.folder-chip:hover { border-color: var(--accent); }
.folder-chip.active { border-color: var(--accent); background: var(--accent-ring); color: var(--accent); }

.folder-list {
  flex: 1;
  overflow-y: auto;
  min-height: 0;
  border-top: 1px solid var(--border);
}

.folder-list-item {
  display: block;
  width: 100%;
  padding: 8px 14px;
  text-align: left;
  border: none;
  border-bottom: 1px solid var(--bg-subtle);
  background: none;
  color: var(--text);
  font-size: 13px;
  font-family: inherit;
  cursor: pointer;
}
.folder-list-item:last-child { border-bottom: none; }
.folder-list-item:hover { background: var(--bg-subtle); }
.folder-list-item.active { background: var(--accent-ring); color: var(--accent); font-weight: 500; }

.folder-list-empty {
  padding: 16px 14px;
  color: var(--text-muted);
  font-size: 13px;
}
</style>
