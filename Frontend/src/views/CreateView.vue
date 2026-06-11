<template>
  <div class="create-view">

    <!-- Toolbar -->
    <div class="toolbar">
      <span class="section-label">{{ i18n.t('create.newDocument') }}</span>
      <div class="spacer"></div>
      <button type="button" @click="resetForm" class="btn btn-ghost">{{ i18n.t('common.reset') }}</button>
      <button type="button" @click="submit" :disabled="loading" class="btn btn-primary">
        {{ loading ? i18n.t('create.saving') : i18n.t('create.saveDocument') }}
      </button>
    </div>

    <!-- Notices -->
    <div v-if="successMsg" class="notice notice-success">{{ i18n.t('create.savedNotice', { id: String(savedId) }) }}</div>
    <div v-if="error" class="notice notice-error">{{ error }}</div>
    <div v-if="validationError" class="notice notice-warn">{{ validationError }}</div>

    <!-- Two-pane form -->
    <div class="panes">

      <!-- Left: metadata -->
      <div class="pane">
        <div class="pane-header">
          <span class="pane-title">{{ i18n.t('common.metadata') }}</span>
        </div>
        <div class="pane-body">

          <div class="field-row">
            <div class="field">
              <label class="field-label">{{ i18n.t('common.date') }}</label>
              <input type="date" v-model="date" required class="input" />
            </div>
            <div class="field">
              <label class="field-label">{{ i18n.t('common.language') }}</label>
              <select v-model="language" class="input">
                <option v-for="lang in languages" :key="lang.id" :value="lang.name">{{ lang.name }}</option>
              </select>
            </div>
          </div>

          <div class="field">
            <label class="field-label">{{ i18n.t('common.category') }}</label>
            <select v-model="catid" class="input">
              <option value="">{{ i18n.t('common.none') }}</option>
              <option v-for="cat in categoriesStore.categories" :key="cat.id" :value="cat.id">{{ cat.name }}</option>
            </select>
          </div>

          <div class="field">
            <label class="field-label">{{ i18n.t('common.title') }} <span class="req">*</span></label>
            <input type="text" v-model="title" :placeholder="i18n.t('create.titlePlaceholder')" class="input" />
          </div>

          <div class="field field-grow">
            <label class="field-label">{{ i18n.t('create.descriptionText') }}</label>
            <textarea v-model="text" rows="8" :placeholder="i18n.t('create.descriptionPlaceholder')" class="input textarea"></textarea>
          </div>

        </div>
      </div>

      <!-- Right: source -->
      <div class="pane">
        <div class="pane-header">
          <span class="pane-title">{{ i18n.t('common.source') }}</span>
        </div>
        <div class="pane-body">

          <div class="field">
            <label class="field-label">{{ i18n.t('common.file') }}</label>
            <div class="file-row">
              <input ref="fileInputRef" type="file" @change="onFileChange" class="file-input" />
              <button type="button" @click="runOcr" :disabled="!ocrEnabled || ocrLoading" class="btn btn-accent">
                {{ ocrLoading ? '…' : i18n.t('create.ocr') }}
              </button>
            </div>
          </div>

          <div class="field">
            <label class="field-label">{{ i18n.t('create.pasteImage') }}</label>
            <div
              ref="pasteArea" tabindex="0"
              @paste="onPaste"
              class="paste-zone"
              :class="{ 'paste-zone-active': !!pastedFile }"
            >
              {{ pastedFile ? i18n.t('create.pasted', { name: pastedFile.name || i18n.t('create.pastedImageDefault') }) : i18n.t('create.pasteHint') }}
            </div>
          </div>

          <div class="divider"><span>{{ i18n.t('common.or') }}</span></div>

          <div class="field">
            <label class="field-label">{{ i18n.t('common.url') }}</label>
            <input type="url" v-model="url" :placeholder="i18n.t('create.urlPlaceholder')" class="input"
              :disabled="!!(selectedFile || pastedFile)" />
          </div>

        </div>
      </div>

    </div>
  </div>
</template>

<script setup lang="ts">
import { scanOcr } from '../api/ocr'
import type { Language } from '../types'
import { storeDocument } from '../api/store'
import { useI18nStore } from '../stores/i18n'
import { ref, computed, onMounted } from 'vue'
import { listLanguages } from '../api/languages'
import { useCategoriesStore } from '../stores/categories'


const props = withDefaults(defineProps<{ initialFldid?: number | null }>(), { initialFldid: null })
const emit = defineEmits<{ saved: [] }>()


const categoriesStore = useCategoriesStore()
const i18n = useI18nStore()


const date = ref(todayIso())
const catid = ref<number | ''>(props.initialFldid ?? '')
const title = ref('')
const text = ref('')
const language = ref('danish')
const url = ref('')
const selectedFile = ref<File | null>(null)
const pastedFile = ref<File | null>(null)
const languages = ref<Language[]>([])
const loading = ref(false)
const ocrLoading = ref(false)
const error = ref<string | null>(null)
const successMsg = ref(false)
const savedId = ref<number | null>(null)
const validationError = ref<string | null>(null)
const fileInputRef = ref<HTMLInputElement | null>(null)
const pasteArea = ref<HTMLElement | null>(null)


const ocrEnabled = computed(() =>
{
  const f = selectedFile.value || pastedFile.value
  return(!!f && (f.type.startsWith('image/') || f.type === 'application/pdf'))
})


function todayIso(): string
{
  return(new Date().toISOString().split('T')[0])
}


onMounted(async () =>
{
  await categoriesStore.load()
  try
  {
    const res = await listLanguages()
    languages.value = (res.data.languages || []) as Language[]
    const danish = languages.value.find(l => l.id === 'DA')
    if (danish) language.value = danish.name
  }
  catch
  {
    languages.value = [{ id: 'DA', name: 'danish' }, { id: 'EN', name: 'english' }]
  }
})


function onFileChange(event: Event)
{
  const input = event.target as HTMLInputElement
  selectedFile.value = input.files?.[0] ?? null
  pastedFile.value = null
}


function onPaste(event: ClipboardEvent)
{
  const items = event.clipboardData?.items ?? []
  for (const item of items)
  {
    if (item.kind === 'file' && item.type.startsWith('image/'))
    {
      event.preventDefault()
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
  error.value = null
  try
  {
    const res = await scanOcr(file)
    text.value = res.data as string
  }
  catch
  {
    error.value = i18n.t('create.ocrFailed')
  }
  finally
  {
    ocrLoading.value = false
  }
}


function resetForm()
{
  title.value = ''
  text.value = ''
  url.value = ''
  selectedFile.value = null
  pastedFile.value = null
  date.value = todayIso()
  catid.value = ''
  error.value = null
  successMsg.value = false
  validationError.value = null
  if (fileInputRef.value) fileInputRef.value.value = ''
}


async function submit()
{
  validationError.value = null
  error.value = null
  successMsg.value = false
  if (!title.value.trim()) { validationError.value = i18n.t('create.titleRequired'); return }
  if (!language.value) { validationError.value = i18n.t('create.languageRequired'); return }
  const hasFile = !!(selectedFile.value || pastedFile.value)
  const hasUrl = !!url.value.trim()
  const hasText = !!text.value.trim()
  if (hasFile && hasUrl) { validationError.value = i18n.t('create.fileOrUrlNotBoth'); return }
  if (!hasFile && !hasUrl && !hasText) { validationError.value = i18n.t('create.provideAtLeastOne'); return }

  const fd = new FormData()
  fd.append('date', date.value)
  fd.append('fldid', String(catid.value || 0))
  fd.append('title', title.value)
  fd.append('language', language.value)
  if (text.value) fd.append('text', text.value)
  if (selectedFile.value) fd.append('file', selectedFile.value)
  else if (pastedFile.value) fd.append('file', pastedFile.value, 'pasted-image.png')
  if (url.value) fd.append('url', url.value)

  loading.value = true
  try
  {
    const res = await storeDocument(fd)
    const data = res.data as { success: boolean; id?: number }
    if (data.success)
    {
      savedId.value = data.id ?? null
      successMsg.value = true
      resetForm()
      emit('saved')
    }
    else
    {
      error.value = i18n.t('create.serverError')
    }
  }
  catch
  {
    error.value = i18n.t('create.saveFailed')
  }
  finally
  {
    loading.value = false
  }
}
</script>

<style scoped>
.create-view {
  display: flex;
  flex-direction: column;
  flex: 1;
  overflow: hidden;
}

.toolbar {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 0 12px;
  height: 44px;
  background: #fff;
  border-bottom: 1px solid #e2e8f0;
  flex-shrink: 0;
}
.spacer { flex: 1; }

.section-label {
  font-size: 10px;
  font-weight: 600;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: #64748b;
}

.notice {
  margin: 0 12px;
  margin-top: 8px;
  padding: 6px 10px;
  border-radius: 4px;
  font-size: 12px;
  flex-shrink: 0;
}
.notice-success { background: #f0fdf4; border-left: 3px solid #22c55e; color: #15803d; }
.notice-error   { background: #fef2f2; border-left: 3px solid #ef4444; color: #b91c1c; }
.notice-warn    { background: #fffbeb; border-left: 3px solid #f59e0b; color: #92400e; }

.panes {
  display: flex;
  flex: 1;
  overflow: hidden;
  gap: 0;
}

.pane {
  display: flex;
  flex-direction: column;
  flex: 1;
  overflow: hidden;
  border-right: 1px solid #e2e8f0;
}
.pane:last-child { border-right: none; }

.pane-header {
  display: flex;
  align-items: center;
  height: 32px;
  padding: 0 12px;
  background: #f8fafc;
  border-bottom: 1px solid #e2e8f0;
  flex-shrink: 0;
}

.pane-title {
  font-size: 10px;
  font-weight: 600;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: #64748b;
}

.pane-body {
  flex: 1;
  overflow-y: auto;
  padding: 14px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.field {
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.field-grow { flex: 1; }
.field-grow .textarea { flex: 1; resize: none; }

.field-row {
  display: flex;
  gap: 10px;
}
.field-row .field { flex: 1; }

.field-label {
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 0.04em;
  text-transform: uppercase;
  color: #475569;
}
.req { color: #ef4444; }

.input {
  height: 32px;
  padding: 0 10px;
  border: 2px solid #94a3b8;
  border-radius: 4px;
  background: #fff;
  color: #1e293b;
  font-size: 12px;
  outline: none;
  width: 100%;
  font-weight: 400;
}
.input:focus { border-color: var(--accent); }
.input::placeholder { color: #94a3b8; }
.input:disabled { background: #f8fafc; color: #94a3b8; cursor: not-allowed; border-color: #e2e8f0; }

.textarea {
  height: auto;
  padding: 8px 10px;
  resize: vertical;
  min-height: 120px;
}

.file-row {
  display: flex;
  align-items: center;
  gap: 8px;
}
.file-input {
  flex: 1;
  font-size: 12px;
  color: #475569;
}

.paste-zone {
  padding: 12px 14px;
  border: 2px dashed #94a3b8;
  border-radius: 4px;
  background: #f8fafc;
  font-size: 12px;
  color: #64748b;
  text-align: center;
  cursor: pointer;
  outline: none;
  transition: border-color 0.1s, color 0.1s;
}
.paste-zone:focus { border-color: var(--accent); color: #1e293b; }
.paste-zone-active { border-color: #22c55e; color: #15803d; background: #f0fdf4; }

.divider {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #94a3b8;
  font-size: 11px;
  letter-spacing: 0.1em;
  text-transform: uppercase;
}
.divider::before,
.divider::after {
  content: '';
  flex: 1;
  height: 1px;
  background: #e2e8f0;
}

.btn {
  display: inline-flex;
  align-items: center;
  padding: 5px 12px;
  border: none;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  white-space: nowrap;
  transition: background 0.1s;
}
.btn:disabled { opacity: 0.45; cursor: default; }
.btn-primary { background: var(--accent); color: #fff; }
.btn-primary:not(:disabled):hover { background: var(--accent-dark); }
.btn-ghost { background: transparent; color: #475569; border: 1px solid #cbd5e1; }
.btn-ghost:hover { background: #f1f5f9; }
.btn-accent { background: #10b981; color: #fff; }
.btn-accent:not(:disabled):hover { background: #059669; }
</style>
