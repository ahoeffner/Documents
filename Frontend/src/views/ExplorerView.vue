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
        <div style="display:flex;gap:6px;flex-shrink:0">
          <button v-if="selectedFolderId" class="btn btn-primary btn-xs" @click="openNew">+ New Document</button>
          <button class="btn btn-ghost btn-xs" @click="showNewFolder = true">+ New Folder</button>
          <button
            v-if="selectedFolderId && !docs.length && !docsLoading"
            class="btn btn-ghost btn-xs"
            :disabled="deleteFolderLoading"
            @click="deleteSelectedFolder"
          >Delete Folder</button>
        </div>
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

      <div v-else-if="!docs.length" class="empty-state">
        <span>No documents in this folder</span>
        <button class="btn btn-primary btn-sm" @click="openNew">+ New Document</button>
      </div>

      <div v-else class="doc-list">
        <DocumentCard
          v-for="doc in docs"
          :key="doc.id"
          :doc="doc"
          :can-edit="true"
          @edit="openEdit"
        />
      </div>
    </div>

    <!-- ── New Folder modal ── -->
    <Teleport to="body">
      <div v-if="showNewFolder" class="modal-backdrop" @click.self="showNewFolder = false">
        <div class="modal-popup modal-popup-sm">
          <div class="modal-header">
            <span class="modal-header-title">New Folder</span>
            <button type="button" class="modal-close" @click="showNewFolder = false">✕</button>
          </div>
          <div class="modal-body">
            <div class="field">
              <label class="field-label">Name</label>
              <input
                ref="newFolderInputEl"
                v-model="newFolderName"
                type="text"
                class="input"
                placeholder="Folder name"
                @keydown.enter.prevent="addFolder"
              />
            </div>
          </div>
          <div class="modal-actions">
            <button type="button" class="btn btn-ghost btn-sm" @click="showNewFolder = false">Cancel</button>
            <button type="button" class="btn btn-primary btn-sm"
              :disabled="!newFolderName.trim() || newFolderLoading" @click="addFolder">
              {{ newFolderLoading ? 'Saving…' : 'Save' }}
            </button>
          </div>
        </div>
      </div>
    </Teleport>

    <!-- ── Edit / New Document modal ── -->
    <Teleport to="body">
      <div v-if="showEditModal" class="modal-backdrop" @click.self="closeEditModal">
        <div class="modal-popup modal-popup-lg">

          <div class="modal-header">
            <span class="modal-header-title">{{ editIsNew ? 'New Document' : editTitle }}</span>
            <button type="button" class="modal-close" @click="closeEditModal">✕</button>
          </div>

          <div v-if="editValidationError" class="notice notice-warn edit-notice">{{ editValidationError }}</div>
          <div v-if="editErrorMsg" class="notice notice-error edit-notice">{{ editErrorMsg }}</div>

          <div class="edit-modal-body">

            <!-- Metadata column -->
            <div class="edit-col">
              <div class="edit-col-header">Metadata</div>
              <div class="edit-col-body">

                <div class="field-row">
                  <div class="field">
                    <label class="field-label">Date</label>
                    <input type="date" v-model="editDate" class="input" />
                  </div>
                  <div class="field">
                    <label class="field-label">Language</label>
                    <select v-model="editLanguage" class="select">
                      <option v-for="lang in languages" :key="lang.id" :value="lang.name">{{ lang.name }}</option>
                    </select>
                  </div>
                </div>

                <div class="field">
                  <label class="field-label">Folder</label>
                  <select v-model="editFldid" class="select">
                    <option :value="null">— none —</option>
                    <option v-for="f in flatFolders" :key="f.id" :value="f.id">{{ f.path }}</option>
                  </select>
                </div>

                <div class="field">
                  <label class="field-label">Title <span class="req">*</span></label>
                  <input type="text" v-model="editTitleField" placeholder="Document title" class="input"
                    @input="editValidationError = null" />
                </div>

                <div class="field edit-field-grow">
                  <label class="field-label">Description / Text</label>
                  <textarea v-model="editText" rows="6" placeholder="Enter description or paste OCR result here…"
                    class="input textarea"></textarea>
                </div>

              </div>
            </div>

            <!-- Source column -->
            <div class="edit-col">
              <div class="edit-col-header">Source</div>
              <div class="edit-col-body">

                <div v-if="!editIsNew && editCurrentFilename" class="field">
                  <label class="field-label">Current File</label>
                  <div class="current-file">
                    <a :href="`/api/content/${editId}/file`" target="_blank" class="file-link">{{ editCurrentFilename }}</a>
                  </div>
                </div>

                <div class="field">
                  <label class="field-label">Choose File</label>
                  <div class="file-row">
                    <button type="button" class="btn btn-grey btn-sm" @click="editFileInputRef?.click()">
                      Choose File
                    </button>
                    <span class="file-chosen">{{ editSelectedFile ? editSelectedFile.name : 'No file chosen' }}</span>
                    <button type="button" class="btn btn-grey btn-sm"
                      :disabled="!ocrEnabled || ocrLoading" @click="runOcr">
                      {{ ocrLoading ? '…' : 'OCR' }}
                    </button>
                    <input ref="editFileInputRef" type="file" @change="onFileChange" style="display:none" />
                  </div>
                </div>

                <div class="field">
                  <label class="field-label">Paste Image</label>
                  <div ref="pasteArea" tabindex="0" @paste="onPaste" class="paste-zone"
                    :class="{ 'paste-active': !!editPastedFile }">
                    {{ editPastedFile ? `Pasted: ${editPastedFile.name || 'image'}` : 'Click here, then Ctrl+V / Cmd+V' }}
                  </div>
                </div>

                <div class="divider"><span>or</span></div>

                <div class="field">
                  <label class="field-label">URL</label>
                  <input type="url" v-model="editUrl" placeholder="https://example.com/document.pdf" class="input"
                    :disabled="!!(editSelectedFile || editPastedFile)" />
                </div>

              </div>
            </div>
          </div>

          <div class="modal-actions">
            <button v-if="!editIsNew" class="btn btn-primary btn-sm" :disabled="editFormLoading" @click="confirmDelete">
              Delete
            </button>
            <div class="spacer"></div>
            <span v-if="editFormLoading" class="saving-hint">
              <span class="spinner spinner-sm"></span> Saving…
            </span>
            <button class="btn btn-ghost btn-sm" @click="resetEditForm">Reset</button>
            <button class="btn btn-primary btn-sm" :disabled="editFormLoading" @click="submitEdit">
              {{ editIsNew ? 'Save' : 'Update' }}
            </button>
          </div>

        </div>
      </div>
    </Teleport>

  </div>
</template>

<script setup lang="ts">
import { ref, computed, nextTick, watch, onMounted, onUnmounted } from 'vue'
import { useFoldersStore } from '../stores/folders'
import { getFolderDocuments, createFolder, deleteFolder } from '../api/folders'
import { getDocument, updateDocument, deleteDocument } from '../api/documents'
import { storeDocument } from '../api/store'
import { listLanguages } from '../api/languages'
import { scanOcr } from '../api/ocr'
import { useResize } from '../composables/useResize'
import FolderTreeItem from '../components/FolderTreeItem.vue'
import DocumentCard from '../components/DocumentCard.vue'
import type { DocumentResult, DocumentDetail, Language, Folder } from '../types'

const { width: sidebarWidth, startResize } = useResize(280, 140, 520)

// ── Folders ──────────────────────────────────────────────────────
const foldersStore = useFoldersStore()
foldersStore.load()

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

// ── Folder documents ─────────────────────────────────────────────
const selectedFolderId = ref<number | null>(null)
const docs = ref<DocumentResult[]>([])
const docsLoading = ref(false)
const docsError = ref<string | null>(null)

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

async function reloadDocs() {
  if (selectedFolderId.value === null) return
  try {
    const res = await getFolderDocuments(selectedFolderId.value)
    docs.value = (res.data.documents || []) as DocumentResult[]
  } catch { /* ignore */ }
}

// ── New Folder ────────────────────────────────────────────────────
const showNewFolder = ref(false)
const newFolderName = ref('')
const newFolderLoading = ref(false)
const newFolderInputEl = ref<HTMLInputElement | null>(null)
const deleteFolderLoading = ref(false)

watch(showNewFolder, v => { if (v) nextTick(() => newFolderInputEl.value?.focus()) })

async function addFolder() {
  const name = newFolderName.value.trim()
  if (!name) return
  newFolderLoading.value = true
  try {
    await createFolder(name, selectedFolderId.value)
    await foldersStore.load()
    newFolderName.value = ''
    showNewFolder.value = false
  } catch { /* ignore */ }
  finally { newFolderLoading.value = false }
}

async function deleteSelectedFolder() {
  if (!selectedFolderId.value || docs.value.length) return
  if (!window.confirm(`Delete folder "${selectedFolder.value?.path}"?`)) return
  deleteFolderLoading.value = true
  try {
    await deleteFolder(selectedFolderId.value)
    selectedFolderId.value = null
    docs.value = []
    await foldersStore.load()
  } catch {
    docsError.value = 'Failed to delete folder.'
  } finally {
    deleteFolderLoading.value = false
  }
}

// ── Languages ─────────────────────────────────────────────────────
const languages = ref<Language[]>([])
async function loadLanguages() {
  try {
    const res = await listLanguages()
    languages.value = (res.data.languages || []) as Language[]
  } catch {
    languages.value = [{ id: 'DA', name: 'danish' }, { id: 'EN', name: 'english' }]
  }
}
onMounted(loadLanguages)

// ── Edit / New Document modal ─────────────────────────────────────
const showEditModal = ref(false)
const editIsNew = ref(false)
const editId = ref<number | null>(null)
const editTitle = ref('')

const editDate = ref(todayIso())
const editTitleField = ref('')
const editLanguage = ref('')
const editText = ref('')
const editFldid = ref<number | null>(null)
const editUrl = ref('')
const editSelectedFile = ref<File | null>(null)
const editPastedFile = ref<File | null>(null)
const editCurrentFilename = ref<string | null>(null)
const editFileInputRef = ref<HTMLInputElement | null>(null)
const pasteArea = ref<HTMLElement | null>(null)

const editFormLoading = ref(false)
const ocrLoading = ref(false)
const editErrorMsg = ref<string | null>(null)
const editValidationError = ref<string | null>(null)

const ocrEnabled = computed(() => {
  const f = editSelectedFile.value || editPastedFile.value
  return !!f && (f.type.startsWith('image/') || f.type === 'application/pdf')
})

function todayIso() { return new Date().toISOString().split('T')[0] }

function clearEditForm() {
  editDate.value = todayIso()
  editTitleField.value = ''
  editText.value = ''
  editUrl.value = ''
  editFldid.value = selectedFolderId.value
  editSelectedFile.value = null
  editPastedFile.value = null
  editCurrentFilename.value = null
  editErrorMsg.value = null
  editValidationError.value = null
  if (editFileInputRef.value) editFileInputRef.value.value = ''
  if (languages.value.length && !editLanguage.value) {
    const da = languages.value.find(l => l.id === 'DA')
    editLanguage.value = da ? da.name : languages.value[0].name
  }
}

function openNew() {
  editIsNew.value = true
  editId.value = null
  editTitle.value = 'New Document'
  clearEditForm()
  showEditModal.value = true
}

async function openEdit(id: number) {
  editIsNew.value = false
  editId.value = id
  clearEditForm()
  showEditModal.value = true
  editFormLoading.value = true
  try {
    const res = await getDocument(id)
    const d = res.data.document as DocumentDetail
    editDate.value = d.date ?? ''
    editTitleField.value = d.title ?? ''
    editTitle.value = d.title ?? ''
    editText.value = d.text ?? ''
    editFldid.value = d.fldid ?? null
    editUrl.value = d.url ?? ''
    editCurrentFilename.value = d.filename ?? null
  } catch {
    editErrorMsg.value = 'Failed to load document details.'
  } finally {
    editFormLoading.value = false
  }
}

function closeEditModal() {
  showEditModal.value = false
}

function resetEditForm() {
  if (editIsNew.value) clearEditForm()
  else if (editId.value !== null) openEdit(editId.value)
}

function onFileChange(e: Event) {
  const input = e.target as HTMLInputElement
  editSelectedFile.value = input.files?.[0] ?? null
  editPastedFile.value = null
}

function onPaste(e: ClipboardEvent) {
  for (const item of (e.clipboardData?.items ?? [])) {
    if (item.kind === 'file' && item.type.startsWith('image/')) {
      e.preventDefault()
      const file = item.getAsFile()
      if (file) {
        editPastedFile.value = file
        editSelectedFile.value = null
        if (editFileInputRef.value) editFileInputRef.value.value = ''
      }
      break
    }
  }
}

async function runOcr() {
  const file = editSelectedFile.value || editPastedFile.value
  if (!file) return
  ocrLoading.value = true
  try {
    const res = await scanOcr(file)
    editText.value = res.data as string
  } catch (err) {
    console.error('OCR error:', err)
    editErrorMsg.value = 'OCR failed.'
  } finally {
    ocrLoading.value = false
  }
}

async function submitEdit() {
  editValidationError.value = null
  editErrorMsg.value = null
  if (!editTitleField.value.trim()) { editValidationError.value = 'Title is required.'; return }
  const hasFile = !!(editSelectedFile.value || editPastedFile.value)
  const hasUrl = !!editUrl.value.trim()
  if (hasFile && hasUrl) { editValidationError.value = 'Provide a file or a URL, not both.'; return }

  const fd = new FormData()
  fd.append('date', editDate.value)
  fd.append('fldid', String(editFldid.value ?? 0))
  fd.append('title', editTitleField.value)
  fd.append('language', editLanguage.value)
  if (editText.value) fd.append('text', editText.value)
  if (editSelectedFile.value) fd.append('file', editSelectedFile.value)
  else if (editPastedFile.value) fd.append('file', editPastedFile.value, 'pasted-image.png')
  if (editUrl.value) fd.append('url', editUrl.value)

  editFormLoading.value = true
  try {
    if (editIsNew.value) {
      const res = await storeDocument(fd)
      const data = res.data as { success: boolean; id?: number }
      if (data.success) {
        closeEditModal()
        await reloadDocs()
      } else {
        editErrorMsg.value = 'Server reported an error.'
      }
    } else if (editId.value !== null) {
      const res = await updateDocument(editId.value, fd)
      if ((res.data as { success: boolean }).success) {
        closeEditModal()
        await reloadDocs()
      } else {
        editErrorMsg.value = 'Server reported an error.'
      }
    }
  } catch {
    editErrorMsg.value = 'Failed — check the connection.'
  } finally {
    editFormLoading.value = false
  }
}

async function confirmDelete() {
  if (editId.value === null || !window.confirm(`Delete "${editTitleField.value}"?`)) return
  editFormLoading.value = true
  try {
    await deleteDocument(editId.value)
    closeEditModal()
    await reloadDocs()
  } catch {
    editErrorMsg.value = 'Delete failed.'
  } finally {
    editFormLoading.value = false
  }
}

// ── Global Escape key ─────────────────────────────────────────────
function onKeydown(e: KeyboardEvent) {
  if (e.key === 'Escape') {
    if (showEditModal.value) { closeEditModal(); return }
    if (showNewFolder.value) { showNewFolder.value = false }
  }
}
onMounted(() => window.addEventListener('keydown', onKeydown))
onUnmounted(() => window.removeEventListener('keydown', onKeydown))
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
  justify-content: space-between;
  height: 38px;
  padding: 0 10px 0 14px;
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

/* ── Edit modal internals ── */
.edit-notice { margin: 8px 16px 0; flex-shrink: 0; }

.edit-modal-body {
  display: flex;
  flex: 1;
  min-height: 0;
  overflow: hidden;
}

.edit-col {
  display: flex;
  flex-direction: column;
  flex: 1;
  overflow: hidden;
  border-right: 1px solid var(--border);
}
.edit-col:last-child { border-right: none; }

.edit-col-header {
  height: 30px;
  display: flex;
  align-items: center;
  padding: 0 14px;
  background: var(--bg-subtle);
  border-bottom: 1px solid var(--border);
  font-size: 10px;
  font-weight: 600;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: var(--text-muted);
  flex-shrink: 0;
}

.edit-col-body {
  flex: 1;
  overflow-y: auto;
  padding: 12px 14px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.field { display: flex; flex-direction: column; gap: 4px; }
.edit-field-grow { flex: 1; }
.edit-field-grow .textarea { flex: 1; min-height: 100px; }
.field-row { display: flex; gap: 10px; }
.field-row .field { flex: 1; }

.input.textarea { height: auto; padding: 7px 10px; resize: vertical; }

.current-file {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 5px 8px;
  background: var(--bg-muted);
  border-radius: 4px;
  font-size: 12px;
}
.file-link { color: var(--accent); flex: 1; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.file-link:hover { text-decoration: underline; }

.file-row { display: flex; align-items: center; gap: 6px; flex-wrap: wrap; }
.file-chosen { font-size: 11px; color: var(--text-muted); flex: 1; min-width: 0; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }

.paste-zone {
  padding: 10px 12px;
  border: 2px dashed var(--border-input);
  border-radius: 5px;
  background: var(--bg-subtle);
  font-size: 12px;
  color: var(--text-muted);
  text-align: center;
  cursor: pointer;
  outline: none;
  transition: border-color 0.1s;
}
.paste-zone:focus { border-color: var(--accent); }
.paste-active { border-color: var(--success); color: #15803d; background: var(--success-bg); }

.saving-hint {
  display: flex;
  align-items: center;
  gap: 5px;
  font-size: 12px;
  color: var(--text-faint);
}
</style>
