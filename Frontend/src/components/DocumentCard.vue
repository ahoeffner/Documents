<template>
  <div class="doc-row">
    <span class="doc-date">{{ formatDate(doc.date) }}</span>
    <span class="doc-title">{{ doc.title || 'Untitled' }}</span>
    <div class="doc-actions">
      <button v-if="doc.description" type="button" class="doc-btn" @click="showDesc = true">
        Description
      </button>
      <a :href="`/api/content/${doc.id}/text`" target="_blank" class="doc-btn">Content</a>
      <a v-if="doc.hasFile" :href="`/api/content/${doc.id}/file`" target="_blank" class="doc-btn">
        {{ doc.filename || 'File' }}
      </a>
    </div>

    <Teleport to="body">
      <div v-if="showDesc" class="desc-backdrop" @click.self="showDesc = false">
        <div class="desc-popup">
          <div class="desc-header">
            <span class="desc-title">{{ doc.title }}</span>
            <button type="button" class="desc-close" @click="showDesc = false">✕</button>
          </div>
          <div class="desc-body">{{ doc.description }}</div>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import type { DocumentResult } from '../types'

defineProps<{ doc: DocumentResult }>()
const showDesc = ref(false)

function formatDate(d: string): string {
  if (!d) return ''
  try { return new Date(d).toLocaleDateString('en-CA') } catch { return d }
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
}
.doc-row:hover { background: var(--bg-subtle); }

.doc-date {
  font-size: 11px;
  color: var(--text-faint);
  font-family: 'SFMono-Regular', Consolas, monospace;
  width: 88px;
  flex-shrink: 0;
}

.doc-title {
  flex: 1;
  min-width: 0;
  color: var(--text);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.doc-actions {
  display: flex;
  align-items: center;
  gap: 5px;
  flex-shrink: 0;
}

.doc-btn {
  display: inline-flex;
  align-items: center;
  padding: 2px 8px;
  height: 22px;
  border: 1px solid var(--accent);
  border-radius: 3px;
  background: transparent;
  color: var(--accent);
  font-size: 11px;
  font-weight: 500;
  cursor: pointer;
  white-space: nowrap;
  text-decoration: none;
  transition: background 0.1s, color 0.1s;
}
.doc-btn:hover {
  background: var(--accent);
  color: #fff;
}

/* ── Description popup ── */
.desc-backdrop {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.35);
  z-index: 9999;
  display: flex;
  align-items: center;
  justify-content: center;
}

.desc-popup {
  background: var(--bg);
  border: 1px solid var(--border);
  border-radius: 8px;
  width: min(560px, 90vw);
  max-height: 70vh;
  display: flex;
  flex-direction: column;
  box-shadow: 0 12px 40px rgba(0, 0, 0, 0.18);
}

.desc-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  border-bottom: 1px solid var(--border);
  gap: 10px;
}

.desc-title {
  font-size: 13px;
  font-weight: 600;
  color: var(--text);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.desc-close {
  width: 26px;
  height: 26px;
  border: none;
  border-radius: 4px;
  background: transparent;
  color: var(--text-muted);
  font-size: 14px;
  cursor: pointer;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
}
.desc-close:hover { background: var(--bg-muted); color: var(--text); }

.desc-body {
  padding: 14px 16px;
  font-size: 13px;
  color: var(--text);
  line-height: 1.6;
  overflow-y: auto;
  white-space: pre-wrap;
  word-break: break-word;
}
</style>
