<template>
  <div class="edit-view">

    <!-- ── Left pane: document list ── -->
    <div class="list-pane" :style="{ width: listWidth + 'px' }">
      <div class="pane-header">
        <span class="section-label">Documents</span>
        <button class="btn-icon" title="New folder" @click="showNewFolder = true">
          <svg width="13" height="13" viewBox="0 0 20 20" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round">
            <path d="M2 6a2 2 0 012-2h4l2 2h6a2 2 0 012 2v6a2 2 0 01-2 2H4a2 2 0 01-2-2V6z"/>
            <path d="M10 9v4M8 11h4"/>
          </svg>
        </button>
      </div>

      <div class="list-filters">
        <select v-model="filterCatid" class="select filter-select" @change="loadList">
          <option :value="null">All folders</option>
          <option v-for="f in flatFolders" :key="f.id" :value="f.id">{{ f.path }}</option>
        </select>
        <input
          v-model="filterQ"
          type="text"
          placeholder="Filter…"
          class="input filter-input"
          @input="onFilterInput"
        />
      </div>

      <div v-if="listLoading" class="empty-state" style="flex:0;padding:20px 0">
        <span class="spinner spinner-md"></span>
      </div>
      <div v-else-if="listError" class="list-error">{{ listError }}</div>
      <div v-else-if="!docList.length" class="list-empty">No documents</div>
      <div v-else class="list-body">
        <div
          v-for="doc in docList"
          :key="doc.id"
          class="list-row"
          :class="{ selected: editId === doc.id }"
          @click="selectDoc(doc.id)"
        >
          {{ doc.title }}
        </div>
      </div>
    </div>

    <!-- ── Resize handle ── -->
    <div class="resize-handle" @mousedown="startResize"></div>

    <!-- ── Right pane: form ── -->
    <div class="form-pane">

      <div v-if="!isNew && editId === null" class="empty-state">
        <svg width="36" height="36" viewBox="0 0 20 20" fill="currentColor" style="color:var(--border-input)">
          <path d="M13.586 3.586a2 2 0 112.828 2.828l-.793.793-2.828-2.828.793-.793zM11.379 5.793L3 14.172V17h2.828l8.38-8.379-2.83-2.828z"/>
        </svg>
        <span>Select a document to edit or create a new one</span>
        <button class="btn btn-primary btn-sm" @click="startNew">+ New Document</button>
      </div>

      <template v-else>

        <!-- Form toolbar -->
        <div class="form-toolbar">
          <span class="mode-label">{{ isNew ? 'New Document' : title }}</span>
          <div class="spacer"></div>
          <span v-if="formLoading" class="saving-hint">
            <span class="spinner spinner-sm"></span> Saving…
          </span>
          <button class="btn btn-primary btn-sm" @click="startNew">+ New</button>
          <button v-if="!isNew" class="btn btn-primary btn-sm" :disabled="formLoading" @click="confirmDelete">Delete</button>
          <button class="btn btn-primary btn-sm" @click="resetForm">Reset</button>
          <button class="btn btn-primary btn-sm" :disabled="formLoading" @click="submit">
            {{ isNew ? 'Save' : 'Update' }}
          </button>
        </div>

        <!-- Notices -->
        <div v-if="successMsg" class="notice notice-success" style="margin:8px 14px 0">{{ successMsg }}</div>
        <div v-if="errorMsg"   class="notice notice-error"   style="margin:8px 14px 0">{{ errorMsg }}</div>
        <div v-if="validationError" class="notice notice-warn" style="margin:8px 14px 0">{{ validationError }}</div>

        <!-- Two-column form -->
        <div class="form-body">

          <!-- Left: metadata -->
          <div class="form-col">
            <div class="col-header">Metadata</div>
            <div class="col-body">

              <div class="field-row">
                <div class="field">
                  <label class="field-label">Date</label>
                  <input type="date" v-model="date" class="input" />
                </div>
                <div class="field">
                  <label class="field-label">Language</label>
                  <select v-model="language" class="select">
                    <option v-for="lang in languages" :key="lang.id" :value="lang.name">{{ lang.name }}</option>
                  </select>
                </div>
              </div>

              <div class="field">
                <label class="field-label">Folder</label>
                <select v-model="catid" class="select">
                  <option :value="null">— none —</option>
                  <option v-for="f in flatFolders" :key="f.id" :value="f.id">{{ f.path }}</option>
                </select>
              </div>

              <div class="field">
                <label class="field-label">Title <span class="req">*</span></label>
                <input type="text" v-model="title" placeholder="Document title" class="input"
                  @input="validationError = null" />
              </div>

              <div class="field field-grow">
                <label class="field-label">Description / Text</label>
                <textarea v-model="text" rows="8" placeholder="Enter description or paste OCR result here…"
                  class="input textarea"></textarea>
              </div>

            </div>
          </div>

          <!-- Right: source -->
          <div class="form-col">
            <div class="col-header">Source</div>
            <div class="col-body">

              <div v-if="!isNew && currentFilename" class="field">
                <label class="field-label">Current File</label>
                <div class="current-file">
                  <a :href="`/api/content/${editId}/file`" target="_blank" class="file-link">{{ currentFilename }}</a>
                </div>
              </div>

              <div class="field">
                <label class="field-label">Choose File</label>
                <div class="file-row">
                  <button type="button" class="btn btn-grey btn-sm" @click="fileInputRef?.click()">
                    Choose File
                  </button>
                  <span class="file-chosen">{{ selectedFile ? selectedFile.name : 'No file chosen' }}</span>
                  <button type="button" @click="runOcr" :disabled="!ocrEnabled || ocrLoading"
                    class="btn btn-grey btn-sm">
                    {{ ocrLoading ? '…' : 'OCR' }}
                  </button>
                  <input ref="fileInputRef" type="file" @change="onFileChange" style="display:none" />
                </div>
              </div>

              <div class="field">
                <label class="field-label">Paste Image</label>
                <div ref="pasteArea" tabindex="0" @paste="onPaste" class="paste-zone"
                  :class="{ 'paste-active': !!pastedFile }">
                  {{ pastedFile ? `Pasted: ${pastedFile.name || 'image'}` : 'Click here, then Ctrl+V / Cmd+V' }}
                </div>
              </div>

              <div class="divider"><span>or</span></div>

              <div class="field">
                <label class="field-label">URL</label>
                <input type="url" v-model="url" placeholder="https://example.com/document.pdf" class="input"
                  :disabled="!!(selectedFile || pastedFile)" />
              </div>

            </div>
          </div>
        </div>
      </template>
    </div>


    <!-- New Folder modal -->
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
              @keydown.esc="showNewFolder = false"
              @keydown.enter.prevent="addFolder"
            />
          </div>
          <div class="field">
            <label class="field-label">Parent Folder</label>
            <select v-model="newFolderPid" class="select">
              <option :value="null">— root —</option>
              <option v-for="f in flatFolders" :key="f.id" :value="f.id">{{ f.path }}</option>
            </select>
          </div>
        </div>
        <div class="modal-actions">
          <button type="button" class="btn btn-ghost btn-sm" @click="showNewFolder = false">Cancel</button>
          <button type="button" class="btn btn-primary btn-sm"
            :disabled="!newFolderName.trim() || newFolderLoading" @click="addFolder">
            {{ newFolderLoading ? 'Adding…' : 'Add Folder' }}
          </button>
        </div>
      </div>
    </div>
  </Teleport>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, nextTick, watch } from 'vue'
import type { DocumentResult, DocumentDetail, Language, Folder } from '../types'
import { scanOcr } from '../api/ocr'
import { storeDocument } from '../api/store'
import { createFolder } from '../api/folders'
import { listLanguages } from '../api/languages'
import { listDocuments, getDocument, updateDocument, deleteDocument } from '../api/documents'
import { useFoldersStore } from '../stores/folders'
import { useResize } from '../composables/useResize'


const { width: listWidth, startResize } = useResize(320, 160, 600)


// ── Folders ──────────────────────────────────────────────────────
const foldersStore = useFoldersStore()
onMounted(() => { foldersStore.load(); loadList(); loadLanguages() })

type FlatFolder = { id: number; path: string }

function flattenFolders(nodes: typeof foldersStore.tree, prefix = ''): FlatFolder[]
{
  const out: FlatFolder[] = []
  for (const n of nodes)
  {
    const path = prefix ? `${prefix} / ${n.name}` : n.name
    out.push({ id: n.id, path })
    out.push(...flattenFolders(n.children, path))
  }
  return(out)
}

const flatFolders = computed(() => flattenFolders(foldersStore.tree))


// ── Document list ─────────────────────────────────────────────────
const docList = ref<DocumentResult[]>([])
const listLoading = ref(false)
const listError = ref<string | null>(null)
const filterCatid = ref<number | null>(null)
const filterQ = ref('')
let filterTimer: ReturnType<typeof setTimeout> | null = null


async function loadList()
{
  listLoading.value = true
  listError.value = null
  try
  {
    const res = await listDocuments(filterCatid.value, filterQ.value)
    docList.value = (res.data.documents || []) as DocumentResult[]
  }
  catch
  {
    listError.value = 'Failed to load.'
  }
  finally
  {
    listLoading.value = false
  }
}


function onFilterInput()
{
  if (filterTimer) clearTimeout(filterTimer)
  filterTimer = setTimeout(loadList, 300)
}


// ── Mode ──────────────────────────────────────────────────────────
const editId = ref<number | null>(null)
const isNew = ref(false)


function startNew()
{
  editId.value = null
  isNew.value = true
  clearForm()
}


async function selectDoc(id: number)
{
  if (editId.value === id) return
  editId.value = id
  isNew.value = false
  clearForm()
  formLoading.value = true
  try
  {
    const res = await getDocument(id)
    const d = res.data.document as DocumentDetail
    date.value = d.date ?? ''
    title.value = d.title ?? ''
    text.value = d.text ?? ''
    catid.value = d.fldid ?? null
    url.value = d.url ?? ''
    currentFilename.value = d.filename ?? null
  }
  catch
  {
    errorMsg.value = 'Failed to load document details.'
  }
  finally
  {
    formLoading.value = false
  }
}


// ── Languages ─────────────────────────────────────────────────────
const languages = ref<Language[]>([])

async function loadLanguages()
{
  try
  {
    const res = await listLanguages()
    languages.value = (res.data.languages || []) as Language[]
    if (!language.value && languages.value.length)
    {
      const da = languages.value.find(l => l.id === 'DA')
      language.value = da ? da.name : languages.value[0].name
    }
  }
  catch
  {
    languages.value = [{ id: 'DA', name: 'danish' }, { id: 'EN', name: 'english' }]
  }
}


// ── Form state ────────────────────────────────────────────────────
const date = ref(todayIso())
const title = ref('')
const language = ref('')
const text = ref('')
const catid = ref<number | null>(null)
const url = ref('')
const selectedFile = ref<File | null>(null)
const pastedFile = ref<File | null>(null)
const currentFilename = ref<string | null>(null)
const fileInputRef = ref<HTMLInputElement | null>(null)
const pasteArea = ref<HTMLElement | null>(null)

const formLoading = ref(false)
const ocrLoading = ref(false)
const successMsg = ref<string | null>(null)
const errorMsg = ref<string | null>(null)
const validationError = ref<string | null>(null)


const ocrEnabled = computed(() =>
{
  const f = selectedFile.value || pastedFile.value
  return(!!f && (f.type.startsWith('image/') || f.type === 'application/pdf'))
})


function todayIso()
{
  return(new Date().toISOString().split('T')[0])
}


function clearForm()
{
  date.value = todayIso()
  title.value = ''
  text.value = ''
  url.value = ''
  catid.value = null
  selectedFile.value = null
  pastedFile.value = null
  currentFilename.value = null
  successMsg.value = null
  errorMsg.value = null
  validationError.value = null
  if (fileInputRef.value) fileInputRef.value.value = ''
}


function resetForm()
{
  if (isNew.value) clearForm()
  else if (editId.value !== null) selectDoc(editId.value)
}


function onFileChange(e: Event)
{
  const input = e.target as HTMLInputElement
  selectedFile.value = input.files?.[0] ?? null
  pastedFile.value = null
}


function onPaste(e: ClipboardEvent)
{
  for (const item of (e.clipboardData?.items ?? []))
  {
    if (item.kind === 'file' && item.type.startsWith('image/'))
    {
      e.preventDefault()
      const file = item.getAsFile()
      if (file)
      {
        pastedFile.value = file
        selectedFile.value = null
        if (fileInputRef.value) fileInputRef.value.value = ''
      }
      break
    }
  }
}


async function runOcr()
{
  const file = selectedFile.value || pastedFile.value
  if (!file) return
  ocrLoading.value = true
  try
  {
    const res = await scanOcr(file)
    text.value = res.data as string
  }
  catch
  {
    errorMsg.value = 'OCR failed.'
  }
  finally
  {
    ocrLoading.value = false
  }
}


// ── Submit ────────────────────────────────────────────────────────
async function submit()
{
  validationError.value = null
  errorMsg.value = null
  successMsg.value = null
  if (!title.value.trim()) { validationError.value = 'Title is required.'; return }
  const hasFile = !!(selectedFile.value || pastedFile.value)
  const hasUrl = !!url.value.trim()
  if (hasFile && hasUrl) { validationError.value = 'Provide a file or a URL, not both.'; return }

  const fd = new FormData()
  fd.append('date', date.value)
  fd.append('fldid', String(catid.value ?? 0))
  fd.append('title', title.value)
  fd.append('language', language.value)
  if (text.value) fd.append('text', text.value)
  if (selectedFile.value) fd.append('file', selectedFile.value)
  else if (pastedFile.value) fd.append('file', pastedFile.value, 'pasted-image.png')
  if (url.value) fd.append('url', url.value)

  formLoading.value = true
  try
  {
    if (isNew.value)
    {
      const res = await storeDocument(fd)
      const data = res.data as { success: boolean; id?: number }
      if (data.success)
      {
        successMsg.value = `Saved — ID ${data.id}`
        isNew.value = false
        editId.value = data.id ?? null
        await loadList()
      }
      else
      {
        errorMsg.value = 'Server reported an error.'
      }
    }
    else if (editId.value !== null)
    {
      const res = await updateDocument(editId.value, fd)
      if ((res.data as { success: boolean }).success)
      {
        successMsg.value = 'Updated.'
        await loadList()
      }
      else
      {
        errorMsg.value = 'Server reported an error.'
      }
    }
  }
  catch
  {
    errorMsg.value = 'Failed — check the connection.'
  }
  finally
  {
    formLoading.value = false
  }
}


// ── Delete ────────────────────────────────────────────────────────
async function confirmDelete()
{
  if (editId.value === null || !window.confirm(`Delete "${title.value}"?`)) return
  formLoading.value = true
  try
  {
    await deleteDocument(editId.value)
    editId.value = null
    isNew.value = false
    clearForm()
    await loadList()
  }
  catch
  {
    errorMsg.value = 'Delete failed.'
  }
  finally
  {
    formLoading.value = false
  }
}


// ── New Folder ────────────────────────────────────────────────────
const showNewFolder = ref(false)
const newFolderName = ref('')
const newFolderPid = ref<number | null>(null)
const newFolderLoading = ref(false)
const newFolderInputEl = ref<HTMLInputElement | null>(null)


watch(showNewFolder, v =>
{
  if (v) nextTick(() => newFolderInputEl.value?.focus())
})


async function addFolder()
{
  const name = newFolderName.value.trim()
  if (!name) return
  newFolderLoading.value = true
  try
  {
    await createFolder(name, newFolderPid.value)
    await foldersStore.load()
    newFolderName.value = ''
    newFolderPid.value = null
    showNewFolder.value = false
  }
  catch { /* ignore */ }
  finally
  {
    newFolderLoading.value = false
  }
}
</script>

<style scoped>
.edit-view {
  display: flex;
  flex: 1;
  overflow: hidden;
}

/* ── List pane ── */
.list-pane {
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  background: var(--bg-subtle);
  overflow: hidden;
}

.pane-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 36px;
  padding: 0 10px 0 12px;
  border-bottom: 1px solid var(--border);
  flex-shrink: 0;
}

.list-filters {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 7px 8px;
  border-bottom: 1px solid var(--border);
  flex-shrink: 0;
}

.filter-select, .filter-input { height: 26px; font-size: 12px; }

.list-empty {
  padding: 20px 12px;
  font-size: 12px;
  color: var(--text-faint);
  text-align: center;
}
.list-error { padding: 10px 12px; font-size: 12px; color: var(--danger); text-align: center; }

.list-body { flex: 1; overflow-y: auto; }

.list-row {
  padding: 7px 12px;
  border-bottom: 1px solid var(--bg-muted);
  cursor: pointer;
  font-size: 12px;
  color: var(--text);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  user-select: none;
}
.list-row:hover { background: var(--bg-muted); }
.list-row.selected { background: color-mix(in srgb, var(--accent) 12%, transparent); color: var(--accent-dark); font-weight: 500; }

/* ── Form pane ── */
.form-pane {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  min-width: 0;
}

.form-toolbar {
  display: flex;
  align-items: center;
  gap: 8px;
  height: 44px;
  padding: 0 14px;
  background: var(--bg);
  border-bottom: 1px solid var(--border);
  flex-shrink: 0;
}

.mode-label {
  font-size: 13px;
  font-weight: 600;
  color: var(--text);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 300px;
}

.saving-hint {
  display: flex;
  align-items: center;
  gap: 5px;
  font-size: 12px;
  color: var(--text-faint);
}

/* ── Form body ── */
.form-body {
  display: flex;
  flex: 1;
  overflow: hidden;
}

.form-col {
  display: flex;
  flex-direction: column;
  flex: 1;
  overflow: hidden;
  border-right: 1px solid var(--border);
}
.form-col:last-child { border-right: none; }

.col-header {
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

.col-body {
  flex: 1;
  overflow-y: auto;
  padding: 12px 14px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.field { display: flex; flex-direction: column; gap: 4px; }
.field-grow { flex: 1; }
.field-grow .textarea { flex: 1; min-height: 120px; }
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
</style>
