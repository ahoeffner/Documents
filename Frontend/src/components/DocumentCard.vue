<template>
  <div class="doc-row">
    <span class="doc-date">{{ formatDate(doc.date) }}</span>
    <span class="doc-title">{{ doc.title || 'Untitled' }}</span>
    <div class="doc-actions">
      <button v-if="doc.description" type="button" class="doc-btn" @click="showText = true">Text</button>
      <span v-else class="doc-btn doc-btn-off">Text</span>
      <a v-if="doc.hasFile" :href="`/api/content/${doc.id}/file`" target="_blank" class="doc-btn">File</a>
      <span v-else class="doc-btn doc-btn-off">File</span>
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
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import type { DocumentResult } from '../types'

defineProps<{ doc: DocumentResult }>()
const showText = ref(false)

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
  justify-content: center;
  width: 44px;
  height: 22px;
  border: 1px solid var(--text-muted);
  border-radius: 3px;
  background: transparent;
  color: var(--text-muted);
  font-size: 11px;
  font-weight: 500;
  cursor: pointer;
  text-decoration: none;
  transition: background 0.1s, color 0.1s, border-color 0.1s;
}
.doc-btn:hover { background: var(--bg-muted); color: var(--text); border-color: var(--text); }
.doc-btn-off {
  border-color: var(--border);
  color: var(--text-faint);
  cursor: default;
  pointer-events: none;
}
</style>
