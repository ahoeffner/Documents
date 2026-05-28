<template>
  <div class="doc-row">
    <span class="doc-date">{{ formatDate(doc.date) }}</span>
    <div class="doc-main">
      <span class="doc-title">{{ doc.title || 'Untitled' }}</span><span v-if="doc.description" class="doc-desc"> — {{ doc.description }}</span>
    </div>
    <div class="doc-actions">
      <button v-if="doc.description" type="button" class="doc-btn" @click="showText = true">Text</button>
      <span v-else class="doc-btn doc-btn-off">Text</span>
      <a v-if="doc.hasFile" :href="`/api/content/${doc.id}/file`" target="_blank" class="doc-btn">File</a>
      <span v-else class="doc-btn doc-btn-off">File</span>
      <button v-if="canEdit" type="button" class="doc-btn" @click="$emit('edit', doc.id)">Edit</button>
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
import { ref, onMounted, onUnmounted } from 'vue'
import type { DocumentResult } from '../types'

defineProps<{ doc: DocumentResult; canEdit?: boolean }>()
defineEmits<{ edit: [id: number] }>()

const showText = ref(false)

function onKeydown(e: KeyboardEvent) {
  if (e.key === 'Escape' && showText.value) showText.value = false
}
onMounted(() => window.addEventListener('keydown', onKeydown))
onUnmounted(() => window.removeEventListener('keydown', onKeydown))

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
  color: var(--text-muted);
  width: 88px;
  flex-shrink: 0;
}
.doc-main {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.doc-title { color: var(--text); }
.doc-desc  { color: var(--text-muted); }
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
  border: 1px solid var(--tab-border);
  border-radius: 3px;
  background: var(--tab-bg);
  color: var(--text);
  font-size: 11px;
  font-weight: 500;
  cursor: pointer;
  text-decoration: none;
  transition: background 0.1s;
}
.doc-btn:hover { background: var(--tab-hover-bg); }
.doc-btn-off {
  background: var(--bg-muted);
  border-color: var(--border);
  color: var(--text-faint);
  cursor: default;
  pointer-events: none;
}
</style>
