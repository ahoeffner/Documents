<template>
  <div class="chat-view">

    <!-- Main toolbar -->
    <div class="toolbar">
      <span class="section-label">Folder</span>
      <select v-model="selectedCategory" class="select folder-select">
        <option value="0">All</option>
        <option v-for="cat in categoriesStore.categories" :key="cat.id" :value="cat.id">{{ cat.name }}</option>
      </select>

      <div class="spacer"></div>

<button type="button" @click="showAdvanced = !showAdvanced" class="btn btn-primary btn-sm adv-btn">
        <svg width="12" height="12" viewBox="0 0 20 20" fill="currentColor">
          <path fill-rule="evenodd" d="M11.49 3.17c-.38-1.56-2.6-1.56-2.98 0a1.532 1.532 0 01-2.286.948c-1.372-.836-2.942.734-2.106 2.106.54.886.061 2.042-.947 2.287-1.561.379-1.561 2.6 0 2.978a1.532 1.532 0 01.947 2.287c-.836 1.372.734 2.942 2.106 2.106a1.532 1.532 0 012.287.947c.379 1.561 2.6 1.561 2.978 0a1.533 1.533 0 012.287-.947c1.372.836 2.942-.734 2.106-2.106a1.533 1.533 0 01.947-2.287c1.561-.379 1.561-2.6 0-2.978a1.532 1.532 0 01-.947-2.287c.836-1.372-.734-2.942-2.106-2.106a1.532 1.532 0 01-2.287-.947zM10 13a3 3 0 100-6 3 3 0 000 6z" clip-rule="evenodd"/>
        </svg>
        Advanced
        <svg width="10" height="10" viewBox="0 0 20 20" fill="currentColor"
          :style="{ transform: showAdvanced ? 'rotate(180deg)' : '' }"
          style="transition:transform 0.15s">
          <path fill-rule="evenodd" d="M5.293 7.293a1 1 0 011.414 0L10 10.586l3.293-3.293a1 1 0 111.414 1.414l-4 4a1 1 0 01-1.414 0l-4-4a1 1 0 010-1.414z" clip-rule="evenodd"/>
        </svg>
      </button>

      <button type="button" @click="chatStore.clear()" class="btn btn-primary btn-sm">Clear</button>
    </div>

    <!-- Advanced row -->
    <div v-show="showAdvanced" class="advanced-row">
      <span class="section-label">Precision</span>
      <input type="range" min="0" max="100" step="5" v-model.number="matchValue" class="slider" />
      <span class="precision-val">{{ matchValue }}%</span>
      <span class="adv-hint">Controls how closely documents must match before being used as relevant documents.</span>
    </div>

    <!-- Messages -->
    <div class="messages" ref="messagesEl">
      <div class="msg msg-ai">
        <span class="msg-sender">Assistant</span>
        <span class="msg-text">I am your personal assistant. Ask me anything about your documents.</span>
      </div>

      <template v-for="msg in chatStore.messages" :key="msg.id">
        <div class="msg" :class="msg.role === 'user' ? 'msg-user' : 'msg-ai'">
          <span class="msg-sender">{{ msg.role === 'user' ? 'You' : 'Assistant' }}</span>
          <span class="msg-text">{{ msg.text }}</span>

          <div v-if="msg.documents && msg.documents.length" class="sources">
            <div class="sources-label">Relevant Documents</div>
            <div
              v-for="d in msg.documents"
              :key="d.id"
              class="source-row"
              @dblclick="openSourceContent(d)"
              @contextmenu.prevent="showSourceCtx($event, d)"
            >
              <span class="source-date">{{ d.date }}</span>
              <span class="source-title">{{ d.title }}</span><span v-if="d.description" class="source-desc"> — {{ d.description }}</span>
            </div>
          </div>
        </div>
      </template>

      <!-- Thinking indicator -->
      <div v-if="loading" class="msg msg-ai msg-thinking">
        <span class="msg-sender">Assistant</span>
        <span class="thinking-dots">
          <span class="td"></span><span class="td"></span><span class="td"></span>
          <span class="thinking-label">Thinking…</span>
        </span>
      </div>
    </div>

    <Teleport to="body">
      <div v-if="showHistory" class="history-backdrop" @click="showHistory = false"></div>
    </Teleport>

    <!-- Wait prompt -->
    <Teleport to="body">
      <div v-if="showWaitPrompt" class="modal-backdrop">
        <div class="modal-popup modal-popup-sm">
          <div class="modal-header">
            <span class="modal-header-title">Still thinking…</span>
          </div>
          <div class="modal-body" style="font-size:13px;color:var(--text-muted)">
            The AI is taking longer than expected. Keep waiting?
          </div>
          <div class="modal-actions">
            <button class="btn btn-ghost btn-sm" @click="cancelRequest">Cancel</button>
            <button class="btn btn-primary btn-sm" @click="keepWaiting">Wait 30 more seconds</button>
          </div>
        </div>
      </div>
    </Teleport>

    <!-- Source text popup -->
    <Teleport to="body">
      <div v-if="srcTextDoc" class="modal-backdrop" @click.self="srcTextDoc = null">
        <div class="modal-popup">
          <div class="modal-header">
            <span class="modal-header-title">{{ srcTextDoc.title }}</span>
            <button type="button" class="modal-close" @click="srcTextDoc = null">✕</button>
          </div>
          <div class="modal-body">
            <p class="modal-text">{{ srcTextDoc.description }}</p>
          </div>
        </div>
      </div>
    </Teleport>

    <!-- Source row context menu -->
    <Teleport to="body">
      <div v-if="srcCtxMenu" class="src-ctx-menu" :style="{ top: srcCtxMenu.y + 'px', left: srcCtxMenu.x + 'px' }" @click.stop>
        <button v-if="srcCtxMenu.doc.description" class="src-ctx-item" @click="srcCtxText">Show Text</button>
        <a v-if="srcCtxMenu.doc.hasFile" :href="`/api/content/${srcCtxMenu.doc.id}/file`" target="_blank" class="src-ctx-item" @click="srcCtxMenu = null">Show File</a>
        <div v-if="auth.isAdmin" class="src-ctx-divider"></div>
        <button v-if="auth.isAdmin" class="src-ctx-item" @click="srcCtxEdit">Edit Document</button>
      </div>
    </Teleport>

    <!-- Input row -->
    <div class="input-row">
      <textarea
        ref="textareaEl"
        v-model="query"
        :disabled="loading"
        placeholder="Ask a question about your documents… (Enter to send, Shift+Enter for new line)"
        class="chat-input"
        rows="4"
        @keydown.enter.exact.prevent="sendMessage"
      />
      <div class="input-buttons">
        <div class="history-wrap">
          <button type="button" @click="queryHistory.length && (showHistory = !showHistory)"
            class="btn btn-primary btn-sm btn-history">
            <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 20 20" fill="currentColor">
              <path fill-rule="evenodd" d="M3 5a1 1 0 011-1h12a1 1 0 110 2H4a1 1 0 01-1-1zm0 5a1 1 0 011-1h12a1 1 0 110 2H4a1 1 0 01-1-1zm0 5a1 1 0 011-1h12a1 1 0 110 2H4a1 1 0 01-1-1z" clip-rule="evenodd"/>
            </svg>
          </button>
          <div v-if="showHistory" class="history-panel">
            <div v-for="(h, i) in queryHistory" :key="i" class="history-item" @click="selectHistory(h)">{{ h }}</div>
          </div>
        </div>
        <button type="button" @click="sendMessage"
          class="btn btn-primary btn-sm btn-send">Send</button>
      </div>
    </div>

  </div>
</template>

<script setup lang="ts">
import { ref, watch, onMounted, onUnmounted, nextTick } from 'vue'
import axios, { type AxiosError } from 'axios'
import type { DocumentResult } from '../types'
import { chat } from '../api/chat'
import { useChatStore } from '../stores/chat'
import { useCategoriesStore } from '../stores/categories'
import { useAuthStore } from '../stores/auth'
import { useEditRequestStore } from '../stores/editRequest'


const srcTextDoc = ref<DocumentResult | null>(null)
const srcCtxMenu = ref<{ x: number; y: number; doc: DocumentResult } | null>(null)
const auth = useAuthStore()
const editRequestStore = useEditRequestStore()


function openSourceContent(d: DocumentResult)
{
  if (d.hasFile) window.open(`/api/content/${d.id}/file`, '_blank')
  else if (d.description) srcTextDoc.value = d
}


function showSourceCtx(e: MouseEvent, d: DocumentResult)
{
  if (!d.description && !d.hasFile && !auth.isAdmin) return
  srcCtxMenu.value = { x: e.clientX, y: e.clientY, doc: d }
}


function srcCtxText()
{
  const doc = srcCtxMenu.value?.doc ?? null
  srcCtxMenu.value = null
  if (doc) srcTextDoc.value = doc
}


function srcCtxEdit()
{
  const id = srcCtxMenu.value?.doc.id
  srcCtxMenu.value = null
  if (id !== undefined) editRequestStore.request(Number(id))
}


watch(srcCtxMenu, val =>
{
  if (val)
  {
    const close = () => { srcCtxMenu.value = null }
    window.addEventListener('click', close, { once: true })
  }
})


const chatStore = useChatStore()
const categoriesStore = useCategoriesStore()


const query = ref('')
const selectedCategory = ref(0)
const matchValue = ref(50)
const showAdvanced = ref(false)
const showHistory = ref(false)
const showWaitPrompt = ref(false)

let waitTimer: ReturnType<typeof setTimeout> | null = null
let abortController: AbortController | null = null

function startWaitTimer()
{
  if (waitTimer) clearTimeout(waitTimer)
  waitTimer = setTimeout(() => { showWaitPrompt.value = true }, 30000)
}


function clearWaitTimer()
{
  if (waitTimer) { clearTimeout(waitTimer); waitTimer = null }
}


function keepWaiting()
{
  showWaitPrompt.value = false
  startWaitTimer()
}


function cancelRequest()
{
  showWaitPrompt.value = false
  clearWaitTimer()
  abortController?.abort()
}


const loading = ref(false)
const messagesEl = ref<HTMLElement | null>(null)
const textareaEl = ref<HTMLTextAreaElement | null>(null)
const queryHistory = ref<string[]>([])


function onKeydown(e: KeyboardEvent)
{
  if (e.key === 'Escape')
  {
    if (srcCtxMenu.value) { srcCtxMenu.value = null; return }
    showHistory.value = false
  }
}

onMounted(() =>
{
  categoriesStore.load()
  const stored = localStorage.getItem('chat-query-history')
  if (stored) queryHistory.value = JSON.parse(stored)
  window.addEventListener('keydown', onKeydown)
})

onUnmounted(() => window.removeEventListener('keydown', onKeydown))

defineExpose({ focus: () => textareaEl.value?.focus() })


function scrollBottom()
{
  nextTick(() =>
  {
    if (messagesEl.value) messagesEl.value.scrollTop = messagesEl.value.scrollHeight
  })
}


async function sendMessage()
{
  const q = query.value.trim()
  if (!q || loading.value) return
  chatStore.addMessage('user', q)
  queryHistory.value = [q, ...queryHistory.value.filter(h => h !== q)].slice(0, 20)
  localStorage.setItem('chat-query-history', JSON.stringify(queryHistory.value))
  query.value = ''
  loading.value = true
  scrollBottom()
  abortController = new AbortController()
  startWaitTimer()
  try
  {
    const res = await chat({ id: chatStore.sessionId, query: q, folder: selectedCategory.value, match: matchValue.value }, abortController.signal)
    const data = res.data as { success: boolean; response?: string; documents?: DocumentResult[]; refresh?: boolean }
    if (data.success)
    {
      chatStore.addMessage('ai', data.response ?? '', data.documents ?? null)
      if (data.refresh) await categoriesStore.load()
    }
    else
    {
      chatStore.addMessage('ai', data.response || 'Request failed.')
    }
  }
  catch (err)
  {
    if (!axios.isCancel(err))
    {
      const e = err as AxiosError<string>
      const msg = e.response?.data
      chatStore.addMessage('ai', `Error: ${typeof msg === 'string' ? msg : (e.message || 'Connection error')}`)
    }
  }
  finally
  {
    clearWaitTimer()
    showWaitPrompt.value = false
    abortController = null
    loading.value = false
    scrollBottom()
    nextTick(() => textareaEl.value?.focus())
  }
}


function selectHistory(h: string)
{
  query.value = h
  showHistory.value = false
  nextTick(() => textareaEl.value?.focus())
}
</script>

<style scoped>
.chat-view {
  display: flex;
  flex-direction: column;
  flex: 1;
  overflow: hidden;
}

/* ── Toolbars ── */
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

.advanced-row {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 0 14px;
  height: 38px;
  background: var(--tab-bar-bg);
  border-bottom: 1px solid var(--border);
  flex-shrink: 0;
}

.slider {
  width: 110px;
  height: 4px;
  accent-color: var(--accent);
  cursor: pointer;
}

.precision-val {
  font-size: 13px;
  font-weight: 700;
  color: var(--accent);
  min-width: 36px;
}

.adv-hint { font-size: 11px; color: var(--text-faint); font-style: italic; }


/* ── Messages ── */
.messages {
  flex: 1;
  overflow-y: auto;
  padding: 14px;
  display: flex;
  flex-direction: column;
  gap: 2px;
  background: var(--bg-subtle);
}

.msg {
  display: flex;
  flex-direction: column;
  gap: 4px;
  max-width: 74%;
  padding: 9px 12px;
  border-radius: 6px;
}
.msg-ai  { align-self: flex-start; background: var(--bg); border: 1px solid var(--border); }
.msg-user { align-self: flex-end; background: var(--tab-bg); border: 1px solid var(--tab-border); color: var(--text); }

.msg-sender {
  font-size: 9px;
  font-weight: 700;
  letter-spacing: 0.1em;
  text-transform: uppercase;
  color: var(--text-faint);
}
.msg-user .msg-sender { color: var(--text-faint); }

.msg-text {
  font-size: 13px;
  line-height: 1.55;
  white-space: pre-wrap;
  word-break: break-word;
}

.sources { margin-top: 7px; padding-top: 7px; border-top: 1px solid var(--border); }
.sources-label {
  font-size: 9px; font-weight: 700; letter-spacing: 0.1em;
  text-transform: uppercase; color: var(--text-faint); margin-bottom: 4px;
}
.source-row {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 3px 4px;
  border-bottom: 1px solid var(--bg-muted);
  border-radius: 3px;
  font-size: 11px;
  cursor: pointer;
  user-select: none;
}
.source-row:last-child { border-bottom: none; }
.source-row:hover { background: var(--bg-muted); }
.source-date {
  font-family: 'SFMono-Regular', Consolas, monospace;
  font-size: 10px;
  color: var(--text);
  width: 80px;
  flex-shrink: 0;
}
.source-title { color: var(--text); }
.source-desc  { color: var(--text); overflow: hidden; text-overflow: ellipsis; white-space: nowrap; flex: 1; min-width: 0; }

.src-ctx-menu {
  position: fixed;
  z-index: 9999;
  background: var(--bg);
  border: 1px solid var(--border);
  border-radius: 6px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.14);
  padding: 4px 0;
  min-width: 130px;
}

.src-ctx-item {
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
.src-ctx-item:hover { background: var(--bg-subtle); }
.src-ctx-divider { height: 1px; background: var(--border); margin: 3px 0; }

/* ── Input row ── */
.input-row {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 14px;
  background: var(--bg);
  border-top: 1px solid var(--border);
  flex-shrink: 0;
}

.chat-input {
  flex: 1;
  min-height: 88px;
  max-height: 220px;
  padding: 10px 12px;
  border: 1.5px solid var(--border-input);
  border-radius: 6px;
  background: var(--bg);
  color: var(--text);
  font-size: 14px;
  font-family: inherit;
  outline: none;
  resize: none;
  overflow-y: auto;
  line-height: 1.55;
  transition: border-color 0.15s;
}
.chat-input:focus { border-color: var(--accent); }
.chat-input::placeholder { color: var(--text-faint); font-size: 13px; }
.chat-input:disabled { background: var(--bg-muted); color: var(--text-faint); }

.input-buttons {
  display: flex;
  flex-direction: column;
  gap: 6px;
  flex-shrink: 0;
  padding-bottom: 2px;
}

.btn-send { width: 100%; }
.btn-history { width: 100%; }

.history-wrap { position: relative; }

.history-panel {
  position: absolute;
  bottom: calc(100% + 6px);
  right: 0;
  width: 320px;
  max-height: 260px;
  overflow-y: auto;
  background: var(--bg);
  border: 1px solid var(--border);
  border-radius: 6px;
  box-shadow: 0 4px 16px rgba(0,0,0,0.18);
  z-index: 200;
}

.history-item {
  padding: 8px 12px;
  font-size: 13px;
  cursor: pointer;
  border-bottom: 1px solid var(--bg-muted);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  color: var(--text);
}
.history-item:last-child { border-bottom: none; }
.history-item:hover { background: var(--bg-subtle); }

.history-backdrop {
  position: fixed;
  inset: 0;
  z-index: 199;
}

.adv-btn { gap: 4px; }

/* ── Thinking bubble ── */
.msg-thinking {
  border-color: var(--border-input);
  border-left: 3px solid var(--border-input);
  background: var(--bg-muted);
}

.thinking-dots {
  display: flex;
  gap: 6px;
  align-items: center;
  padding: 6px 2px;
}

.td {
  width: 10px;
  height: 10px;
  background: var(--text-muted);
  border-radius: 50%;
  animation: td-bounce 1.2s ease-in-out infinite;
}
.td:nth-child(2) { animation-delay: 0.2s; }
.td:nth-child(3) { animation-delay: 0.4s; }

.thinking-label {
  font-size: 13px;
  font-style: italic;
  color: var(--text-muted);
  margin-left: 4px;
}

@keyframes td-bounce {
  0%, 60%, 100% { transform: translateY(0); opacity: 0.35; }
  30% { transform: translateY(-6px); opacity: 1; }
}
</style>
