<template>
  <div class="explorer-view">

    <!-- ── Sidebar ── -->
    <div class="sidebar" :style="{ width: sidebarWidth + 'px' }">
      <div class="sidebar-header">
        <span class="section-label">Folders</span>
        <button class="btn-icon" title="New folder" @click="showNewFolder = true">
          <svg width="13" height="13" viewBox="0 0 20 20" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round">
            <path d="M10 4v12M4 10h12"/>
          </svg>
        </button>
      </div>

      <!-- New folder inline form -->
      <form v-if="showNewFolder" class="new-folder-form" @submit.prevent="addFolder">
        <input
          ref="newFolderInputEl"
          v-model="newFolderName"
          type="text"
          placeholder="Folder name"
          class="input new-folder-input"
          @keydown.esc="showNewFolder = false"
        />
        <div class="new-folder-row">
          <select v-model="newFolderPid" class="select new-folder-select">
            <option :value="null">— root —</option>
            <option v-for="f in flatFolders" :key="f.id" :value="f.id">{{ f.path }}</option>
          </select>
        </div>
        <div class="new-folder-actions">
          <button type="submit" class="btn btn-primary btn-xs" :disabled="!newFolderName.trim()">Add</button>
          <button type="button" class="btn btn-ghost btn-xs" @click="showNewFolder = false">Cancel</button>
        </div>
      </form>

      <div v-if="foldersStore.loading" class="tree-empty">
        <span class="spinner spinner-md"></span>
      </div>
      <div v-else-if="foldersStore.error" class="tree-empty tree-error">{{ foldersStore.error }}</div>
      <div v-else-if="!foldersStore.tree.length" class="tree-empty">No folders yet</div>
      <div v-else class="tree-body">
        <FolderTreeItem
          v-for="folder in foldersStore.tree"
          :key="folder.id"
          :folder="folder"
          :depth="0"
          :selected-id="selectedFolderId"
          @select="selectFolder"
        />
      </div>
    </div>

    <!-- ── Resize handle ── -->
    <div class="resize-handle" @mousedown="startResize"></div>

    <!-- ── Main panel ── -->
    <div class="main-panel">
      <div class="panel-header">
        <span class="panel-title">
          {{ selectedFolder ? selectedFolder.path : 'Select a folder' }}
        </span>
      </div>

      <div v-if="!selectedFolderId" class="empty-state">
        <svg width="32" height="32" viewBox="0 0 20 20" fill="currentColor" style="color:var(--border-input)">
          <path d="M2 6a2 2 0 012-2h4l2 2h6a2 2 0 012 2v6a2 2 0 01-2 2H4a2 2 0 01-2-2V6z"/>
        </svg>
        <span>Select a folder to view its documents</span>
      </div>

      <div v-else-if="docsLoading" class="empty-state">
        <span class="spinner spinner-lg"></span>
      </div>

      <div v-else-if="docsError" class="notice notice-error" style="margin:12px">{{ docsError }}</div>

      <div v-else-if="!docs.length" class="empty-state">No documents in this folder</div>

      <div v-else class="doc-list">
        <DocumentCard v-for="doc in docs" :key="doc.id" :doc="doc" />
      </div>
    </div>

  </div>
</template>

<script setup lang="ts">
import { ref, computed, nextTick, watch } from 'vue'
import { useFoldersStore } from '../stores/folders'
import { getFolderDocuments, createFolder } from '../api/folders'
import { useResize } from '../composables/useResize'
import FolderTreeItem from '../components/FolderTreeItem.vue'
import DocumentCard from '../components/DocumentCard.vue'
import type { DocumentResult, Folder } from '../types'

const { width: sidebarWidth, startResize } = useResize(280, 140, 520)

const foldersStore = useFoldersStore()
foldersStore.load()

const selectedFolderId = ref<number | null>(null)
const docs = ref<DocumentResult[]>([])
const docsLoading = ref(false)
const docsError = ref<string | null>(null)

const showNewFolder = ref(false)
const newFolderName = ref('')
const newFolderPid = ref<number | null>(null)
const newFolderInputEl = ref<HTMLInputElement | null>(null)

watch(showNewFolder, v => { if (v) nextTick(() => newFolderInputEl.value?.focus()) })

function flattenFolders(nodes: Folder[], prefix = ''): { id: number; path: string }[] {
  const out: { id: number; path: string }[] = []
  for (const n of nodes) {
    const path = prefix ? `${prefix} / ${n.name}` : n.name
    out.push({ id: n.id, path })
    out.push(...flattenFolders(n.children, path))
  }
  return out
}

const flatFolders = computed(() => flattenFolders(foldersStore.tree))
const selectedFolder = computed(() => flatFolders.value.find(f => f.id === selectedFolderId.value) ?? null)

async function selectFolder(id: number) {
  if (selectedFolderId.value === id) return
  selectedFolderId.value = id
  docs.value = []
  docsError.value = null
  docsLoading.value = true
  try {
    const res = await getFolderDocuments(id)
    docs.value = (res.data.documents || []) as DocumentResult[]
  } catch {
    docsError.value = 'Failed to load documents.'
  } finally {
    docsLoading.value = false
  }
}

async function addFolder() {
  const name = newFolderName.value.trim()
  if (!name) return
  try {
    await createFolder(name, newFolderPid.value)
    await foldersStore.load()
    newFolderName.value = ''
    newFolderPid.value = null
    showNewFolder.value = false
  } catch { /* toast later */ }
}
</script>

<style scoped>
.explorer-view {
  display: flex;
  flex: 1;
  overflow: hidden;
}

/* ── Sidebar ── */
.sidebar {
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  background: var(--bg-subtle);
  overflow: hidden;
}

.sidebar-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 36px;
  padding: 0 10px 0 12px;
  border-bottom: 1px solid var(--border);
  flex-shrink: 0;
}

.new-folder-form {
  padding: 8px 10px;
  border-bottom: 1px solid var(--border);
  display: flex;
  flex-direction: column;
  gap: 6px;
  flex-shrink: 0;
  background: var(--bg);
}

.new-folder-input { height: 28px; font-size: 12px; }
.new-folder-row { display: flex; gap: 4px; }
.new-folder-select { height: 26px; font-size: 11px; }
.new-folder-actions { display: flex; gap: 4px; }

.tree-body { flex: 1; overflow-y: auto; padding: 3px; }

.tree-empty {
  padding: 20px 12px;
  font-size: 12px;
  color: var(--text-faint);
  text-align: center;
  display: flex;
  align-items: center;
  justify-content: center;
}
.tree-error { color: var(--danger); }

/* ── Main panel ── */
.main-panel {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  min-width: 0;
}

.panel-header {
  display: flex;
  align-items: center;
  height: 38px;
  padding: 0 14px;
  border-bottom: 1px solid var(--border);
  background: var(--bg);
  flex-shrink: 0;
}

.panel-title {
  font-size: 13px;
  font-weight: 600;
  color: var(--text);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* ── Document list ── */
.doc-list { flex: 1; overflow-y: auto; }
</style>
