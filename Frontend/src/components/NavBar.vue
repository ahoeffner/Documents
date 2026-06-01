<template>
  <header class="navbar">
    <div class="navbar-brand">Documents</div>
    <nav class="navbar-links">
      <RouterLink to="/" :class="{ active: route.path === '/' }">Search</RouterLink>
      <RouterLink to="/create" :class="{ active: route.path === '/create' }">Create</RouterLink>
      <RouterLink to="/chat" :class="{ active: route.path === '/chat' }">AI Chat</RouterLink>
    </nav>
    <div class="navbar-spacer"></div>
    <button class="navbar-help" @click="showHelp = true">Help</button>
  </header>

  <Teleport to="body">
    <div v-if="showHelp" class="modal-backdrop" @click.self="showHelp = false">
      <div class="modal-popup">
        <div class="modal-header">
          <span class="modal-header-title">Help</span>
          <button type="button" class="modal-close" @click="showHelp = false">✕</button>
        </div>
        <div class="modal-body help-body">

          <h4>Documents</h4>
          <p>Organise your documents into folders. Use <strong>right-click</strong> to manage everything — there are no toolbar buttons.</p>
          <ul>
            <li>Right-click a <strong>folder</strong> in the sidebar → New Subfolder, Rename, Delete</li>
            <li>Right-click <strong>empty sidebar space</strong> → New Root Folder</li>
            <li>Right-click <strong>content area</strong> → New Document, New Folder</li>
          </ul>

          <h4>Search</h4>
          <p>Uses <strong>BM25 full-text matching</strong> — a word-based ranking algorithm that finds documents containing the exact words you type.</p>
          <ul>
            <li>No semantic expansion — <em>car</em> will not match <em>vehicle</em>.</li>
            <li>No fuzzy matching — spelling must be correct.</li>
            <li>Use the most distinctive words from what you are looking for.</li>
          </ul>
          <p class="help-tip-accent">💡 For meaning-based queries, use the <strong>AI Chat</strong> instead.</p>

          <h4>AI Chat</h4>
          <p>Uses <strong>RAG (Retrieval-Augmented Generation)</strong> — the AI answers exclusively from documents you have uploaded. It does not search the internet.</p>
          <ul>
            <li>Combines vector similarity search with keyword matching to find relevant passages.</li>
            <li>Sources below each answer show which documents were used.</li>
            <li>Use <strong>Folder</strong> to narrow the search to a specific category.</li>
            <li><strong>Precision</strong> (under Advanced) controls how strictly documents must match. Lower = broader, higher = stricter.</li>
            <li>Use the history button to recall and reuse previous questions.</li>
          </ul>

        </div>
      </div>
    </div>
  </Teleport>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { useRoute } from 'vue-router'


const route = useRoute()
const showHelp = ref(false)


function onKeydown(e: KeyboardEvent) { if (e.key === 'Escape') showHelp.value = false }
onMounted(() => window.addEventListener('keydown', onKeydown))
onUnmounted(() => window.removeEventListener('keydown', onKeydown))
</script>

<style scoped>
.navbar {
  display: flex;
  align-items: center;
  gap: 24px;
  height: 36px;
  padding: 0 16px;
  background: #1e293b;
  border-bottom: 1px solid #0f172a;
  flex-shrink: 0;
}

.navbar-brand {
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 0.1em;
  text-transform: uppercase;
  color: #94a3b8;
  user-select: none;
}

.navbar-links {
  display: flex;
  align-items: center;
  gap: 2px;
  height: 100%;
}

.navbar-links a {
  display: flex;
  align-items: center;
  height: 100%;
  padding: 0 12px;
  font-size: 11px;
  font-weight: 500;
  letter-spacing: 0.05em;
  color: #64748b;
  border-bottom: 2px solid transparent;
  transition: color 0.1s, border-color 0.1s;
}

.navbar-links a:hover { color: #cbd5e1; }

.navbar-links a.active {
  color: #f8fafc;
  border-bottom-color: var(--accent);
}

.navbar-spacer { flex: 1; }

.navbar-help {
  height: 22px;
  padding: 0 10px;
  font-size: 11px;
  font-weight: 500;
  letter-spacing: 0.03em;
  border-radius: 4px;
  border: 1px solid #334155;
  background: transparent;
  color: #64748b;
  cursor: pointer;
  font-family: inherit;
  transition: background 0.1s, color 0.1s;
}
.navbar-help:hover { background: #334155; color: #cbd5e1; }
</style>
