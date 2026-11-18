<template>
  <div class="home-view">

    <!-- Main toolbar -->
    <div class="toolbar">
      <div class="spacer"></div>
      <button type="button" @click="togglePanel" class="btn btn-primary btn-sm" :class="{ 'panel-on': showPanel }">{{ i18n.t('home.panel') }}</button>
      <button type="button" @click="homeStore.clear()" class="btn btn-primary btn-sm">{{ i18n.t('chat.clear') }}</button>
    </div>

    <!-- Discussion with the control panel in its upper right corner -->
    <div class="discussion">
    <div class="messages" ref="messagesEl">
      <div class="msg msg-ai">
        <span class="msg-sender">{{ i18n.t('chat.assistant') }}</span>
        <span class="msg-text">{{ i18n.t('home.welcome') }}</span>
      </div>

      <template v-for="msg in homeStore.messages" :key="msg.id">
        <div class="msg" :class="msg.role === 'user' ? 'msg-user' : 'msg-ai'">
          <span class="msg-sender">{{ msg.role === 'user' ? i18n.t('chat.you') : i18n.t('chat.assistant') }}</span>
          <template v-for="(seg, i) in parseSegments(msg.text)" :key="i">
            <table v-if="seg.kind === 'table'" class="msg-table">
              <thead>
                <tr><th v-for="(h, c) in seg.head" :key="c">{{ h }}</th></tr>
              </thead>
              <tbody>
                <tr v-for="(row, r) in seg.rows" :key="r"><td v-for="(cell, c) in row" :key="c">{{ cell }}</td></tr>
              </tbody>
            </table>
            <span v-else class="msg-text">{{ seg.text }}</span>
          </template>
        </div>
      </template>

      <!-- Thinking indicator -->
      <div v-if="loading" class="msg msg-ai msg-thinking">
        <span class="msg-sender">{{ i18n.t('chat.assistant') }}</span>
        <span class="thinking-dots">
          <span class="td"></span><span class="td"></span><span class="td"></span>
          <span class="thinking-label">{{ i18n.t('chat.thinking') }}</span>
        </span>
      </div>
    </div>


    <aside v-if="showPanel" class="panel-aside">
      <HomePanel ref="panelRef" />
    </aside>
    </div>

    <Teleport to="body">
      <div v-if="showHistory" class="history-backdrop" @click="showHistory = false"></div>
    </Teleport>

    <!-- Wait prompt -->
    <Teleport to="body">
      <div v-if="showWaitPrompt" class="modal-backdrop">
        <div class="modal-popup modal-popup-sm">
          <div class="modal-header">
            <span class="modal-header-title">{{ i18n.t('chat.stillThinking') }}</span>
          </div>
          <div class="modal-body" style="font-size:13px;color:var(--text-muted)">
            {{ i18n.t('chat.stillThinkingBody') }}
          </div>
          <div class="modal-actions">
            <button class="btn btn-ghost btn-sm" @click="cancelRequest">{{ i18n.t('common.cancel') }}</button>
            <button class="btn btn-primary btn-sm" @click="keepWaiting">{{ i18n.t('chat.waitMore') }}</button>
          </div>
        </div>
      </div>
    </Teleport>

    <!-- Input row -->
    <div class="input-row">
      <div class="input-wrap">
        <textarea
          ref="textareaEl"
          v-model="query"
          :disabled="loading"
          :placeholder="i18n.t('home.placeholder')"
          class="chat-input"
          rows="4"
          @keydown.enter.exact.prevent="sendMessage()"
        />
        <button v-if="query" type="button" class="input-clear" :title="i18n.t('common.clearInput')" @click="query = ''; nextTick(() => textareaEl?.focus())">✕</button>
      </div>
      <div class="input-buttons">
        <button type="button" class="btn btn-icon mic-btn" :class="{ 'mic-recording': recorder.recording.value }" :title="i18n.t('chat.micTitle')" :disabled="loading || transcribing" @click="toggleMic">
          <span v-if="transcribing" class="spinner spinner-sm"></span>
          <svg v-else width="14" height="14" viewBox="0 0 20 20" fill="currentColor">
            <path d="M10 12a3 3 0 003-3V5a3 3 0 10-6 0v4a3 3 0 003 3zM5 9a1 1 0 10-2 0 7 7 0 006 6.93V18H7a1 1 0 100 2h6a1 1 0 100-2h-2v-2.07A7 7 0 0017 9a1 1 0 10-2 0 5 5 0 01-10 0z"/>
          </svg>
        </button>
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
        <button type="button" @click="sendMessage()"
          class="btn btn-primary btn-sm btn-send">{{ i18n.t('chat.send') }}</button>
      </div>
    </div>

  </div>
</template>

<script setup lang="ts">
import { home } from '../api/home'
import { useHomeStore } from '../stores/home'
import { useI18nStore } from '../stores/i18n'
import axios, { type AxiosError } from 'axios'
import { transcribeAudio } from '../api/transcribe'
import HomePanel from '../components/HomePanel.vue'
import { ref, onMounted, onUnmounted, nextTick } from 'vue'
import { useAudioRecorder } from '../composables/useAudioRecorder'


const HISTORY_KEY = 'home-query-history'
const PANEL_KEY = 'home-panel'


const panelRef = ref<InstanceType<typeof HomePanel> | null>(null)
const showPanel = ref(readPanelSetting())


function readPanelSetting(): boolean
{
  try
  {
    return(localStorage.getItem(PANEL_KEY) !== 'off')
  }
  catch
  {
    return(true)
  }
}


function togglePanel()
{
  showPanel.value = !showPanel.value
  try
  {
    localStorage.setItem(PANEL_KEY, showPanel.value ? 'on' : 'off')
  }
  catch
  {
    /* ignore */
  }
}


interface Segment
{
  kind: 'text' | 'table'
  text: string
  head: string[]
  rows: string[][]
}


function tableCells(line: string): string[]
{
  return(line.trim().replace(/^\|/, '').replace(/\|$/, '').split('|').map(c => c.trim()))
}


function tableSegment(lines: string[]): Segment
{
  const rows = lines.map(tableCells).filter(cells => !cells.every(c => /^:?-+:?$/.test(c)))
  if (rows.length < 2) return({ kind: 'text', text: lines.join('\n'), head: [], rows: [] })
  return({ kind: 'table', text: '', head: rows[0], rows: rows.slice(1) })
}


// Splits a reply into plain text and Markdown tables (lines starting with "|")
function parseSegments(text: string): Segment[]
{
  const segments: Segment[] = []
  let plain: string[] = []
  let table: string[] = []

  const flushPlain = () =>
  {
    const t = plain.join('\n').trim()
    if (t) segments.push({ kind: 'text', text: t, head: [], rows: [] })
    plain = []
  }

  const flushTable = () =>
  {
    if (table.length) segments.push(tableSegment(table))
    table = []
  }

  for (const line of text.split('\n'))
  {
    if (line.trim().startsWith('|')) { flushPlain(); table.push(line) }
    else { flushTable(); plain.push(line) }
  }

  flushTable()
  flushPlain()
  return(segments)
}


const i18n = useI18nStore()
const homeStore = useHomeStore()


const query = ref('')
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
const recorder = useAudioRecorder()
const transcribing = ref(false)


function onKeydown(e: KeyboardEvent)
{
  if (e.key === 'Escape') showHistory.value = false
}


onMounted(() =>
{
  const stored = localStorage.getItem(HISTORY_KEY)
  if (stored) queryHistory.value = JSON.parse(stored)
  window.addEventListener('keydown', onKeydown)
})

onUnmounted(() => window.removeEventListener('keydown', onKeydown))

defineExpose({ focus: () => textareaEl.value?.focus(), toggleMic })


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
  homeStore.addMessage('user', q)
  queryHistory.value = [q, ...queryHistory.value.filter(h => h !== q)].slice(0, 20)
  localStorage.setItem(HISTORY_KEY, JSON.stringify(queryHistory.value))
  query.value = ''
  loading.value = true
  scrollBottom()
  abortController = new AbortController()
  startWaitTimer()
  try
  {
    const res = await home({ id: homeStore.sessionId, query: q }, abortController.signal)
    const data = res.data as { success: boolean; response?: string }
    if (data.success)
    {
      homeStore.addMessage('ai', data.response ?? '')
      setTimeout(() => panelRef.value?.load(), 1500)
    }
    else
    {
      homeStore.addMessage('ai', data.response || i18n.t('chat.requestFailed'))
    }
  }
  catch (err)
  {
    if (!axios.isCancel(err))
    {
      const e = err as AxiosError
      const body = e.response?.data as Record<string, unknown> | string | undefined
      const msg = typeof body === 'object' && body?.response ? String(body.response) : typeof body === 'string' ? body : (e.message || i18n.t('chat.connectionError'))
      homeStore.addMessage('ai', i18n.t('chat.error', { message: msg }))
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


async function toggleMic()
{
  if (recorder.recording.value)
  {
    recorder.stop()
    return
  }
  try
  {
    query.value = ''
    const blob = await recorder.start()
    transcribing.value = true
    const res = await transcribeAudio(blob)
    const transcript = (res.data.text || '').trim()
    // home commands are often one word ("sluk", "status", "støvsug")
    if (!transcript) return
    query.value = transcript
    nextTick(() => textareaEl.value?.focus())
    await sendMessage()
  }
  catch (err)
  {
    const e = err as AxiosError
    const body = e.response?.data as Record<string, unknown> | undefined
    const detail = body?.error ? String(body.error) : e.message
    homeStore.addMessage('ai', i18n.t('chat.error', { message: detail || i18n.t('chat.micError') }))
  }
  finally
  {
    transcribing.value = false
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
.home-view {
  display: flex;
  flex-direction: column;
  flex: 1;
  overflow: hidden;
}

/* ── Toolbar ── */
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


.panel-on { border-color: var(--accent); color: var(--accent); }

/* ── Discussion + panel ── */
.discussion {
  position: relative;
  flex: 1;
  display: flex;
  min-height: 0;
  overflow: hidden;
  background: var(--bg);
}

/* Floating popup over the upper right corner of the discussion; takes no layout space */
.panel-aside {
  position: absolute;
  top: 10px;
  right: 18px;
  z-index: 20;
}

@media (max-width: 520px) {
  .panel-aside { left: 8px; right: 8px; }
}

/* ── Messages ── */
.messages {
  flex: 1;
  overflow-y: auto;
  padding: 14px;
  display: flex;
  flex-direction: column;
  gap: 2px;
  background: var(--bg);
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

.msg-text {
  font-size: 13px;
  line-height: 1.55;
  white-space: pre-wrap;
  word-break: break-word;
}

.msg-table {
  border-collapse: collapse;
  font-size: 13px;
  margin: 2px 0;
}
.msg-table th,
.msg-table td {
  padding: 4px 12px 4px 0;
  text-align: left;
  border-bottom: 1px solid var(--border);
}
.msg-table th {
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 0.06em;
  text-transform: uppercase;
  color: var(--text-faint);
}
.msg-table tr:last-child td { border-bottom: none; }

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

.input-wrap { position: relative; flex: 1; display: flex; }

.chat-input {
  flex: 1;
  min-height: 88px;
  max-height: 220px;
  padding: 10px 28px 10px 12px;
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

.input-clear {
  position: absolute;
  top: 8px;
  right: 8px;
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

.input-buttons {
  display: flex;
  flex-direction: column;
  gap: 6px;
  flex-shrink: 0;
  padding-bottom: 2px;
}

.btn-send { width: 100%; }
.btn-history { width: 100%; }
.mic-btn { width: 100%; }
.mic-btn.mic-recording { color: var(--danger); }

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
