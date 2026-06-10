<template>
  <div class="search-view" @contextmenu.prevent="showAreaCtx">

    <div class="toolbar">
      <span class="section-label">{{ i18n.t('common.folder') }}</span>
      <select v-model="selectedCategory" class="select folder-select">
        <option value="0">{{ i18n.t('common.all') }}</option>
        <option v-for="cat in categoriesStore.categories" :key="cat.id" :value="cat.id">{{ cat.name }}</option>
      </select>

      <form @submit.prevent="doSearch" class="search-form">
        <input
          ref="searchInputEl"
          v-model="query"
          type="text"
          :placeholder="i18n.t('search.placeholder')"
          class="search-input"
        />
        <button type="submit" class="btn btn-primary btn-sm">
          {{ loading ? i18n.t('search.searching') : i18n.t('search.search') }}
        </button>
      </form>

    </div>

    <div class="results">
      <div v-if="error" class="notice notice-error" style="margin:12px">{{ error }}</div>

      <div v-else-if="loading" class="empty-state">
        <span class="spinner spinner-lg"></span>
      </div>

      <div v-else-if="!searched" class="empty-state">{{ i18n.t('search.enterQuery') }}</div>

      <div v-else-if="documents.length === 0" class="empty-state">
        {{ i18n.t('search.noMatch', { query: lastQuery }) }}
      </div>

      <template v-else>
        <DocumentCard
          v-for="doc in sortedDocuments"
          :key="doc.id"
          :doc="doc"
          :can-edit="auth.isAdmin"
          :can-link="auth.isAdmin"
          :can-create="false"
          :checked="selectedIds.has(doc.id)"
          :selected-count="selectedIds.size"
          :all-selected="selectedIds.size > 0"
          :active-sort="sortMode"
          @edit="editRequestStore.request($event)"
          @sort="sortMode = $event"
          @check="toggleCheck"
          @select-all="toggleAll"
          @link="onLink"
          @move="onMove"
          @delete="deleteSingle"
        />
      </template>
    </div>

    <LinkFolderModal :visible="showLinkModal" :title="pickerMode === 'move' ? i18n.t('explorer.moveToFolder') : i18n.t('explorer.linkToFolder')" @close="showLinkModal = false" @confirm="onFolderPickerConfirm" />

    <Teleport to="body">
      <div v-if="areaCtx" class="ctx-menu" :style="{ top: areaCtx.y + 'px', left: areaCtx.x + 'px' }" @click.stop>
        <button class="ctx-item" :class="{ 'ctx-item-active': sortMode === 'title' }" @click="areaCtxSort('title')">{{ i18n.t('search.sortByTitle') }}</button>
        <button class="ctx-item" :class="{ 'ctx-item-active': sortMode === 'date' }" @click="areaCtxSort('date')">{{ i18n.t('search.sortByDate') }}</button>
      </div>
    </Teleport>

    <Teleport to="body">
      <div v-if="showDeleteConfirm" class="modal-backdrop" @click.self="showDeleteConfirm = false">
        <div class="modal-popup modal-popup-sm">
          <div class="modal-header">
            <span class="modal-header-title">{{ i18n.t('search.deleteCountTitle', { count: String(selectedIds.size) }) }}</span>
            <button type="button" class="modal-close" @click="showDeleteConfirm = false">✕</button>
          </div>
          <div class="modal-body">
            <p class="modal-text">{{ i18n.t('search.deleteCountBody', { count: String(selectedIds.size) }) }}</p>
          </div>
          <div class="modal-actions">
            <button class="btn btn-ghost btn-sm" @click="showDeleteConfirm = false">{{ i18n.t('common.cancel') }}</button>
            <button class="btn btn-danger btn-sm" :disabled="deleteLoading" @click="confirmBulkDelete">
              {{ deleteLoading ? i18n.t('search.deleting') : i18n.t('common.delete') }}
            </button>
          </div>
        </div>
      </div>
    </Teleport>

  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import type { DocumentResult } from '../types'
import { search, linkDocuments, moveDocument, deleteDocument } from '../api/documents'
import { useCategoriesStore } from '../stores/categories'
import { useAuthStore } from '../stores/auth'
import { useEditRequestStore } from '../stores/editRequest'
import { useI18nStore } from '../stores/i18n'
import DocumentCard from '../components/DocumentCard.vue'
import LinkFolderModal from '../components/LinkFolderModal.vue'


const categoriesStore = useCategoriesStore()
const auth = useAuthStore()
const editRequestStore = useEditRequestStore()
const i18n = useI18nStore()
const searchInputEl = ref<HTMLInputElement | null>(null)
const query = ref('')
const lastQuery = ref('')
const selectedCategory = ref(0)
const documents = ref<DocumentResult[]>([])
const sortMode = ref<'title' | 'date'>('title')
const selectedIds = ref(new Set<number>())
const lastCheckedId = ref<number | null>(null)
const showLinkModal = ref(false)
const pickerMode = ref<'link' | 'move'>('link')
const linkTargetIds = ref<(number | string)[]>([])
const showDeleteConfirm = ref(false)
const deleteLoading = ref(false)
const areaCtx = ref<{ x: number; y: number } | null>(null)
let selfOpeningCtx = false
const onCloseAllCtx = () => { if (!selfOpeningCtx) areaCtx.value = null }

const sortedDocuments = computed(() =>
{
  const list = [...documents.value]
  if (sortMode.value === 'date')
    return(list.sort((a, b) => (b.date ?? '').localeCompare(a.date ?? '')))
  return(list.sort((a, b) => (a.title ?? '').localeCompare(b.title ?? '', undefined, { sensitivity: 'base' })))
})

const loading = ref(false)
const error = ref<string | null>(null)
const searched = ref(false)
onMounted(() =>
{
  categoriesStore.load()
  window.addEventListener('close-all-ctx', onCloseAllCtx)
})
onUnmounted(() => window.removeEventListener('close-all-ctx', onCloseAllCtx))

defineExpose({ focus: () => searchInputEl.value?.focus() })


function toggleCheck(id: number, shift: boolean, ctrl: boolean)
{
  if (shift && lastCheckedId.value !== null)
  {
    const list = sortedDocuments.value
    const from = list.findIndex(d => d.id === lastCheckedId.value)
    const to = list.findIndex(d => d.id === id)
    if (from !== -1 && to !== -1)
    {
      const [lo, hi] = from < to ? [from, to] : [to, from]
      selectedIds.value = new Set(list.slice(lo, hi + 1).map(d => d.id))
      return
    }
  }
  if (ctrl)
  {
    const next = new Set(selectedIds.value)
    if (next.has(id)) next.delete(id)
    else next.add(id)
    selectedIds.value = next
  }
  else
  {
    selectedIds.value = new Set([id])
  }
  lastCheckedId.value = id
}


function toggleAll()
{
  if (selectedIds.value.size > 0)
    selectedIds.value = new Set()
  else
    selectedIds.value = new Set(sortedDocuments.value.map(d => d.id))
}


function onLink(docId: string | number)
{
  pickerMode.value = 'link'
  linkTargetIds.value = selectedIds.value.size > 0 ? [...selectedIds.value] : [docId]
  showLinkModal.value = true
}


function onMove(docId: string | number)
{
  pickerMode.value = 'move'
  linkTargetIds.value = selectedIds.value.size > 0 ? [...selectedIds.value] : [docId]
  showLinkModal.value = true
}


function showAreaCtx(e: MouseEvent)
{
  selfOpeningCtx = true
  window.dispatchEvent(new Event('close-all-ctx'))
  selfOpeningCtx = false
  const x = Math.min(e.clientX, window.innerWidth - 210)
  const y = Math.min(e.clientY, window.innerHeight - 80)
  areaCtx.value = { x: Math.max(4, x), y: Math.max(4, y) }
  window.addEventListener('click', () => { areaCtx.value = null }, { once: true })
}


function areaCtxSort(mode: 'title' | 'date')
{
  areaCtx.value = null
  sortMode.value = mode
}




async function deleteSingle(id: number)
{
  if (selectedIds.value.size > 1)
  {
    showDeleteConfirm.value = true
    return
  }
  const doc = documents.value.find(d => d.id === id)
  if (!window.confirm(i18n.t('explorer.deleteSingleConfirm', { label: `"${doc?.title ?? id}"` }))) return
  try
  {
    await deleteDocument(id)
    documents.value = documents.value.filter(d => d.id !== id)
    selectedIds.value = new Set([...selectedIds.value].filter(x => x !== id))
  }
  catch { /* ignore */ }
}


async function confirmBulkDelete()
{
  deleteLoading.value = true
  const ids = [...selectedIds.value]
  try
  {
    await Promise.all(ids.map(id => deleteDocument(id)))
    documents.value = documents.value.filter(d => !ids.includes(d.id))
    selectedIds.value = new Set()
    showDeleteConfirm.value = false
  }
  catch { /* ignore */ }
  finally
  {
    deleteLoading.value = false
  }
}


async function onFolderPickerConfirm(fldid: number)
{
  showLinkModal.value = false
  try
  {
    if (pickerMode.value === 'link')
    {
      await linkDocuments(fldid, linkTargetIds.value)
    }
    else
    {
      await Promise.all(linkTargetIds.value.map(id => moveDocument(id, fldid)))
      documents.value = documents.value.filter(d => !linkTargetIds.value.includes(d.id))
    }
    selectedIds.value = new Set()
  }
  catch { /* ignore */ }
}


async function doSearch()
{
  if (!query.value.trim()) return
  loading.value = true
  error.value = null
  lastQuery.value = query.value
  selectedIds.value = new Set()
  lastCheckedId.value = null
  try
  {
    const res = await search({ query: query.value, folder: selectedCategory.value })
    documents.value = (res.data.documents || []) as DocumentResult[]
    searched.value = true
  }
  catch
  {
    error.value = i18n.t('search.failed')
  }
  finally
  {
    loading.value = false
  }
}
</script>

<style scoped>
.search-view {
  display: flex;
  flex-direction: column;
  flex: 1;
  overflow: hidden;
}

.toolbar {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 0 14px;
  height: 46px;
  background: var(--bg-muted);
  border-bottom: 1px solid var(--border);
  flex-shrink: 0;
}

.folder-select { width: auto; height: 32px; }

.search-form {
  display: flex;
  align-items: center;
  gap: 8px;
}

.search-input {
  width: 40ch;
  height: 32px;
  padding: 0 12px;
  border: 1.5px solid var(--border-input);
  border-radius: 6px;
  background: var(--bg);
  color: var(--text);
  font-size: 13px;
  font-family: inherit;
  outline: none;
  transition: border-color 0.15s, box-shadow 0.15s;
}
.search-input:focus {
  border-color: var(--accent);
  box-shadow: 0 0 0 3px var(--accent-ring);
}
.search-input::placeholder { color: var(--text-faint); }

.result-count {
  font-size: 12px;
  color: var(--text-muted);
  white-space: nowrap;
  font-weight: 500;
}

.results { flex: 1; overflow-y: auto; }

.ctx-menu {
  position: fixed;
  z-index: 9999;
  background: var(--bg);
  border: 1px solid var(--border);
  border-radius: 6px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.14);
  padding: 4px 0;
  min-width: 130px;
}
.ctx-item {
  display: block;
  width: 100%;
  padding: 6px 14px;
  text-align: left;
  font-size: 12.5px;
  font-family: inherit;
  color: var(--text);
  background: none;
  border: none;
  cursor: pointer;
}
.ctx-item:hover { background: var(--bg-subtle); }
.ctx-item-active { font-weight: 600; color: var(--accent); }
.ctx-divider { height: 1px; background: var(--border); margin: 3px 0; }
</style>
