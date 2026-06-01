<template>
  <div class="search-view">

    <div class="toolbar">
      <span class="section-label">Folder</span>
      <select v-model="selectedCategory" class="select folder-select">
        <option value="0">All</option>
        <option v-for="cat in categoriesStore.categories" :key="cat.id" :value="cat.id">{{ cat.name }}</option>
      </select>

      <form @submit.prevent="doSearch" class="search-form">
        <input
          ref="searchInputEl"
          v-model="query"
          type="text"
          placeholder="Search documents…"
          class="search-input"
        />
        <button type="submit" class="btn btn-primary btn-sm">
          {{ loading ? 'Searching…' : 'Search' }}
        </button>
      </form>

      <span v-if="searched && !loading" class="result-count">
        {{ documents.length }} result{{ documents.length !== 1 ? 's' : '' }}
      </span>

      <button type="button" @click="showHelp = true" class="btn btn-primary btn-sm">Help</button>
    </div>

    <Teleport to="body">
      <div v-if="showHelp" class="modal-backdrop" @click.self="showHelp = false">
        <div class="modal-popup">
          <div class="modal-header">
            <span class="modal-header-title">About search</span>
            <button type="button" class="modal-close" @click="showHelp = false">✕</button>
          </div>
          <div class="modal-body help-body">
            <p>Search uses <strong>BM25 full-text matching</strong> — a word-based ranking algorithm. It finds documents that contain the exact words you type.</p>
            <h4>How it works</h4>
            <ul>
              <li>Each word in your query is looked up directly in the document index.</li>
              <li>Results are ranked by how often and how prominently the words appear.</li>
              <li>No semantic expansion — <em>car</em> will not match <em>vehicle</em> or <em>automobile</em>.</li>
              <li>No fuzzy matching — spelling must be correct.</li>
            </ul>
            <h4>Tips</h4>
            <ul>
              <li>Use the most distinctive words from what you are looking for.</li>
              <li>Shorter, specific queries often give better results than long sentences.</li>
              <li>Use <strong>Folder</strong> to narrow results to a specific category.</li>
            </ul>
            <p class="help-tip-accent">💡 For meaning-based queries, use the <strong>AI Chat</strong> instead.</p>
          </div>
        </div>
      </div>
    </Teleport>

    <div class="results">
      <div v-if="error" class="notice notice-error" style="margin:12px">{{ error }}</div>

      <div v-else-if="loading" class="empty-state">
        <span class="spinner spinner-lg"></span>
      </div>

      <div v-else-if="!searched" class="empty-state">Enter a query to search documents</div>

      <div v-else-if="documents.length === 0" class="empty-state">
        No documents matched <em>"{{ lastQuery }}"</em>
      </div>

      <template v-else>
        <DocumentCard v-for="doc in documents" :key="doc.id" :doc="doc" />
      </template>
    </div>

  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import type { DocumentResult } from '../types'
import { search } from '../api/documents'
import { useCategoriesStore } from '../stores/categories'
import DocumentCard from '../components/DocumentCard.vue'


const categoriesStore = useCategoriesStore()
const searchInputEl = ref<HTMLInputElement | null>(null)
const query = ref('')
const lastQuery = ref('')
const selectedCategory = ref(0)
const documents = ref<DocumentResult[]>([])
const loading = ref(false)
const error = ref<string | null>(null)
const searched = ref(false)
const showHelp = ref(false)


function onKeydown(e: KeyboardEvent) { if (e.key === 'Escape') showHelp.value = false }
onMounted(() => { categoriesStore.load(); window.addEventListener('keydown', onKeydown) })
onUnmounted(() => window.removeEventListener('keydown', onKeydown))

defineExpose({ focus: () => searchInputEl.value?.focus() })


async function doSearch()
{
  if (!query.value.trim()) return
  loading.value = true
  error.value = null
  lastQuery.value = query.value
  try
  {
    const res = await search({ query: query.value, folder: selectedCategory.value })
    documents.value = (res.data.documents || []) as DocumentResult[]
    searched.value = true
  }
  catch
  {
    error.value = 'Search failed — could not reach the server.'
  }
  finally
  {
    loading.value = false
  }
}
</script>

<style scoped>
.search-view {
  display: flex;
  flex-direction: column;
  flex: 1;
  overflow: hidden;
}

.toolbar {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 0 14px;
  height: 54px;
  background: var(--bg-muted);
  border-bottom: 1px solid var(--border);
  flex-shrink: 0;
}

.folder-select { width: auto; height: 38px; }

.search-form {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 8px;
}

/* Larger / more prominent search input */
.search-input {
  flex: 1;
  height: 42px;
  padding: 0 14px;
  border: 1.5px solid var(--border-input);
  border-radius: 6px;
  background: var(--bg);
  color: var(--text);
  font-size: 15px;
  font-family: inherit;
  outline: none;
  transition: border-color 0.15s, box-shadow 0.15s;
}
.search-input:focus {
  border-color: var(--accent);
  box-shadow: 0 0 0 3px var(--accent-ring);
}
.search-input::placeholder { color: var(--text-faint); }

.result-count {
  font-size: 12px;
  color: var(--text-muted);
  white-space: nowrap;
  font-weight: 500;
}

.results { flex: 1; overflow-y: auto; }
</style>
