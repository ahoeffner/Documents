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

      <span v-if="voiceStatus" class="status-text" :class="{ listening: isListening }">{{ voiceStatus }}</span>

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
      <span class="adv-hint">Controls how closely documents must match before being used as sources.</span>
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
            <div class="sources-label">Sources</div>
            <div v-for="d in msg.documents" :key="d.id" class="source-row">
              <span class="source-date">{{ d.date }}</span>
              <span class="source-title">{{ d.title }}</span>
              <div class="source-btns">
                <button v-if="d.description" type="button" class="src-btn" @click="srcTextDoc = d">Text</button>
                <span v-else class="src-btn src-btn-off">Text</span>
                <a v-if="d.hasFile" :href="`/api/content/${d.id}/file`" target="_blank" class="src-btn">File</a>
                <span v-else class="src-btn src-btn-off">File</span>
              </div>
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
        <button type="button" @click="toggleMic" :disabled="loading"
          class="btn btn-ghost btn-mic" :class="{ listening: isListening }">
          <svg xmlns="http://www.w3.org/2000/svg" width="15" height="15" viewBox="0 0 20 20" fill="currentColor">
            <path fill-rule="evenodd" d="M7 4a3 3 0 016 0v4a3 3 0 11-6 0V4zm-2 4a5 5 0 1010 0v-1h-1.5v1a3.5 3.5 0 01-7 0v-1H4v1zm5 6.93V18h-2v-3.07A7.003 7.003 0 013 8H5a5 5 0 0010 0h2a7.003 7.003 0 01-6 6.93z" clip-rule="evenodd"/>
          </svg>
        </button>
        <button type="button" @click="sendMessage" :disabled="!query.trim() || loading"
          class="btn btn-primary btn-send">Send</button>
      </div>
    </div>

  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, nextTick } from 'vue'
import { useChatStore } from '../stores/chat'
import { useCategoriesStore } from '../stores/categories'
import { chat } from '../api/chat'
import type { AxiosError } from 'axios'
import type { DocumentResult } from '../types'

const srcTextDoc = ref<DocumentResult | null>(null)

const chatStore = useChatStore()
const categoriesStore = useCategoriesStore()

const query = ref('')
const selectedCategory = ref(0)
const matchValue = ref(50)
const showAdvanced = ref(false)
const loading = ref(false)
const voiceStatus = ref('')
const isListening = ref(false)
const messagesEl = ref<HTMLElement | null>(null)
const textareaEl = ref<HTMLTextAreaElement | null>(null)

onMounted(() => categoriesStore.load())

defineExpose({ focus: () => textareaEl.value?.focus() })

function scrollBottom() {
  nextTick(() => { if (messagesEl.value) messagesEl.value.scrollTop = messagesEl.value.scrollHeight })
}

async function sendMessage() {
  const q = query.value.trim()
  if (!q || loading.value) return
  chatStore.addMessage('user', q)
  query.value = ''
  loading.value = true
  scrollBottom()
  try {
    const res = await chat({ id: chatStore.sessionId, query: q, folder: selectedCategory.value, match: matchValue.value })
    const data = res.data as { success: boolean; response?: string; documents?: DocumentResult[]; refresh?: boolean }
    if (data.success) {
      chatStore.addMessage('ai', data.response ?? '', data.documents ?? null)
      if (data.refresh) await categoriesStore.load()
    } else {
      chatStore.addMessage('ai', data.response || 'Request failed.')
    }
  } catch (err) {
    const e = err as AxiosError<string>
    const msg = e.response?.data
    chatStore.addMessage('ai', `Error: ${typeof msg === 'string' ? msg : (e.message || 'Connection error')}`)
  } finally {
    loading.value = false
    scrollBottom()
  }
}

// Voice
interface Recognition {
  lang: string; interimResults: boolean; continuous: boolean
  start(): void; stop(): void
  onstart: (() => void) | null; onend: (() => void) | null
  onresult: ((e: { results: Array<{ isFinal: boolean; 0: { transcript: string } }> }) => void) | null
  onerror: ((e: { error: string }) => void) | null
}
type RecognitionCtor = new () => Recognition
const RecognitionClass: RecognitionCtor | undefined =
  (window as unknown as { SpeechRecognition?: RecognitionCtor }).SpeechRecognition ||
  (window as unknown as { webkitSpeechRecognition?: RecognitionCtor }).webkitSpeechRecognition
let recognition: Recognition | null = null

if (RecognitionClass) {
  recognition = new RecognitionClass()
  recognition.lang = 'da-DK'; recognition.interimResults = true; recognition.continuous = false
  recognition.onstart = () => { isListening.value = true; voiceStatus.value = 'Listening…' }
  recognition.onend = () => { isListening.value = false; voiceStatus.value = ''; if (query.value.trim()) sendMessage() }
  recognition.onresult = (e) => { query.value = e.results[0][0].transcript; voiceStatus.value = e.results[0].isFinal ? 'Submitting…' : `"${query.value}"` }
  recognition.onerror = (e) => { isListening.value = false; voiceStatus.value = `Mic error: ${e.error}` }
}

function toggleMic() {
  if (!recognition || loading.value) return
  if (isListening.value) { recognition.stop() }
  else { query.value = ''; try { recognition.start() } catch { voiceStatus.value = 'Could not start microphone.' } }
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
  background: var(--bg);
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
  background: var(--bg-subtle);
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

/* ── Status ── */
.status-text {
  display: flex;
  align-items: center;
  gap: 5px;
  font-size: 12px;
  color: var(--text-faint);
}
.status-text.listening { color: var(--danger); }

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
  padding: 3px 0;
  border-bottom: 1px solid var(--bg-muted);
  font-size: 11px;
}
.source-row:last-child { border-bottom: none; }
.source-date {
  font-family: 'SFMono-Regular', Consolas, monospace;
  font-size: 10px;
  color: var(--text-faint);
  width: 80px;
  flex-shrink: 0;
}
.source-title { flex: 1; min-width: 0; color: var(--text-muted); overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.source-btns { display: flex; gap: 4px; flex-shrink: 0; }

.src-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 18px;
  border: 1px solid var(--tab-border);
  border-radius: 3px;
  background: var(--tab-bg);
  color: var(--text);
  font-size: 10px;
  font-weight: 500;
  cursor: pointer;
  text-decoration: none;
  transition: background 0.1s;
}
.src-btn:hover { background: var(--tab-hover-bg); }
.src-btn-off {
  background: var(--bg-muted);
  border-color: var(--border);
  color: var(--text-faint);
  cursor: default;
  pointer-events: none;
}

/* ── Input row ── */
.input-row {
  display: flex;
  align-items: flex-end;
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

.btn-send { height: 38px; padding: 0 18px; }
.btn-mic  { width: 42px; height: 38px; padding: 0; }
.btn-mic.listening { background: var(--danger-bg); color: var(--danger); border-color: var(--danger); }

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
