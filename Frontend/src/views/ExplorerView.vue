<template>
  <div class="explorer-view">

    <!-- ── Sidebar ── -->
    <div class="sidebar" :style="{ width: sidebarWidth + 'px' }" @contextmenu.prevent="showCtx($event, 'tree')">
      <div class="sidebar-header">
        <span class="section-label">{{ i18n.t('explorer.folders') }}</span>
      </div>

      <div v-if="foldersStore.loading" class="tree-empty">
        <span class="spinner spinner-md"></span>
      </div>
      <div v-else-if="foldersStore.error" class="tree-empty tree-error">{{ foldersStore.error }}</div>
      <div v-else-if="!foldersStore.tree.length" class="tree-empty">{{ i18n.t('explorer.noFoldersYet') }}</div>
      <div v-else class="tree-body" @keydown="onTreeKeydown">
        <FolderTreeItem
          v-for="folder in foldersStore.tree"
          :key="folder.id"
          :folder="folder"
          :depth="0"
          :selected-id="selectedFolderId"
          :focused-id="focusedFolderId"
          :closed-ids="closedIds"
          @select="onTreeSelect"
          @context="onFolderCtx"
          @toggle="toggleFolder"
          @drop="onFolderDrop"
        />
      </div>
    </div>

    <!-- ── Resize handle ── -->
    <div class="resize-handle" @mousedown="startResize"></div>

    <!-- ── Main panel ── -->
    <div class="main-panel" @contextmenu.prevent="showCtx($event, 'content')">
      <div class="panel-header">
        <span class="panel-title">
          {{ selectedFolder ? selectedFolder.path : i18n.t('explorer.selectFolder') }}
        </span>
      </div>

      <div v-if="!selectedFolderId" class="empty-state">
        <svg width="32" height="32" viewBox="0 0 20 20" fill="currentColor" style="color:var(--border-input)">
          <path d="M2 6a2 2 0 012-2h4l2 2h6a2 2 0 012 2v6a2 2 0 01-2 2H4a2 2 0 01-2-2V6z"/>
        </svg>
        <span>{{ i18n.t('explorer.selectFolderHint') }}</span>
      </div>

      <div v-else-if="docsLoading" class="empty-state">
        <span class="spinner spinner-lg"></span>
      </div>

      <div v-else-if="docsError" class="notice notice-error" style="margin:12px">{{ docsError }}</div>

      <div v-else-if="!docs.length" class="empty-state">
        <span>{{ i18n.t('explorer.noDocsInFolder') }}</span>
      </div>

      <div v-else class="doc-list" @keydown="onDocListKeydown">
        <DocumentCard
          v-for="doc in sortedDocs"
          :key="doc.id"
          :doc="doc"
          :focused="focusedDocId === doc.id"
          :draggable="auth.isAdmin"
          :can-edit="auth.isAdmin"
          :can-link="auth.isAdmin"
          :can-create="auth.isAdmin"
          :is-link="doc.isLink"
          :link-id="doc.linkId"
          :checked="selectedIds.has(doc.id)"
          :selected-count="selectedIds.size"
          :all-selected="selectedIds.size > 0"
          :active-sort="sortMode"
          @edit="openEdit"
          @delete="deleteDoc"
          @remove-link="removeLink"
          @check="toggleCheck"
          @select-all="toggleAll"
          @link="onLinkDoc"
          @move="onMoveDoc"
          @new-doc="openNew"
          @new-folder="() => openNewFolder(selectedFolderId)"
          @sort="sortMode = $event"
          @dragstart="onDocDragStart"
        />
      </div>
    </div>

    <!-- ── New Folder modal ── -->
    <Teleport to="body">
      <div v-if="showNewFolder" class="modal-backdrop" @click.self="showNewFolder = false">
        <div class="modal-popup modal-popup-sm">
          <div class="modal-header">
            <span class="modal-header-title">{{ i18n.t('edit.newFolder') }}</span>
            <button type="button" class="modal-close" @click="showNewFolder = false">✕</button>
          </div>
          <div class="modal-body">
            <div class="field">
              <label class="field-label">{{ i18n.t('common.name') }}</label>
              <input
                ref="newFolderInputEl"
                v-model="newFolderName"
                type="text"
                class="input"
                :placeholder="i18n.t('edit.folderNamePlaceholder')"
                @keydown.enter.prevent="addFolder"
              />
            </div>
          </div>
          <div class="modal-actions">
            <button type="button" class="btn btn-ghost btn-sm" @click="showNewFolder = false">{{ i18n.t('common.cancel') }}</button>
            <button type="button" class="btn btn-primary btn-sm"
              :disabled="!newFolderName.trim() || newFolderLoading" @click="addFolder">
              {{ newFolderLoading ? i18n.t('create.saving') : i18n.t('common.save') }}
            </button>
          </div>
        </div>
      </div>

      <!-- ── Rename Folder modal ── -->
      <div v-if="showRenameFolder" class="modal-backdrop" @click.self="showRenameFolder = false">
        <div class="modal-popup modal-popup-sm">
          <div class="modal-header">
            <span class="modal-header-title">{{ i18n.t('explorer.renameFolder') }}</span>
            <button type="button" class="modal-close" @click="showRenameFolder = false">✕</button>
          </div>
          <div class="modal-body">
            <div class="field">
              <label class="field-label">{{ i18n.t('explorer.newName') }}</label>
              <input
                ref="renameFolderInputEl"
                v-model="renameFolderName"
                type="text"
                class="input"
                :placeholder="i18n.t('edit.folderNamePlaceholder')"
                @keydown.enter.prevent="doRenameFolder"
              />
            </div>
          </div>
          <div class="modal-actions">
            <button type="button" class="btn btn-ghost btn-sm" @click="showRenameFolder = false">{{ i18n.t('common.cancel') }}</button>
            <button type="button" class="btn btn-primary btn-sm"
              :disabled="!renameFolderName.trim() || renameFolderLoading" @click="doRenameFolder">
              {{ renameFolderLoading ? i18n.t('create.saving') : i18n.t('explorer.rename') }}
            </button>
          </div>
        </div>
      </div>
    </Teleport>

    <!-- ── No text warning ── -->
    <Teleport to="body">
      <div v-if="showNoTextWarning" class="modal-backdrop" style="z-index: 10000;">
        <div class="modal-popup modal-popup-sm">
          <div class="modal-header">
            <span class="modal-header-title">{{ i18n.t('explorer.noTextTitle') }}</span>
            <button type="button" class="modal-close" @click="showNoTextWarning = false">✕</button>
          </div>
          <div class="modal-body">
            <p class="modal-text">{{ noTextWarningMsg }}</p>
          </div>
          <div class="modal-actions">
            <button class="btn btn-ghost btn-sm" @click="showNoTextWarning = false">{{ i18n.t('explorer.continueEditing') }}</button>
            <button class="btn btn-primary btn-sm" @click="saveAnyway">{{ i18n.t('explorer.saveAnyway') }}</button>
          </div>
        </div>
      </div>
    </Teleport>

    <!-- ── Context menu ── -->
    <Teleport to="body">
      <div v-if="ctxMenu" class="ctx-menu" :style="{ top: ctxMenu.y + 'px', left: ctxMenu.x + 'px' }" @click.stop>
        <template v-if="ctxMenu.type === 'tree'">
          <button class="ctx-item" @click="ctxAction(() => openNewFolder(null))">{{ i18n.t('explorer.newRootFolder') }}</button>
        </template>
        <template v-if="ctxMenu.type === 'folder'">
          <button class="ctx-item" @click="ctxAction(openNew)">{{ i18n.t('create.newDocument') }}</button>
          <button class="ctx-item" @click="ctxAction(() => openNewFolder(ctxMenu!.folderId ?? null))">{{ i18n.t('explorer.newSubfolder') }}</button>
          <button class="ctx-item" @click="ctxRenameFolder">{{ i18n.t('explorer.rename') }}</button>
          <div class="ctx-divider"></div>
          <button class="ctx-item ctx-item-danger" @click="ctxDeleteFolder">{{ i18n.t('common.delete') }}</button>
        </template>
        <template v-if="ctxMenu.type === 'content'">
          <button class="ctx-item" @click="ctxAction(openNew)">{{ i18n.t('create.newDocument') }}</button>
          <button class="ctx-item" @click="ctxAction(() => openNewFolder(selectedFolderId))">{{ i18n.t('edit.newFolder') }}</button>
        </template>
      </div>
    </Teleport>

    <!-- ── Edit / New Document modal ── -->
    <Teleport to="body">
      <div v-if="showEditModal" class="modal-backdrop" @click.self="closeEditModal">
        <div class="modal-popup modal-popup-lg">

          <div class="modal-header">
            <span class="modal-header-title">{{ editIsNew ? i18n.t('create.newDocument') : editTitle }}</span>
            <button type="button" class="modal-close" @click="closeEditModal">✕</button>
          </div>

          <div v-if="editValidationError" class="notice notice-warn edit-notice">{{ editValidationError }}</div>
          <div v-if="editErrorMsg" class="notice notice-error edit-notice">{{ editErrorMsg }}</div>

          <div class="edit-modal-body">

            <!-- Metadata column -->
            <div class="edit-col">
              <div class="edit-col-header">{{ i18n.t('common.metadata') }}</div>
              <div class="edit-col-body">

                <div class="field-row">
                  <div class="field">
                    <label class="field-label">{{ i18n.t('common.date') }}</label>
                    <input type="date" v-model="editDate" class="input" />
                  </div>
                  <div class="field">
                    <label class="field-label">{{ i18n.t('common.language') }}</label>
                    <select v-model="editLanguage" class="select">
                      <option v-for="lang in languages" :key="lang.id" :value="lang.name">{{ lang.name }}</option>
                    </select>
                  </div>
                </div>

                <div class="field">
                  <label class="field-label">{{ i18n.t('common.folder') }}</label>
                  <select v-model="editFldid" class="select">
                    <option :value="null">{{ i18n.t('common.none') }}</option>
                    <option v-for="f in flatFolders" :key="f.id" :value="f.id">{{ f.path }}</option>
                  </select>
                </div>

                <div class="field">
                  <label class="field-label">{{ i18n.t('common.title') }} <span class="req">*</span></label>
                  <input type="text" v-model="editTitleField" :placeholder="i18n.t('create.titlePlaceholder')" class="input"
                    @input="editValidationError = null" />
                </div>

                <div class="field edit-field-grow">
                  <label class="field-label">{{ i18n.t('create.descriptionText') }}</label>
                  <textarea v-model="editText" rows="6" :placeholder="i18n.t('create.descriptionPlaceholder')"
                    class="input textarea"></textarea>
                </div>

              </div>
            </div>

            <!-- Source column -->
            <div class="edit-col">
              <div class="edit-col-header">{{ i18n.t('common.source') }}</div>
              <div class="edit-col-body">

                <div v-if="!editIsNew && editCurrentFilename" class="field">
                  <label class="field-label">{{ i18n.t('edit.currentFile') }}</label>
                  <div class="current-file">
                    <a href="#" class="file-link" @click.prevent="openOrDownload(editId!, editCurrentFilename, i18n.t('chat.cannotDisplayFile'))">{{ editCurrentFilename }}</a>
                  </div>
                </div>

                <div class="field">
                  <label class="field-label">{{ i18n.t('edit.chooseFile') }}</label>
                  <div class="file-row">
                    <button type="button" class="btn btn-grey btn-sm" @click="editFileInputRef?.click()">
                      {{ i18n.t('edit.chooseFile') }}
                    </button>
                    <span class="file-chosen">{{ editSelectedFile ? editSelectedFile.name : i18n.t('edit.noFileChosen') }}</span>
                    <button type="button" class="btn btn-grey btn-sm"
                      :disabled="!ocrEnabled || ocrLoading" @click="runOcr">
                      {{ ocrLoading ? '…' : i18n.t('create.ocr') }}
                    </button>
                    <input ref="editFileInputRef" type="file" @change="onFileChange" style="display:none" />
                  </div>
                </div>

                <div class="field">
                  <label class="field-label">{{ i18n.t('create.pasteImage') }}</label>
                  <div ref="pasteArea" tabindex="0" @paste="onPaste" class="paste-zone"
                    :class="{ 'paste-active': !!editPastedFile }">
                    {{ editPastedFile ? i18n.t('create.pasted', { name: editPastedFile.name || i18n.t('create.pastedImageDefault') }) : i18n.t('create.pasteHint') }}
                  </div>
                </div>

                <div class="divider"><span>{{ i18n.t('common.or') }}</span></div>

                <div class="field">
                  <label class="field-label">{{ i18n.t('common.url') }}</label>
                  <input type="url" v-model="editUrl" :placeholder="i18n.t('create.urlPlaceholder')" class="input"
                    :disabled="!!(editSelectedFile || editPastedFile)" />
                </div>

              </div>
            </div>
          </div>

          <div class="modal-actions">
            <div class="spacer"></div>
            <span v-if="editFormLoading" class="saving-hint">
              <span class="spinner spinner-md"></span> {{ i18n.t('create.saving') }}
            </span>
            <button v-if="!editIsNew" class="btn btn-primary btn-sm" :disabled="editFormLoading" @click="confirmDelete">
              {{ i18n.t('common.delete') }}
            </button>
            <button class="btn btn-ghost btn-sm" @click="resetEditForm">{{ i18n.t('common.reset') }}</button>
            <button class="btn btn-primary btn-sm" :disabled="editFormLoading" @click="submitEdit">
              {{ editIsNew ? i18n.t('common.save') : i18n.t('common.update') }}
            </button>
          </div>

        </div>
      </div>
    </Teleport>

    <LinkFolderModal
      :visible="showFolderPicker"
      :title="folderPickerMode === 'move' ? i18n.t('explorer.moveToFolder') : i18n.t('explorer.linkToFolder')"
      :confirm-label="folderPickerMode === 'move' ? i18n.t('linkFolderModal.move') : i18n.t('linkFolderModal.link')"
      @close="showFolderPicker = false"
      @confirm="onFolderPickerConfirm"
    />

  </div>
</template>

<script setup lang="ts">
import { scanOcr } from '../api/ocr'
import { storeDocument } from '../api/store'
import { useAuthStore } from '../stores/auth'
import { useI18nStore } from '../stores/i18n'
import { openOrDownload } from '../utils/file'
import { listLanguages } from '../api/languages'
import { useFoldersStore } from '../stores/folders'
import { useConfirmStore } from '../stores/confirm'
import { useResize } from '../composables/useResize'
import DocumentCard from '../components/DocumentCard.vue'
import { useEditRequestStore } from '../stores/editRequest'
import FolderTreeItem from '../components/FolderTreeItem.vue'
import LinkFolderModal from '../components/LinkFolderModal.vue'
import { ref, computed, nextTick, watch, onMounted, onUnmounted } from 'vue'
import type { DocumentResult, DocumentDetail, Language, Folder } from '../types'
import { getFolderDocuments, createFolder, renameFolder, deleteFolder } from '../api/folders'
import { getDocument, updateDocument, deleteDocument, deleteLink, linkDocuments, moveDocument } from '../api/documents'


const { width: sidebarWidth, startResize } = useResize(280, 140, 520)
const auth = useAuthStore()
const editRequestStore = useEditRequestStore()
const i18n = useI18nStore()
const confirm = useConfirmStore()

watch(() => editRequestStore.pendingId, id =>
{
  if (id !== null)
  {
    openEdit(id)
    editRequestStore.clear()
  }
})


watch(() => editRequestStore.pendingNew, val =>
{
  if (val)
  {
    openNew()
    editRequestStore.clear()
  }
})


// ── Folders ──────────────────────────────────────────────────────
const foldersStore = useFoldersStore()
foldersStore.load()


function flattenFolders(nodes: Folder[], prefix = ''): { id: number; path: string }[]
{
  const out: { id: number; path: string }[] = []
  for (const n of nodes)
  {
    const path = prefix ? `${prefix} / ${n.name}` : n.name
    out.push({ id: n.id, path })
    out.push(...flattenFolders(n.children, path))
  }
  return(out)
}


const flatFolders = computed(() => flattenFolders(foldersStore.tree))
const selectedFolder = computed(() => flatFolders.value.find(f => f.id === selectedFolderId.value) ?? null)


// ── Tree keyboard navigation ───────────────────────────────────────
const closedIds = ref(new Set<number>())
const focusedFolderId = ref<number | null>(null)


interface FlatFolderNode { id: number; parentId: number | null; hasChildren: boolean }


const flatVisibleFolders = computed<FlatFolderNode[]>(() =>
{
  const out: FlatFolderNode[] = []
  function walk(nodes: Folder[], parentId: number | null)
  {
    for (const n of nodes)
    {
      out.push({ id: n.id, parentId, hasChildren: n.children.length > 0 })
      if (n.children.length && !closedIds.value.has(n.id)) walk(n.children, n.id)
    }
  }
  walk(foldersStore.tree, null)
  return(out)
})


let initialTreeFocusDone = false
watch(() => foldersStore.tree, tree =>
{
  if (!initialTreeFocusDone && tree.length)
  {
    initialTreeFocusDone = true
    focusedFolderId.value = tree[0].id
    focusTreeRow(tree[0].id)
  }
}, { immediate: true })


function toggleFolder(id: number)
{
  const next = new Set(closedIds.value)
  if (next.has(id)) next.delete(id)
  else next.add(id)
  closedIds.value = next
}


function focusTreeRow(id: number)
{
  nextTick(() => document.querySelector<HTMLElement>(`.tree-body [data-folder-id="${id}"]`)?.focus())
}


function onTreeSelect(id: number)
{
  focusedFolderId.value = id
  selectFolder(id)
}


function moveTreeFocus(id: number)
{
  focusedFolderId.value = id
  selectFolder(id)
  focusTreeRow(id)
}


function onTreeKeydown(e: KeyboardEvent)
{
  const list = flatVisibleFolders.value
  const idx = list.findIndex(n => n.id === focusedFolderId.value)
  if (idx === -1) return

  switch (e.key)
  {
    case 'ArrowDown':
      e.preventDefault()
      if (idx < list.length - 1) moveTreeFocus(list[idx + 1].id)
      break

    case 'ArrowUp':
      e.preventDefault()
      if (idx > 0) moveTreeFocus(list[idx - 1].id)
      break

    case 'ArrowRight':
    {
      e.preventDefault()
      const node = list[idx]
      if (node.hasChildren && closedIds.value.has(node.id)) toggleFolder(node.id)
      else if (node.hasChildren && list[idx + 1]?.parentId === node.id) moveTreeFocus(list[idx + 1].id)
      break
    }

    case 'ArrowLeft':
    {
      e.preventDefault()
      const node = list[idx]
      if (node.hasChildren && !closedIds.value.has(node.id)) toggleFolder(node.id)
      else if (node.parentId !== null) moveTreeFocus(node.parentId)
      break
    }

    case 'Home':
      e.preventDefault()
      moveTreeFocus(list[0].id)
      break

    case 'End':
      e.preventDefault()
      moveTreeFocus(list[list.length - 1].id)
      break
  }
}


function focusTree()
{
  if (focusedFolderId.value !== null) focusTreeRow(focusedFolderId.value)
}


// ── Folder documents ─────────────────────────────────────────────
const selectedFolderId = ref<number | null>(null)
const docs = ref<DocumentResult[]>([])
const docsLoading = ref(false)
const docsError = ref<string | null>(null)
const sortMode = ref<'title' | 'date'>('title')
const selectedIds = ref(new Set<number>())
const lastCheckedId = ref<number | null>(null)

const sortedDocs = computed(() =>
{
  const list = [...docs.value]
  if (sortMode.value === 'date')
    return(list.sort((a, b) => (b.date ?? '').localeCompare(a.date ?? '')))
  return(list.sort((a, b) => (a.title ?? '').localeCompare(b.title ?? '', undefined, { sensitivity: 'base' })))
})


// ── Document list keyboard navigation ──────────────────────────────
const focusedDocId = ref<number | null>(null)


watch(sortedDocs, list =>
{
  if (!list.length) { focusedDocId.value = null; return }
  if (focusedDocId.value === null || !list.some(d => d.id === focusedDocId.value)) focusedDocId.value = list[0].id
})


function focusDocRow(id: number)
{
  nextTick(() => document.querySelector<HTMLElement>(`.doc-list [data-doc-id="${id}"]`)?.focus())
}


function onDocListKeydown(e: KeyboardEvent)
{
  const list = sortedDocs.value
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


function focusDocList()
{
  if (focusedDocId.value !== null) focusDocRow(focusedDocId.value)
}


defineExpose({ focus: focusTree, focusDocList })


async function selectFolder(id: number)
{
  if (selectedFolderId.value === id) return
  selectedFolderId.value = id
  selectedIds.value = new Set()
  lastCheckedId.value = null
  await loadFolderDocs(id)
}


async function loadFolderDocs(id: number)
{
  docs.value = []
  docsError.value = null
  docsLoading.value = true
  try
  {
    const res = await getFolderDocuments(id)
    docs.value = (res.data.documents || []) as DocumentResult[]
  }
  catch
  {
    docsError.value = i18n.t('explorer.loadDocsFailed')
  }
  finally
  {
    docsLoading.value = false
  }
}


async function reloadDocs()
{
  if (selectedFolderId.value === null) return
  try
  {
    const res = await getFolderDocuments(selectedFolderId.value)
    docs.value = (res.data.documents || []) as DocumentResult[]
  }
  catch
  {
    /* ignore */
  }
}


// ── New Folder ────────────────────────────────────────────────────
const ctxMenu = ref<{ x: number; y: number; type: 'tree' | 'content' | 'folder'; folderId?: number | null } | null>(null)
const showNewFolder = ref(false)
const newFolderPid = ref<number | null>(null)
const newFolderName = ref('')
const newFolderLoading = ref(false)
const newFolderInputEl = ref<HTMLInputElement | null>(null)
const deleteFolderLoading = ref(false)
const showRenameFolder = ref(false)
const renameFolderName = ref('')
const renameFolderLoading = ref(false)
const renameFolderInputEl = ref<HTMLInputElement | null>(null)


watch(showNewFolder, v =>
{
  if (v) nextTick(() => newFolderInputEl.value?.focus())
})

watch(showRenameFolder, v =>
{
  if (v) nextTick(() => renameFolderInputEl.value?.focus())
})


function openNewFolder(pid: number | null)
{
  newFolderPid.value = pid
  showNewFolder.value = true
}


let selfOpeningCtx = false
const onCloseAllCtx = () => { if (!selfOpeningCtx) ctxMenu.value = null }


function showCtx(e: MouseEvent, type: 'tree' | 'content')
{
  if (!auth.isAdmin) return
  selfOpeningCtx = true
  window.dispatchEvent(new Event('close-all-ctx'))
  selfOpeningCtx = false
  const x = Math.min(e.clientX, window.innerWidth - 210)
  const y = Math.min(e.clientY, window.innerHeight - 120)
  ctxMenu.value = { x: Math.max(4, x), y: Math.max(4, y), type }
}


function onFolderCtx(payload: { id: number; e: MouseEvent })
{
  if (!auth.isAdmin) return
  selfOpeningCtx = true
  window.dispatchEvent(new Event('close-all-ctx'))
  selfOpeningCtx = false
  const x = Math.min(payload.e.clientX, window.innerWidth - 210)
  const y = Math.min(payload.e.clientY, window.innerHeight - 200)
  ctxMenu.value = { x: Math.max(4, x), y: Math.max(4, y), type: 'folder', folderId: payload.id }
}


function ctxAction(fn: () => void)
{
  ctxMenu.value = null
  fn()
}


function ctxRenameFolder()
{
  const id = ctxMenu.value?.folderId
  ctxMenu.value = null
  if (!id) return
  selectedFolderId.value = id
  const folder = flatFolders.value.find(f => f.id === id)
  renameFolderName.value = folder?.path.split(' / ').at(-1) ?? ''
  showRenameFolder.value = true
}


async function ctxDeleteFolder()
{
  const id = ctxMenu.value?.folderId
  ctxMenu.value = null
  if (!id) return
  const folder = flatFolders.value.find(f => f.id === id)
  if (!await confirm.ask({ message: i18n.t('explorer.deleteFolderConfirm', { path: folder?.path ?? '' }), confirmLabel: i18n.t('common.delete'), danger: true })) return
  deleteFolderLoading.value = true
  try
  {
    await deleteFolder(id)
    if (selectedFolderId.value === id) { selectedFolderId.value = null; docs.value = [] }
    await foldersStore.load()
  }
  catch (err)
  {
    const axiosErr = err as { response?: { status?: number } }
    docsError.value = axiosErr?.response?.status === 409 ? i18n.t('explorer.deleteFolderNotEmpty') : i18n.t('explorer.deleteFolderFailed')
  }
  finally
  {
    deleteFolderLoading.value = false
  }
}


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
    showNewFolder.value = false
  }
  catch
  {
    /* ignore */
  }
  finally
  {
    newFolderLoading.value = false
  }
}


function startRename()
{
  renameFolderName.value = selectedFolder.value?.path.split(' / ').at(-1) ?? ''
  showRenameFolder.value = true
}


async function doRenameFolder()
{
  const name = renameFolderName.value.trim()
  if (!name || selectedFolderId.value === null) return
  renameFolderLoading.value = true
  try
  {
    await renameFolder(selectedFolderId.value, name)
    await foldersStore.load()
    renameFolderName.value = ''
    showRenameFolder.value = false
  }
  catch
  {
    /* ignore */
  }
  finally
  {
    renameFolderLoading.value = false
  }
}


async function deleteSelectedFolder()
{
  if (!selectedFolderId.value || docs.value.length) return
  if (!await confirm.ask({ message: i18n.t('explorer.deleteFolderConfirm', { path: selectedFolder.value?.path ?? '' }), confirmLabel: i18n.t('common.delete'), danger: true })) return
  deleteFolderLoading.value = true
  try
  {
    await deleteFolder(selectedFolderId.value)
    selectedFolderId.value = null
    docs.value = []
    await foldersStore.load()
  }
  catch
  {
    docsError.value = i18n.t('explorer.deleteFolderFailed')
  }
  finally
  {
    deleteFolderLoading.value = false
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
  }
  catch
  {
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

const showNoTextWarning = ref(false)
const noTextWarningMsg = ref('')
const editFormLoading = ref(false)
const ocrLoading = ref(false)
const editErrorMsg = ref<string | null>(null)
const editValidationError = ref<string | null>(null)


const ocrEnabled = computed(() =>
{
  const f = editSelectedFile.value || editPastedFile.value
  return(!!f && (f.type.startsWith('image/') || f.type === 'application/pdf'))
})


function todayIso()
{
  return(new Date().toISOString().split('T')[0])
}


function clearEditForm()
{
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
  if (languages.value.length && !editLanguage.value)
  {
    const da = languages.value.find(l => l.id === 'DA')
    editLanguage.value = da ? da.name : languages.value[0].name
  }
}


function openNew()
{
  editIsNew.value = true
  editId.value = null
  editTitle.value = i18n.t('create.newDocument')
  clearEditForm()
  showEditModal.value = true
}


async function deleteOne(id: number): Promise<void>
{
  const doc = docs.value.find(d => d.id === id)
  if (doc?.isLink && doc.linkId) await deleteLink(doc.linkId)
  else await deleteDocument(id)
}


async function deleteDoc(id: number)
{
  const idsToDelete = selectedIds.value.size > 1 ? [...selectedIds.value] : [id]
  if (idsToDelete.length > 1)
  {
    if (!await confirm.ask({ message: i18n.t('explorer.deleteItemsConfirm', { count: String(idsToDelete.length) }), confirmLabel: i18n.t('common.delete'), danger: true })) return
    try
    {
      await Promise.all(idsToDelete.map(d => deleteOne(d)))
      selectedIds.value = new Set()
      await reloadDocs()
    }
    catch
    {
      /* ignore */
    }
  }
  else
  {
    const doc = docs.value.find(d => d.id === id)
    const label = doc?.isLink ? i18n.t('explorer.deleteLinkLabel', { title: String(doc.title ?? id) }) : `"${doc?.title ?? id}"`
    if (!await confirm.ask({ message: i18n.t('explorer.deleteSingleConfirm', { label }), confirmLabel: i18n.t('common.delete'), danger: true })) return
    try
    {
      await deleteOne(id)
      await reloadDocs()
    }
    catch
    {
      /* ignore */
    }
  }
}


const showFolderPicker = ref(false)
const folderPickerMode = ref<'link' | 'move'>('link')
const folderPickerDocIds = ref<number[]>([])


function onLinkDoc(id: number)
{
  folderPickerMode.value = 'link'
  folderPickerDocIds.value = selectedIds.value.size > 0 ? [...selectedIds.value] : [id]
  showFolderPicker.value = true
}


function onMoveDoc(id: number)
{
  folderPickerMode.value = 'move'
  folderPickerDocIds.value = selectedIds.value.size > 0 ? [...selectedIds.value] : [id]
  showFolderPicker.value = true
}


async function onFolderPickerConfirm(fldid: number)
{
  showFolderPicker.value = false
  try
  {
    if (folderPickerMode.value === 'link')
      await linkDocuments(fldid, folderPickerDocIds.value)
    else
    {
      await Promise.all(folderPickerDocIds.value.map(id => moveDocument(id, fldid)))
      selectedIds.value = new Set()
      await reloadDocs()
    }
  }
  catch
  {
    /* ignore */
  }
}


// ── Drag & drop move ─────────────────────────────────────────────
const DOC_DRAG_MIME = 'application/x-doc-ids'


function onDocDragStart(id: number, e: DragEvent)
{
  const ids = selectedIds.value.has(id) && selectedIds.value.size > 1 ? [...selectedIds.value] : [id]
  e.dataTransfer?.setData(DOC_DRAG_MIME, JSON.stringify(ids))
  if (e.dataTransfer) e.dataTransfer.effectAllowed = 'move'
}


async function onFolderDrop(folderId: number, e: DragEvent)
{
  const data = e.dataTransfer?.getData(DOC_DRAG_MIME)
  if (!data) return
  const ids: number[] = JSON.parse(data)
  if (!ids.length) return
  try
  {
    await Promise.all(ids.map(id => moveDocument(id, folderId)))
    selectedIds.value = new Set()
    await reloadDocs()
  }
  catch
  {
    /* ignore */
  }
}


function toggleCheck(id: number, shift: boolean, ctrl: boolean)
{
  if (shift && lastCheckedId.value !== null)
  {
    const list = sortedDocs.value
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
    selectedIds.value = new Set(sortedDocs.value.map(d => d.id))
}


async function removeLink(linkId: number)
{
  try
  {
    await deleteLink(linkId)
    await reloadDocs()
  }
  catch
  {
    /* ignore */
  }
}


async function openEdit(id: number)
{
  editIsNew.value = false
  editId.value = id
  clearEditForm()
  showEditModal.value = true
  editFormLoading.value = true
  try
  {
    const res = await getDocument(id)
    const d = res.data.document as DocumentDetail
    editDate.value = d.date ?? ''
    editTitleField.value = d.title ?? ''
    editTitle.value = d.title ?? ''
    editText.value = d.text ?? ''
    editFldid.value = d.fldid ?? null
    editUrl.value = d.url ?? ''
    editCurrentFilename.value = d.filename ?? null
  }
  catch
  {
    editErrorMsg.value = i18n.t('edit.loadDetailFailed')
  }
  finally
  {
    editFormLoading.value = false
  }
}


function closeEditModal()
{
  showEditModal.value = false
}


function resetEditForm()
{
  if (editIsNew.value) clearEditForm()
  else if (editId.value !== null) openEdit(editId.value)
}


function onFileChange(e: Event)
{
  const input = e.target as HTMLInputElement
  editSelectedFile.value = input.files?.[0] ?? null
  editPastedFile.value = null
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
        editPastedFile.value = file
        editSelectedFile.value = null
        if (editFileInputRef.value) editFileInputRef.value.value = ''
      }
      break
    }
  }
}


async function runOcr()
{
  const file = editSelectedFile.value || editPastedFile.value
  if (!file) return
  ocrLoading.value = true
  try
  {
    const res = await scanOcr(file)
    editText.value = res.data as string
  }
  catch (err)
  {
    console.error('OCR error:', err)
    editErrorMsg.value = i18n.t('create.ocrFailed')
  }
  finally
  {
    ocrLoading.value = false
  }
}


async function submitEdit()
{
  editValidationError.value = null
  editErrorMsg.value = null
  if (!editTitleField.value.trim()) { editValidationError.value = i18n.t('create.titleRequired'); return }
  const hasFile = !!(editSelectedFile.value || editPastedFile.value)
  const hasUrl = !!editUrl.value.trim()
  if (hasFile && hasUrl) { editValidationError.value = i18n.t('create.fileOrUrlNotBoth'); return }

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
  try
  {
    if (editIsNew.value)
    {
      const res = await storeDocument(fd)
      const data = res.data as { success: boolean; id?: number; warning?: string }
      if (data.success)
      {
        closeEditModal()
        await reloadDocs()
      }
      else if (data.warning)
      {
        noTextWarningMsg.value = data.warning
        showNoTextWarning.value = true
      }
      else
      {
        editErrorMsg.value = i18n.t('edit.serverError')
      }
    }
    else if (editId.value !== null)
    {
      const res = await updateDocument(editId.value, fd)
      if ((res.data as { success: boolean }).success)
      {
        closeEditModal()
        await reloadDocs()
      }
      else
      {
        editErrorMsg.value = i18n.t('edit.serverError')
      }
    }
  }
  catch (err: unknown)
  {
    const axiosErr = err as { response?: { data?: { warning?: string } } }
    if (axiosErr?.response?.data?.warning)
    {
      noTextWarningMsg.value = axiosErr.response.data.warning
      showNoTextWarning.value = true
    }
    else
      editErrorMsg.value = i18n.t('edit.failedConnection')
  }
  finally
  {
    editFormLoading.value = false
  }
}


async function saveAnyway()
{
  showNoTextWarning.value = false
  const fd = new FormData()
  fd.append('date', editDate.value)
  fd.append('fldid', String(editFldid.value ?? 0))
  fd.append('title', editTitleField.value)
  fd.append('language', editLanguage.value)
  if (editText.value) fd.append('text', editText.value)
  if (editSelectedFile.value) fd.append('file', editSelectedFile.value)
  else if (editPastedFile.value) fd.append('file', editPastedFile.value, 'pasted-image.png')
  if (editUrl.value) fd.append('url', editUrl.value)
  fd.append('noExtract', 'true')
  editFormLoading.value = true
  try
  {
    const res = await storeDocument(fd)
    if ((res.data as { success: boolean }).success)
    {
      closeEditModal()
      await reloadDocs()
    }
    else
    {
      editErrorMsg.value = i18n.t('edit.serverError')
    }
  }
  catch
  {
    editErrorMsg.value = i18n.t('edit.failedConnection')
  }
  finally
  {
    editFormLoading.value = false
  }
}


async function confirmDelete()
{
  if (editId.value === null || !await confirm.ask({ message: i18n.t('edit.deleteConfirm', { title: editTitleField.value }), confirmLabel: i18n.t('common.delete'), danger: true })) return
  editFormLoading.value = true
  try
  {
    await deleteDocument(editId.value)
    closeEditModal()
    await reloadDocs()
  }
  catch
  {
    editErrorMsg.value = i18n.t('edit.deleteFailed')
  }
  finally
  {
    editFormLoading.value = false
  }
}


// ── Global Escape key ─────────────────────────────────────────────
function onKeydown(e: KeyboardEvent)
{
  if (e.key === 'Escape')
  {
    if (ctxMenu.value) { ctxMenu.value = null; return }
    if (showEditModal.value) { closeEditModal(); return }
    if (showRenameFolder.value) { showRenameFolder.value = false; return }
    if (showNewFolder.value) { showNewFolder.value = false }
  }
}


function onGlobalClick()
{
  ctxMenu.value = null
}


function onVisibilityChange()
{
  if (document.visibilityState === 'visible' && docsError.value && selectedFolderId.value !== null)
    loadFolderDocs(selectedFolderId.value)
}


onMounted(() =>
{
  window.addEventListener('keydown', onKeydown)
  window.addEventListener('click', onGlobalClick)
  window.addEventListener('close-all-ctx', onCloseAllCtx)
  document.addEventListener('visibilitychange', onVisibilityChange)
})


onUnmounted(() =>
{
  window.removeEventListener('keydown', onKeydown)
  window.removeEventListener('click', onGlobalClick)
  window.removeEventListener('close-all-ctx', onCloseAllCtx)
  document.removeEventListener('visibilitychange', onVisibilityChange)
})
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
  gap: 8px;
  font-size: 17px;
  font-weight: 700;
  letter-spacing: 0.01em;
  color: var(--text-faint);
}

.ctx-menu {
  position: fixed;
  z-index: 9999;
  background: var(--bg);
  border: 1px solid var(--border);
  border-radius: 6px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.14);
  padding: 4px 0;
  min-width: 170px;
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
.ctx-item-danger { color: var(--danger); }
.ctx-item-danger:hover { background: var(--danger-bg, #fef2f2); }
.ctx-divider { height: 1px; background: var(--border); margin: 3px 0; }
</style>
