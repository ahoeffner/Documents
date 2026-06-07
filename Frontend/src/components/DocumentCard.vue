<template>
  <div class="doc-row" :class="{ 'doc-row-checked': checked }" @dblclick="openContent" @contextmenu.prevent.stop="showCtxMenu">
    <input v-if="selectable" type="checkbox" class="doc-check" :checked="checked" @click.stop @change="$emit('check', doc.id)" />
    <span class="doc-date">{{ formatDate(doc.date) }}</span>
    <div class="doc-main">
      <span class="doc-title">{{ doc.title || 'Untitled' }}</span><span v-if="doc.description" class="doc-desc"> — {{ doc.description }}</span>
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
        <button v-if="doc.description" class="ctx-item" @click="ctxText">Show Text</button>
        <a v-if="doc.hasFile" :href="`/api/content/${doc.id}/file`" target="_blank" class="ctx-item" @click="ctxMenu = null">Show File</a>
        <div v-if="(doc.description || doc.hasFile) && canEdit" class="ctx-divider"></div>
        <button v-if="canEdit" class="ctx-item" @click="ctxEdit">Edit Document</button>
        <button v-if="canLink" class="ctx-item" @click="ctxLink">{{ selectedCount && selectedCount > 1 ? 'Link Documents' : 'Link Document' }}</button>
        <button v-if="canLink && !isLink" class="ctx-item" @click="ctxMove">{{ selectedCount && selectedCount > 1 ? 'Move Documents' : 'Move Document' }}</button>
        <div v-if="canEdit && canCreate !== false" class="ctx-divider"></div>
        <button v-if="canEdit && canCreate !== false" class="ctx-item" @click="ctxNewFolder">New Folder</button>
        <button v-if="canEdit && canCreate !== false" class="ctx-item" @click="ctxNewDoc">New Document</button>
        <div v-if="selectable" class="ctx-divider"></div>
        <button v-if="selectable" class="ctx-item" @click="ctxSelectAll">{{ allSelected ? 'Deselect All' : 'Select All' }}</button>
        <div class="ctx-divider"></div>
        <button class="ctx-item" :class="{ 'ctx-item-active': activeSort === 'title' }" @click="ctxSort('title')">Sort by Title</button>
        <button class="ctx-item" :class="{ 'ctx-item-active': activeSort === 'date' }" @click="ctxSort('date')">Sort by Date</button>
        <div v-if="canEdit" class="ctx-divider"></div>
        <button v-if="canEdit && !isLink" class="ctx-item ctx-item-danger" @click="ctxDelete">
          {{ selectedCount && selectedCount > 1 ? `Delete Documents (${selectedCount})` : 'Delete Document' }}
        </button>
        <button v-if="isLink && linkId" class="ctx-item ctx-item-danger" @click="ctxRemoveLink">Remove Link</button>
      </div>
    </Teleport>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, onMounted, onUnmounted } from 'vue'
import type { DocumentResult } from '../types'


const props = defineProps<{ doc: DocumentResult; canEdit?: boolean; canLink?: boolean; canCreate?: boolean; selectable?: boolean; checked?: boolean; selectedCount?: number; allSelected?: boolean; activeSort?: 'title' | 'date'; isLink?: boolean; linkId?: number | null }>()
const emit = defineEmits<{ edit: [id: number]; delete: [id: number]; move: [id: number]; 'new-doc': []; 'new-folder': []; sort: ['title' | 'date']; check: [id: number]; link: [id: number]; 'select-all': []; 'remove-link': [linkId: number] }>()

const showText = ref(false)
const ctxMenu = ref<{ x: number; y: number } | null>(null)


function openContent()
{
  if (props.doc.hasFile) window.open(`/api/content/${props.doc.id}/file`, '_blank')
  else if (props.doc.description) showText.value = true
}


function showCtxMenu(e: MouseEvent)
{
  ctxMenu.value = { x: e.clientX, y: e.clientY }
}


function ctxText()
{
  ctxMenu.value = null
  showText.value = true
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


watch(ctxMenu, val =>
{
  if (val)
  {
    const close = () => { ctxMenu.value = null }
    window.addEventListener('click', close, { once: true })
  }
})


function onKeydown(e: KeyboardEvent)
{
  if (e.key === 'Escape')
  {
    if (ctxMenu.value) { ctxMenu.value = null; return }
    if (showText.value) showText.value = false
  }
}

onMounted(() => window.addEventListener('keydown', onKeydown))
onUnmounted(() => window.removeEventListener('keydown', onKeydown))


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
  padding: 7px 14px;
  border-bottom: 1px solid var(--bg-subtle);
  font-size: 12px;
  cursor: pointer;
  user-select: none;
}
.doc-row:hover { background: var(--bg-subtle); }
.doc-row-checked { background: var(--accent-ring); }
.doc-row-checked:hover { background: var(--accent-ring); }
.doc-check { flex-shrink: 0; cursor: pointer; accent-color: var(--accent); }

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
