<template>
  <div class="doc-row" :class="{ 'doc-row-checked': checked }" @click="onRowClick" @dblclick="openContent" @contextmenu.prevent.stop="showCtxMenu">

    <svg v-if="!doc.hasFile && !doc.isLink" class="doc-icon" viewBox="0 0 16 16" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
      <path d="M3 2h6.5L13 5.5V14H3V2z"/>
      <polyline points="9.5 2 9.5 5.5 13 5.5"/>
      <line x1="5" y1="8" x2="11" y2="8"/>
      <line x1="5" y1="10" x2="11" y2="10"/>
      <line x1="5" y1="12" x2="9" y2="12"/>
    </svg>
    <svg v-else-if="doc.isLink" class="doc-icon doc-icon-link" viewBox="0 0 16 16" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round">
      <path d="M7 4H5a4 4 0 0 0 0 8h2"/>
      <path d="M9 12h2a4 4 0 0 0 0-8H9"/>
      <line x1="6" y1="8" x2="10" y2="8"/>
    </svg>
    <svg v-else class="doc-icon" viewBox="0 0 16 16" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
      <path d="M3 2h6.5L13 5.5V14H3V2z"/>
      <polyline points="9.5 2 9.5 5.5 13 5.5"/>
    </svg>

    <span class="doc-date">{{ formatDate(doc.date) }}</span>
    <div class="doc-main">
      <span class="doc-title">{{ doc.title || i18n.t('documentCard.untitled') }}</span><span v-if="doc.description" class="doc-desc"> — {{ doc.description }}</span>
    </div>

    <Teleport to="body">
      <div v-if="showText" class="modal-backdrop" @click.self="showText = false">
        <div class="modal-popup">
          <div class="modal-header">
            <span class="modal-header-title">{{ doc.title }}</span>
            <button type="button" class="modal-close" @click="showText = false">✕</button>
          </div>
          <div class="modal-body">
            <p class="modal-text">{{ doc.description }}</p>
          </div>
        </div>
      </div>
    </Teleport>

    <Teleport to="body">
      <div v-if="ctxMenu" class="ctx-menu" :style="{ top: ctxMenu.y + 'px', left: ctxMenu.x + 'px' }" @click.stop>
        <button v-if="doc.description" class="ctx-item" @click="ctxText">{{ i18n.t('chat.showText') }}</button>
        <button v-if="doc.hasFile" class="ctx-item" @click="ctxShowFile">{{ i18n.t('chat.showFile') }}</button>
        <div v-if="(doc.description || doc.hasFile) && canEdit" class="ctx-divider"></div>
        <button v-if="canEdit" class="ctx-item" @click="ctxEdit">{{ i18n.t('chat.editDocument') }}</button>
        <button v-if="canLink" class="ctx-item" @click="ctxLink">{{ selectedCount && selectedCount > 1 ? i18n.t('documentCard.linkDocuments') : i18n.t('documentCard.linkDocument') }}</button>
        <button v-if="canLink && !isLink" class="ctx-item" @click="ctxMove">{{ selectedCount && selectedCount > 1 ? i18n.t('documentCard.moveDocuments') : i18n.t('documentCard.moveDocument') }}</button>
        <div v-if="canEdit && canCreate" class="ctx-divider"></div>
        <button v-if="canEdit && canCreate" class="ctx-item" @click="ctxNewFolder">{{ i18n.t('edit.newFolder') }}</button>
        <button v-if="canEdit && canCreate" class="ctx-item" @click="ctxNewDoc">{{ i18n.t('create.newDocument') }}</button>
        <div v-if="canEdit" class="ctx-divider"></div>
        <button v-if="canEdit" class="ctx-item" @click="ctxSelectAll">{{ (selectedCount ?? 0) > 0 ? i18n.t('documentCard.deselectAll') : i18n.t('documentCard.selectAll') }}</button>
        <div class="ctx-divider"></div>
        <button class="ctx-item" :class="{ 'ctx-item-active': activeSort === 'title' }" @click="ctxSort('title')">{{ i18n.t('search.sortByTitle') }}</button>
        <button class="ctx-item" :class="{ 'ctx-item-active': activeSort === 'date' }" @click="ctxSort('date')">{{ i18n.t('search.sortByDate') }}</button>
        <div v-if="canEdit" class="ctx-divider"></div>
        <button v-if="canEdit && selectedCount && selectedCount > 1" class="ctx-item ctx-item-danger" @click="ctxDelete">{{ i18n.t('documentCard.deleteDocumentsCount', { count: String(selectedCount) }) }}</button>
        <button v-else-if="canEdit && isLink && linkId" class="ctx-item ctx-item-danger" @click="ctxRemoveLink">{{ i18n.t('documentCard.removeLink') }}</button>
        <button v-else-if="canEdit" class="ctx-item ctx-item-danger" @click="ctxDelete">{{ i18n.t('documentCard.deleteDocument') }}</button>
      </div>
    </Teleport>
  </div>
</template>

<script setup lang="ts">
import { useI18nStore } from '../stores/i18n'
import { openOrDownload } from '../utils/file'
import type { DocumentResult } from '../types'
import { ref, onMounted, onUnmounted } from 'vue'


const props = defineProps<{ doc: DocumentResult; canEdit?: boolean; canLink?: boolean; canCreate?: boolean; checked?: boolean; selectedCount?: number; allSelected?: boolean; activeSort?: 'title' | 'date'; isLink?: boolean; linkId?: number | null }>()
const emit = defineEmits<{ edit: [id: number]; delete: [id: number]; move: [id: number]; 'new-doc': []; 'new-folder': []; sort: ['title' | 'date']; check: [id: number, shift: boolean, ctrl: boolean]; link: [id: number]; 'select-all': []; 'remove-link': [linkId: number] }>()


const i18n = useI18nStore()

const showText = ref(false)
const ctxMenu = ref<{ x: number; y: number } | null>(null)
let selfOpening = false
const onCloseAllCtx = () => { if (!selfOpening) ctxMenu.value = null }


function onRowClick(e: MouseEvent)
{
  emit('check', props.doc.id, e.shiftKey, e.ctrlKey || e.metaKey)
}


function openContent()
{
  if (props.doc.hasFile) openOrDownload(props.doc.id, props.doc.filename, i18n.t('chat.cannotDisplayFile'))
  else if (props.doc.description) showText.value = true
}


function showCtxMenu(e: MouseEvent)
{
  selfOpening = true
  window.dispatchEvent(new Event('close-all-ctx'))
  selfOpening = false
  const x = Math.min(e.clientX, window.innerWidth - 210)
  const y = Math.min(e.clientY, window.innerHeight - 380)
  ctxMenu.value = { x: Math.max(4, x), y: Math.max(4, y) }
  window.addEventListener('click', () => { ctxMenu.value = null }, { once: true })
}


function ctxText()
{
  ctxMenu.value = null
  showText.value = true
}


function ctxShowFile()
{
  ctxMenu.value = null
  openOrDownload(props.doc.id, props.doc.filename, i18n.t('chat.cannotDisplayFile'))
}


function ctxEdit()
{
  ctxMenu.value = null
  emit('edit', props.doc.id)
}


function ctxNewDoc()
{
  ctxMenu.value = null
  emit('new-doc')
}


function ctxNewFolder()
{
  ctxMenu.value = null
  emit('new-folder')
}


function ctxLink()
{
  ctxMenu.value = null
  emit('link', props.doc.id)
}


function ctxDelete()
{
  ctxMenu.value = null
  emit('delete', props.doc.id)
}


function ctxMove()
{
  ctxMenu.value = null
  emit('move', props.doc.id)
}


function ctxSelectAll()
{
  ctxMenu.value = null
  emit('select-all')
}


function ctxRemoveLink()
{
  ctxMenu.value = null
  if (props.linkId) emit('remove-link', props.linkId)
}


function ctxSort(mode: 'title' | 'date')
{
  ctxMenu.value = null
  emit('sort', mode)
}


function onKeydown(e: KeyboardEvent)
{
  if (e.key === 'Escape')
  {
    if (ctxMenu.value) { ctxMenu.value = null; return }
    if (showText.value) showText.value = false
  }
}


onMounted(() =>
{
  window.addEventListener('keydown', onKeydown)
  window.addEventListener('close-all-ctx', onCloseAllCtx)
})
onUnmounted(() =>
{
  window.removeEventListener('keydown', onKeydown)
  window.removeEventListener('close-all-ctx', onCloseAllCtx)
})


function formatDate(d: string): string
{
  if (!d) return('')
  try
  {
    return(new Date(d).toLocaleDateString('en-CA'))
  }
  catch
  {
    return(d)
  }
}
</script>

<style scoped>
.doc-row {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 9px 14px;
  border-bottom: 1px solid var(--bg-subtle);
  font-size: 12px;
  cursor: pointer;
  user-select: none;
  position: relative;
}
.doc-row:hover { background: var(--select-bg); }
.doc-row-checked {
  background: var(--select-bg);
  box-shadow: inset 0 0 0 2px var(--select-border);
  z-index: 1;
}
.doc-row-checked:hover { background: var(--select-bg); }
.doc-icon { width: 16px; height: 16px; flex-shrink: 0; color: var(--text); opacity: 0.55; }
.doc-icon-link { color: var(--accent); opacity: 1; }

.doc-date {
  color: var(--text);
  width: 88px;
  flex-shrink: 0;
}
.doc-main {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.doc-title { color: var(--text); }
.doc-desc  { color: var(--text); }

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
  text-decoration: none;
}
.ctx-item:hover { background: var(--bg-subtle); }
.ctx-item-active { font-weight: 600; color: var(--accent); }
.ctx-item-danger { color: var(--danger); }
.ctx-item-danger:hover { background: var(--danger-bg); }
.ctx-divider { height: 1px; background: var(--border); margin: 3px 0; }
</style>
