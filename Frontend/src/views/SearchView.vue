<template>
  <div class="search-view" @contextmenu.prevent="showAreaCtx">

    <div class="toolbar">
      <span class="section-label">{{ i18n.t('common.folder') }}</span>
      <select v-model="selectedCategory" class="select folder-select">
        <option value="0">{{ i18n.t('common.all') }}</option>
        <option v-for="cat in categoriesStore.categories" :key="cat.id" :value="cat.id">{{ cat.name }}</option>
      </select>

      <form @submit.prevent="doSearch" class="search-form">
        <div class="input-wrap">
          <input
            ref="searchInputEl"
            v-model="query"
            type="text"
            :placeholder="i18n.t('search.placeholder')"
            class="search-input"
          />
          <button v-if="query" type="button" class="input-clear" :title="i18n.t('common.clearInput')" @click="clearAll">✕</button>
        </div>
        <button type="button" class="btn btn-icon mic-btn" :class="{ 'mic-recording': recorder.recording.value }" :title="i18n.t('search.micTitle')" :disabled="transcribing" @click="toggleMic">
          <span v-if="transcribing" class="spinner spinner-sm"></span>
          <svg v-else width="14" height="14" viewBox="0 0 20 20" fill="currentColor">
            <path d="M10 12a3 3 0 003-3V5a3 3 0 10-6 0v4a3 3 0 003 3zM5 9a1 1 0 10-2 0 7 7 0 006 6.93V18H7a1 1 0 100 2h6a1 1 0 100-2h-2v-2.07A7 7 0 0017 9a1 1 0 10-2 0 5 5 0 01-10 0z"/>
          </svg>
        </button>
        <button type="submit" class="btn btn-primary btn-sm">
          {{ loading ? i18n.t('search.searching') : i18n.t('search.search') }}
        </button>
        <button type="button" @click="showAdvanced = !showAdvanced" class="btn btn-primary btn-sm adv-btn">
          <svg width="12" height="12" viewBox="0 0 20 20" fill="currentColor">
            <path fill-rule="evenodd" d="M11.49 3.17c-.38-1.56-2.6-1.56-2.98 0a1.532 1.532 0 01-2.286.948c-1.372-.836-2.942.734-2.106 2.106.54.886.061 2.042-.947 2.287-1.561.379-1.561 2.6 0 2.978a1.532 1.532 0 01.947 2.287c-.836 1.372.734 2.942 2.106 2.106a1.532 1.532 0 012.287.947c.379 1.561 2.6 1.561 2.978 0a1.533 1.533 0 012.287-.947c1.372.836 2.942-.734 2.106-2.106a1.533 1.533 0 01.947-2.287c1.561-.379 1.561-2.6 0-2.978a1.532 1.532 0 01-.947-2.287c.836-1.372-.734-2.942-2.106-2.106a1.532 1.532 0 01-2.287-.947zM10 13a3 3 0 100-6 3 3 0 000 6z" clip-rule="evenodd"/>
          </svg>
          {{ i18n.t('search.advanced') }}
          <svg width="10" height="10" viewBox="0 0 20 20" fill="currentColor"
            :style="{ transform: showAdvanced ? 'rotate(180deg)' : '' }"
            style="transition:transform 0.15s">
            <path fill-rule="evenodd" d="M5.293 7.293a1 1 0 011.414 0L10 10.586l3.293-3.293a1 1 0 111.414 1.414l-4 4a1 1 0 01-1.414 0l-4-4a1 1 0 010-1.414z" clip-rule="evenodd"/>
          </svg>
        </button>
      </form>

      <button type="button" class="btn btn-primary btn-sm" @click="clearAll">{{ i18n.t('common.clearInput') }}</button>

      <div class="spacer"></div>

    </div>

    <form v-show="showAdvanced" @submit.prevent="applyAdvanced" class="advanced-row">
      <div class="adv-field">
        <label class="adv-label">{{ i18n.t('search.allWords') }}</label>
        <input v-model="advAll" type="text" class="adv-input" :placeholder="i18n.t('search.allWordsPlaceholder')" />
      </div>
      <div class="adv-field">
        <label class="adv-label">{{ i18n.t('search.anyWords') }}</label>
        <input v-model="advAny" type="text" class="adv-input" :placeholder="i18n.t('search.anyWordsPlaceholder')" />
      </div>
      <div class="adv-field">
        <label class="adv-label">{{ i18n.t('search.excludeWords') }}</label>
        <input v-model="advExclude" type="text" class="adv-input" :placeholder="i18n.t('search.excludeWordsPlaceholder')" />
      </div>
      <button type="submit" class="btn btn-primary btn-sm">{{ i18n.t('search.apply') }}</button>
    </form>

    <div class="results" @keydown="onResultsKeydown">
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
          :focused="focusedDocId === doc.id"
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

    <LinkFolderModal :visible="showLinkModal" :title="pickerMode === 'move' ? i18n.t('explorer.moveToFolder') : i18n.t('explorer.linkToFolder')" :confirm-label="pickerMode === 'move' ? i18n.t('linkFolderModal.move') : i18n.t('linkFolderModal.link')" @close="showLinkModal = false" @confirm="onFolderPickerConfirm" />

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
import { useAuthStore } from '../stores/auth'
import { useI18nStore } from '../stores/i18n'
import { synthesizeSpeech } from '../api/tts'
import type { DocumentResult } from '../types'
import { transcribeAudio } from '../api/transcribe'
import { useConfirmStore } from '../stores/confirm'
import { useSettingsStore } from '../stores/settings'
import { useCategoriesStore } from '../stores/categories'
import DocumentCard from '../components/DocumentCard.vue'
import { useEditRequestStore } from '../stores/editRequest'
import LinkFolderModal from '../components/LinkFolderModal.vue'
import { useAudioRecorder } from '../composables/useAudioRecorder'
import { type AxiosError } from 'axios'
import { ref, computed, watch, nextTick, onMounted, onUnmounted } from 'vue'
import { search, linkDocuments, moveDocument, deleteDocument, extractSearchTerms } from '../api/documents'


const categoriesStore = useCategoriesStore()
const auth = useAuthStore()
const editRequestStore = useEditRequestStore()
const i18n = useI18nStore()
const confirm = useConfirmStore()
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
const loading = ref(false)
const error = ref<string | null>(null)
const searched = ref(false)
const recorder = useAudioRecorder()
const transcribing = ref(false)
const settings = useSettingsStore()
const showAdvanced = ref(false)
const advAll = ref('')
const advAny = ref('')
const advExclude = ref('')


const sortedDocuments = computed(() =>
{
  const list = [...documents.value]
  if (sortMode.value === 'date')
    return(list.sort((a, b) => (b.date ?? '').localeCompare(a.date ?? '')))
  return(list.sort((a, b) => (a.title ?? '').localeCompare(b.title ?? '', undefined, { sensitivity: 'base' })))
})


function onVisibilityChange()
{
  if (document.visibilityState === 'visible' && error.value && lastQuery.value)
    doSearch()
}


onMounted(() =>
{
  categoriesStore.load()
  window.addEventListener('close-all-ctx', onCloseAllCtx)
  document.addEventListener('visibilitychange', onVisibilityChange)
})


onUnmounted(() =>
{
  window.removeEventListener('close-all-ctx', onCloseAllCtx)
  document.removeEventListener('visibilitychange', onVisibilityChange)
})


defineExpose({ focus: () => searchInputEl.value?.focus(), toggleMic })


// ── Results keyboard navigation ────────────────────────────────────
const focusedDocId = ref<number | null>(null)


watch(sortedDocuments, list =>
{
  if (!list.length) { focusedDocId.value = null; return }
  if (focusedDocId.value === null || !list.some(d => d.id === focusedDocId.value)) focusedDocId.value = list[0].id
})


function focusDocRow(id: number)
{
  nextTick(() => document.querySelector<HTMLElement>(`.results [data-doc-id="${id}"]`)?.focus())
}


function onResultsKeydown(e: KeyboardEvent)
{
  const list = sortedDocuments.value
  if (!list.length) return
  const idx = list.findIndex(d => d.id === focusedDocId.value)

  if (e.key === 'ArrowDown')
  {
    e.preventDefault()
    const next = list[Math.min(idx + 1, list.length - 1)]
    focusedDocId.value = next.id
    focusDocRow(next.id)
  }
  else if (e.key === 'ArrowUp')
  {
    e.preventDefault()
    const prev = list[Math.max(idx - 1, 0)]
    focusedDocId.value = prev.id
    focusDocRow(prev.id)
  }
}


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
  if (!await confirm.ask({ message: i18n.t('explorer.deleteSingleConfirm', { label: `"${doc?.title ?? id}"` }), confirmLabel: i18n.t('common.delete'), danger: true })) return
  try
  {
    await deleteDocument(id)
    documents.value = documents.value.filter(d => d.id !== id)
    selectedIds.value = new Set([...selectedIds.value].filter(x => x !== id))
  }
  catch
  {
    /* ignore */
  }
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
  catch
  {
    /* ignore */
  }
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
  catch
  {
    /* ignore */
  }
}


let speechAudio: HTMLAudioElement | null = null
let speechGeneration = 0


function stopSpeech()
{
  speechGeneration++
  if (speechAudio) { speechAudio.pause(); speechAudio = null }
}


async function speakText(text: string)
{
  stopSpeech()
  const generation = speechGeneration
  try
  {
    const res = await synthesizeSpeech(text)
    if (generation !== speechGeneration) return
    const url = URL.createObjectURL(res.data as Blob)
    speechAudio = new Audio(url)
    speechAudio.onended = () => URL.revokeObjectURL(url)
    speechAudio.play()
  }
  catch { /* ignore */ }
}


async function toggleMic()
{
  if (recorder.recording.value)
  {
    recorder.stop()
    return
  }
  stopSpeech()
  try
  {
    query.value = ''
    const blob = await recorder.start()
    transcribing.value = true
    const res = await transcribeAudio(blob)
    const transcript = (res.data.text || '').trim()
    if (transcript.split(/\s+/).filter(Boolean).length < 2) return
    const extracted = await extractSearchTerms(transcript, settings.languageIndependent)
    query.value = (extracted.data.terms || transcript).trim()
    searchInputEl.value?.focus()
    await doSearch()
    speakText(i18n.t('search.resultsFoundCount', { count: String(documents.value.length) }))
  }
  catch (err)
  {
    const e = err as AxiosError
    const body = e.response?.data as Record<string, unknown> | undefined
    const detail = body?.error ? String(body.error) : e.message
    error.value = detail || i18n.t('search.micError')
  }
  finally
  {
    transcribing.value = false
  }
}


function clearAll()
{
  query.value = ''
  advAll.value = ''
  advAny.value = ''
  advExclude.value = ''
  documents.value = []
  searched.value = false
  error.value = null
  selectedIds.value = new Set()
  lastCheckedId.value = null
  searchInputEl.value?.focus()
}


function parseQueryToAdvanced(q: string)
{
  const excluded: string[] = []
  const any: string[] = []
  const all: string[] = []

  let remaining = q.trim()

  const parenMatch = remaining.match(/\(([^)]+)\)/)
  if (parenMatch)
  {
    const inner = parenMatch[1]
    if (inner.includes('|'))
      any.push(...inner.split('|').map(s => s.trim()).filter(Boolean))
    remaining = remaining.replace(parenMatch[0], ' ')
  }

  for (const token of remaining.split(/\s+/).filter(Boolean))
  {
    if (token.startsWith('!')) excluded.push(token.slice(1))
    else all.push(token)
  }

  advAll.value = all.join(' ')
  advAny.value = any.join(' ')
  advExclude.value = excluded.join(' ')
}


watch(query, parseQueryToAdvanced)


function applyAdvanced()
{
  const parts: string[] = []

  const all = advAll.value.replace(/,/g, ' ').trim().split(/\s+/).filter(Boolean).join(' ')
  if (all) parts.push(all)

  const any = advAny.value.replace(/,/g, ' ').trim().split(/\s+/).filter(Boolean)
  if (any.length === 1) parts.push(any[0])
  else if (any.length > 1) parts.push('(' + any.join(' | ') + ')')

  const exclude = advExclude.value.replace(/,/g, ' ').trim().split(/\s+/).filter(Boolean)
  for (const w of exclude) parts.push('!' + w)

  query.value = parts.join(' ')
  doSearch()
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
    const res = await search({ query: query.value, folder: selectedCategory.value, languageIndependent: settings.languageIndependent })
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

.spacer { flex: 1; }

.search-form {
  display: flex;
  align-items: center;
  gap: 8px;
}

.input-wrap { position: relative; display: flex; align-items: center; }

.search-input {
  width: 40ch;
  height: 32px;
  padding: 0 28px 0 12px;
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

.input-clear {
  position: absolute;
  right: 4px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 20px;
  height: 20px;
  padding: 0;
  background: transparent;
  border: none;
  border-radius: 50%;
  color: var(--text-faint);
  cursor: pointer;
  font-size: 12px;
  line-height: 1;
}
.input-clear:hover { background: var(--bg-muted); color: var(--text); }

.mic-btn.mic-recording { color: var(--danger); }

.adv-btn { gap: 4px; }

.advanced-row {
  display: flex;
  align-items: flex-end;
  gap: 10px;
  padding: 10px 14px;
  background: var(--tab-bar-bg);
  border-bottom: 1px solid var(--border);
  flex-shrink: 0;
}

.adv-field {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.adv-label {
  font-size: 11px;
  font-weight: 600;
  color: var(--text-muted);
}

.adv-input {
  width: 22ch;
  height: 30px;
  padding: 0 10px;
  border: 1.5px solid var(--border-input);
  border-radius: 6px;
  background: var(--bg);
  color: var(--text);
  font-size: 13px;
  font-family: inherit;
  outline: none;
  transition: border-color 0.15s, box-shadow 0.15s;
}
.adv-input:focus {
  border-color: var(--accent);
  box-shadow: 0 0 0 3px var(--accent-ring);
}
.adv-input::placeholder { color: var(--text-faint); }

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
